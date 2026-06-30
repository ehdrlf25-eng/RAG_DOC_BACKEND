package com.ragdoc.platform.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ragdoc.platform.kafka.outbox.OutboxService;
import com.ragdoc.platform.rag.PdfTextExtractor;
import com.ragdoc.platform.rag.ingestion.ParentSection;
import com.ragdoc.platform.rag.parentchild.ChildChunk;
import com.ragdoc.platform.rag.pdf.PdfLine;
import com.ragdoc.platform.rag.provider.EmbeddingProvider;
import com.ragdoc.platform.rag.section.SectionChunkingService;
import com.ragdoc.platform.rag.vector.VectorStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DocumentIngestionServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private DocumentChunkRepository documentChunkRepository;

    @Mock
    private ParentSectionRepository parentSectionRepository;

    @Mock
    private PdfTextExtractor pdfTextExtractor;

    @Mock
    private SectionChunkingService sectionChunkingService;

    @Mock
    private EmbeddingProvider embeddingProvider;

    @Mock
    private VectorStore vectorStore;

    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private DocumentIngestionService documentIngestionService;

    @TempDir
    Path tempDir;

    @Test
    void ingestUploadedDocument_marksFailedWhenPdfMissing() {
        Document document = processingDocument(tempDir.resolve("missing.pdf").toString());
        ReflectionTestUtils.setField(document, "id", 10L);
        when(documentRepository.findById(10L)).thenReturn(Optional.of(document));
        when(documentChunkRepository.findByDocumentIdOrderByChunkIndexAsc(10L)).thenReturn(List.of());

        documentIngestionService.ingestUploadedDocument(10L);

        assertThat(document.getStatus()).isEqualTo(DocumentStatus.FAILED);
        verify(outboxService).enqueueDocumentProcessingFailed(document, "Stored PDF file not found: " + document.getStoragePath());
        verify(outboxService, never()).enqueueDocumentProcessed(any());
    }

    @Test
    void ingestUploadedDocument_skipsWhenStatusIsNotProcessing() {
        Document document = processingDocument(tempDir.resolve("sample.pdf").toString());
        document.setStatus(DocumentStatus.READY);
        when(documentRepository.findById(11L)).thenReturn(Optional.of(document));

        documentIngestionService.ingestUploadedDocument(11L);

        verify(documentChunkRepository, never()).findByDocumentIdOrderByChunkIndexAsc(11L);
        verify(outboxService, never()).enqueueDocumentProcessed(any());
    }

    @Test
    void ingestUploadedDocument_completesWhenPipelineSucceeds() throws Exception {
        Path pdf = tempDir.resolve("sample.pdf");
        Files.writeString(pdf, "pdf");
        Document document = processingDocument(pdf.toString());
        ReflectionTestUtils.setField(document, "id", 12L);
        when(documentRepository.findById(12L)).thenReturn(Optional.of(document));
        when(documentChunkRepository.findByDocumentIdOrderByChunkIndexAsc(12L)).thenReturn(List.of());
        when(pdfTextExtractor.extract(any())).thenReturn(
                new PdfTextExtractor.PdfExtractionResult(List.of(PdfLine.of("line", 12f, false)), 1)
        );
        when(sectionChunkingService.buildParentSections(anyList())).thenReturn(List.of(
                new ParentSection("Section", 0, "full", List.of(ChildChunk.of("chunk", 0)), false)
        ));
        when(parentSectionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(documentChunkRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(embeddingProvider.embedAll(anyList())).thenReturn(List.of(new float[] {0.1f, 0.2f}));

        documentIngestionService.ingestUploadedDocument(12L);

        assertThat(document.getStatus()).isEqualTo(DocumentStatus.READY);
        assertThat(document.getChunkCount()).isEqualTo(1);
        verify(outboxService).enqueueDocumentProcessed(document);
    }

    private Document processingDocument(String storagePath) {
        Document document = new Document();
        document.setUserId(1L);
        document.setOriginalFilename("sample.pdf");
        document.setStoragePath(storagePath);
        document.setStatus(DocumentStatus.PROCESSING);
        document.setChunkCount(0);
        return document;
    }
}
