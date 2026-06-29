package com.ragdoc.platform.document;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ragdoc.platform.rag.provider.EmbeddingProvider;
import com.ragdoc.platform.rag.vector.VectorStore;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

@ExtendWith(MockitoExtension.class)
class DocumentReembeddingServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private DocumentChunkRepository documentChunkRepository;

    @Mock
    private EmbeddingProvider embeddingProvider;

    @Mock
    private VectorStore vectorStore;

    @Mock
    private PlatformTransactionManager transactionManager;

    @InjectMocks
    private DocumentReembeddingService documentReembeddingService;

    @Test
    void reembedDocument_regeneratesEmbeddingsAndMarksReady() {
        Document document = mock(Document.class);

        DocumentChunk chunk = mock(DocumentChunk.class);
        when(chunk.getId()).thenReturn(10L);
        when(chunk.getSectionTitle()).thenReturn("Intro");
        when(chunk.getContent()).thenReturn("Hello world");

        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));
        when(documentChunkRepository.findByDocumentIdOrderByChunkIndexAsc(1L)).thenReturn(List.of(chunk));
        when(embeddingProvider.embedAll(anyList())).thenReturn(List.of(new float[1536]));

        documentReembeddingService.reembedDocument(1L);

        verify(vectorStore).saveEmbedding(eq(10L), any(float[].class));
        verify(document).setStatus(DocumentStatus.READY);
        verify(document).setChunkCount(1);
        verify(documentRepository).save(document);
    }
}
