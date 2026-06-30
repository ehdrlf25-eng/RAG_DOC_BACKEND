package com.ragdoc.platform.document;

import com.ragdoc.platform.common.MessageKeys;
import com.ragdoc.platform.document.dto.DocumentResponse;
import com.ragdoc.platform.document.storage.DocumentStorage;
import com.ragdoc.platform.kafka.outbox.OutboxService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * PDF 문서 업로드·조회·삭제를 담당한다.
 * 업로드 시 파일 저장과 PROCESSING 등록 후 Kafka Outbox로 ingest를 비동기 위임한다.
 */
@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final ParentSectionRepository parentSectionRepository;
    private final DocumentStorage documentStorage;
    private final OutboxService outboxService;

    public DocumentService(
            DocumentRepository documentRepository,
            DocumentChunkRepository documentChunkRepository,
            ParentSectionRepository parentSectionRepository,
            DocumentStorage documentStorage,
            OutboxService outboxService
    ) {
        this.documentRepository = documentRepository;
        this.documentChunkRepository = documentChunkRepository;
        this.parentSectionRepository = parentSectionRepository;
        this.documentStorage = documentStorage;
        this.outboxService = outboxService;
    }

    /** 현재 사용자가 업로드한 문서를 최신순으로 반환한다. */
    @Transactional(readOnly = true)
    public List<DocumentResponse> listDocuments(Long userId) {
        return documentRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(DocumentResponse::from)
                .toList();
    }

    /** 문서 상세 조회. 타 사용자 문서는 403을 반환한다. */
    @Transactional(readOnly = true)
    public DocumentResponse getDocument(Long userId, Long documentId) {
        return DocumentResponse.from(getOwnedDocument(userId, documentId));
    }

    /**
     * PDF를 저장하고 PROCESSING 상태로 등록한 뒤 Outbox에 DocumentUploaded를 적재한다.
     * ingest는 Kafka Consumer가 비동기로 수행한다.
     */
    @Transactional
    public DocumentResponse uploadDocument(Long userId, MultipartFile file) {
        validatePdf(file);
        String filename = sanitizeFilename(file.getOriginalFilename());

        log.info("Document upload started userId={} filename={} sizeBytes={}", userId, filename, file.getSize());

        String storageKey = documentStorage.store(userId, file);

        Document document = new Document();
        document.setUserId(userId);
        document.setOriginalFilename(filename);
        document.setStoragePath(storageKey);
        document.setStatus(DocumentStatus.PROCESSING);
        document.setChunkCount(0);
        document = documentRepository.save(document);

        outboxService.enqueueDocumentUploaded(document);

        log.info(
                "Document upload accepted userId={} documentId={} filename={} status={} storageKey={}",
                userId,
                document.getId(),
                filename,
                document.getStatus(),
                storageKey
        );

        return DocumentResponse.from(document);
    }

    /** DB 청크·섹션·메타데이터와 저장소 파일을 함께 삭제한다. */
    @Transactional
    public void deleteDocument(Long userId, Long documentId) {
        Document document = getOwnedDocument(userId, documentId);
        documentChunkRepository.deleteByDocumentId(documentId);
        parentSectionRepository.deleteByDocumentId(documentId);
        documentRepository.delete(document);
        documentStorage.delete(document.getStoragePath());
        log.info("Document deleted userId={} documentId={} filename={}", userId, documentId, document.getOriginalFilename());
    }

    /** 존재하지 않으면 404, 소유자가 다르면 403. RAG 검색 범위 격리의 기준점. */
    private Document getOwnedDocument(Long userId, Long documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        MessageKeys.DOCUMENT_NOT_FOUND
                ));

        if (!document.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, MessageKeys.DOCUMENT_ACCESS_DENIED);
        }

        return document;
    }

    private void validatePdf(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, MessageKeys.DOCUMENT_FILE_REQUIRED);
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".pdf")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, MessageKeys.DOCUMENT_INVALID_TYPE);
        }
    }

    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "document.pdf";
        }
        return filename.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
