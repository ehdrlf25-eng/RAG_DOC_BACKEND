package com.ragdoc.platform.document;

import com.ragdoc.platform.rag.parentchild.ChildChunk;
import com.ragdoc.platform.rag.provider.EmbeddingProvider;
import com.ragdoc.platform.rag.vector.VectorStore;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 기존 청크 텍스트를 유지한 채 임베딩만 재생성한다.
 * <p>
 * V7 마이그레이션(768→1536) 이후 {@link DocumentStatus#PROCESSING} 상태 문서를 대상으로 한다.
 */
@Service
public class DocumentReembeddingService {

    private static final Logger log = LoggerFactory.getLogger(DocumentReembeddingService.class);

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final EmbeddingProvider embeddingProvider;
    private final VectorStore vectorStore;
    private final TransactionTemplate transactionTemplate;

    public DocumentReembeddingService(
            DocumentRepository documentRepository,
            DocumentChunkRepository documentChunkRepository,
            EmbeddingProvider embeddingProvider,
            VectorStore vectorStore,
            PlatformTransactionManager transactionManager
    ) {
        this.documentRepository = documentRepository;
        this.documentChunkRepository = documentChunkRepository;
        this.embeddingProvider = embeddingProvider;
        this.vectorStore = vectorStore;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /** PROCESSING 상태인 모든 문서에 대해 임베딩을 재생성한다. */
    public void reembedAll() {
        List<Document> documents = documentRepository.findByStatus(DocumentStatus.PROCESSING);
        log.info("Document re-embedding started documentCount={}", documents.size());

        int succeeded = 0;
        int failed = 0;
        for (Document document : documents) {
            try {
                transactionTemplate.executeWithoutResult(status -> reembedDocument(document.getId()));
                succeeded++;
            } catch (Exception ex) {
                failed++;
                log.warn(
                        "Document re-embedding failed documentId={} filename={} reason={}",
                        document.getId(),
                        document.getOriginalFilename(),
                        ex.getMessage(),
                        ex
                );
            }
        }

        log.info("Document re-embedding finished succeeded={} failed={}", succeeded, failed);
    }

    void reembedDocument(Long documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));

        List<DocumentChunk> chunks = documentChunkRepository.findByDocumentIdOrderByChunkIndexAsc(documentId);
        if (chunks.isEmpty()) {
            document.setStatus(DocumentStatus.FAILED);
            documentRepository.save(document);
            throw new IllegalStateException("No chunks found for documentId=" + documentId);
        }

        List<String> embeddingTexts = new ArrayList<>(chunks.size());
        for (DocumentChunk chunk : chunks) {
            embeddingTexts.add(toEmbeddingText(chunk.getSectionTitle(), chunk.getContent()));
        }

        List<float[]> embeddings = embeddingProvider.embedAll(embeddingTexts);
        if (embeddings.size() != chunks.size()) {
            throw new IllegalStateException(
                    "Embedding count mismatch for documentId=" + documentId
                            + " chunks=" + chunks.size()
                            + " embeddings=" + embeddings.size()
            );
        }

        for (int i = 0; i < chunks.size(); i++) {
            vectorStore.saveEmbedding(chunks.get(i).getId(), embeddings.get(i));
        }

        document.setStatus(DocumentStatus.READY);
        document.setChunkCount(chunks.size());
        documentRepository.save(document);

        log.info(
                "Document re-embedding completed documentId={} chunkCount={} embeddingDimensions={}",
                documentId,
                chunks.size(),
                embeddings.isEmpty() ? 0 : embeddings.getFirst().length
        );
    }

    private String toEmbeddingText(String sectionTitle, String content) {
        return ChildChunk.of(content, 0).toEmbeddingText(sectionTitle);
    }
}
