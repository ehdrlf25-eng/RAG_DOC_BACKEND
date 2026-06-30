package com.ragdoc.platform.document;

import com.ragdoc.platform.document.storage.DocumentStorage;
import com.ragdoc.platform.kafka.outbox.OutboxService;
import com.ragdoc.platform.rag.PdfTextExtractor;
import com.ragdoc.platform.rag.ingestion.ParentSection;
import com.ragdoc.platform.rag.parentchild.ChildChunk;
import com.ragdoc.platform.rag.provider.EmbeddingProvider;
import com.ragdoc.platform.rag.section.SectionChunkingService;
import com.ragdoc.platform.rag.vector.VectorStore;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Kafka Consumer가 호출하는 PDF ingest 파이프라인.
 * <p>
 * 디스크에 저장된 PDF를 읽어 텍스트 추출 → 청킹 → 임베딩 → DB 저장을 수행한다.
 */
@Service
public class DocumentIngestionService {

    private static final Logger log = LoggerFactory.getLogger(DocumentIngestionService.class);

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final ParentSectionRepository parentSectionRepository;
    private final PdfTextExtractor pdfTextExtractor;
    private final SectionChunkingService sectionChunkingService;
    private final EmbeddingProvider embeddingProvider;
    private final VectorStore vectorStore;
    private final OutboxService outboxService;
    private final DocumentStorage documentStorage;

    public DocumentIngestionService(
            DocumentRepository documentRepository,
            DocumentChunkRepository documentChunkRepository,
            ParentSectionRepository parentSectionRepository,
            PdfTextExtractor pdfTextExtractor,
            SectionChunkingService sectionChunkingService,
            EmbeddingProvider embeddingProvider,
            VectorStore vectorStore,
            OutboxService outboxService,
            DocumentStorage documentStorage
    ) {
        this.documentRepository = documentRepository;
        this.documentChunkRepository = documentChunkRepository;
        this.parentSectionRepository = parentSectionRepository;
        this.pdfTextExtractor = pdfTextExtractor;
        this.sectionChunkingService = sectionChunkingService;
        this.embeddingProvider = embeddingProvider;
        this.vectorStore = vectorStore;
        this.outboxService = outboxService;
        this.documentStorage = documentStorage;
    }

    /**
     * DocumentUploaded 이벤트 수신 후 ingest를 수행한다.
     * 실패해도 예외를 던지지 않고 DB 상태·Outbox로 결과를 기록한다.
     */
    @Transactional
    public void ingestUploadedDocument(Long documentId) {
        Document document = documentRepository.findById(documentId).orElse(null);
        if (document == null) {
            log.warn("Document ingest skipped because document was not found documentId={}", documentId);
            return;
        }
        if (document.getStatus() != DocumentStatus.PROCESSING) {
            log.info(
                    "Document ingest skipped because status is not PROCESSING documentId={} status={}",
                    documentId,
                    document.getStatus()
            );
            return;
        }
        if (!documentChunkRepository.findByDocumentIdOrderByChunkIndexAsc(documentId).isEmpty()) {
            log.info(
                    "Document ingest skipped because chunks already exist documentId={}",
                    documentId
            );
            return;
        }

        try {
            ingestFromStorage(document);
            document.setStatus(DocumentStatus.READY);
            documentRepository.save(document);
            outboxService.enqueueDocumentProcessed(document);
            log.info(
                    "Document ingest completed documentId={} filename={} chunkCount={} pageCount={}",
                    document.getId(),
                    document.getOriginalFilename(),
                    document.getChunkCount(),
                    document.getPageCount()
            );
        } catch (Exception ex) {
            markFailed(document, ex.getMessage(), ex);
        }
    }

    /** DLQ 수신 시 PROCESSING 상태 문서를 FAILED로 전환한다. */
    @Transactional
    public void markFailedIfProcessing(Long documentId, String reason) {
        Document document = documentRepository.findById(documentId).orElse(null);
        if (document == null || document.getStatus() != DocumentStatus.PROCESSING) {
            return;
        }
        markFailed(document, reason, null);
    }

    private void markFailed(Document document, String reason, Exception ex) {
        document.setStatus(DocumentStatus.FAILED);
        documentRepository.save(document);
        outboxService.enqueueDocumentProcessingFailed(document, reason);
        if (ex == null) {
            log.warn(
                    "Document ingest failed documentId={} filename={} reason={}",
                    document.getId(),
                    document.getOriginalFilename(),
                    reason
            );
        } else {
            log.warn(
                    "Document ingest failed documentId={} filename={} reason={}",
                    document.getId(),
                    document.getOriginalFilename(),
                    reason,
                    ex
            );
        }
    }

    private void ingestFromStorage(Document document) throws IOException {
        String storageKey = document.getStoragePath();
        if (!documentStorage.exists(storageKey)) {
            throw new IllegalStateException("Stored PDF file not found: " + storageKey);
        }

        try (InputStream inputStream = documentStorage.open(storageKey)) {
            PdfTextExtractor.PdfExtractionResult extraction = pdfTextExtractor.extract(inputStream);
            document.setPageCount(extraction.pageCount());

            List<ParentSection> parentSections = sectionChunkingService.buildParentSections(extraction.lines());
            if (parentSections.isEmpty()) {
                throw new IllegalStateException("No text extracted from PDF.");
            }

            log.info(
                    "Document ingestion prepared documentId={} documentFilename={} pageCount={} lineCount={} parentCount={} childCount={}",
                    document.getId(),
                    document.getOriginalFilename(),
                    extraction.pageCount(),
                    extraction.lines().size(),
                    parentSections.size(),
                    parentSections.stream().mapToInt(section -> section.children().size()).sum()
            );

            Long documentId = document.getId();
            List<DocumentChunk> savedChildren = new ArrayList<>();
            List<String> embeddingTexts = new ArrayList<>();

            for (ParentSection parentSection : parentSections) {
                com.ragdoc.platform.document.ParentSection parent = new com.ragdoc.platform.document.ParentSection();
                parent.setDocumentId(documentId);
                parent.setSectionIndex(parentSection.sectionIndex());
                parent.setTitle(parentSection.sectionTitle());
                parent.setFullContent(parentSection.fullContent());
                parent = parentSectionRepository.save(parent);

                for (ChildChunk childChunk : parentSection.children()) {
                    DocumentChunk child = new DocumentChunk();
                    child.setDocumentId(documentId);
                    child.setParentSectionId(parent.getId());
                    child.setChunkIndex(childChunk.chunkIndex());
                    child.setSectionTitle(parentSection.sectionTitle());
                    child.setContent(childChunk.content());
                    savedChildren.add(documentChunkRepository.save(child));
                    embeddingTexts.add(childChunk.toEmbeddingText(parentSection.sectionTitle()));
                }
            }

            List<float[]> embeddings = embeddingProvider.embedAll(embeddingTexts);
            for (int i = 0; i < savedChildren.size(); i++) {
                vectorStore.saveEmbedding(savedChildren.get(i).getId(), embeddings.get(i));
            }

            document.setChunkCount(savedChildren.size());
        }
    }
}
