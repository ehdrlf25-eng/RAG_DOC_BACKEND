package com.ragdoc.platform.document;

import com.ragdoc.platform.common.MessageKeys;
import com.ragdoc.platform.config.RagProperties;
import com.ragdoc.platform.document.dto.DocumentResponse;
import com.ragdoc.platform.rag.PdfTextExtractor;
import com.ragdoc.platform.rag.provider.EmbeddingProvider;
import com.ragdoc.platform.rag.ingestion.ParentSection;
import com.ragdoc.platform.rag.section.SectionChunkingService;
import com.ragdoc.platform.rag.parentchild.ChildChunk;
import com.ragdoc.platform.rag.vector.VectorStore;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final PdfTextExtractor pdfTextExtractor;
    private final SectionChunkingService sectionChunkingService;
    private final ParentSectionRepository parentSectionRepository;
    private final EmbeddingProvider embeddingProvider;
    private final VectorStore vectorStore;
    private final RagProperties ragProperties;

    public DocumentService(
            DocumentRepository documentRepository,
            DocumentChunkRepository documentChunkRepository,
            PdfTextExtractor pdfTextExtractor,
            SectionChunkingService sectionChunkingService,
            ParentSectionRepository parentSectionRepository,
            EmbeddingProvider embeddingProvider,
            VectorStore vectorStore,
            RagProperties ragProperties
    ) {
        this.documentRepository = documentRepository;
        this.documentChunkRepository = documentChunkRepository;
        this.pdfTextExtractor = pdfTextExtractor;
        this.sectionChunkingService = sectionChunkingService;
        this.parentSectionRepository = parentSectionRepository;
        this.embeddingProvider = embeddingProvider;
        this.vectorStore = vectorStore;
        this.ragProperties = ragProperties;
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> listDocuments(Long userId) {
        return documentRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(DocumentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public DocumentResponse getDocument(Long userId, Long documentId) {
        return DocumentResponse.from(getOwnedDocument(userId, documentId));
    }

    @Transactional
    public DocumentResponse uploadDocument(Long userId, MultipartFile file) {
        validatePdf(file);
        String filename = sanitizeFilename(file.getOriginalFilename());

        log.info("Document upload started userId={} filename={} sizeBytes={}", userId, filename, file.getSize());

        Path storagePath = storeFile(userId, file);

        Document document = new Document();
        document.setUserId(userId);
        document.setOriginalFilename(filename);
        document.setStoragePath(storagePath.toString());
        document.setStatus(DocumentStatus.PROCESSING);
        document.setChunkCount(0);

        try {
            ingestDocument(document, file);
            document.setStatus(DocumentStatus.READY);
            log.info(
                    "Document upload completed userId={} documentId={} filename={} status={} pageCount={} chunkCount={}",
                    userId,
                    document.getId(),
                    filename,
                    document.getStatus(),
                    document.getPageCount(),
                    document.getChunkCount()
            );
        } catch (Exception ex) {
            document.setStatus(DocumentStatus.FAILED);
            log.warn(
                    "Document upload failed userId={} filename={} status={} reason={}",
                    userId,
                    filename,
                    document.getStatus(),
                    ex.getMessage(),
                    ex
            );
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    MessageKeys.DOCUMENT_PROCESSING_FAILED,
                    ex
            );
        } finally {
            documentRepository.save(document);
        }

        return DocumentResponse.from(document);
    }

    @Transactional
    public void deleteDocument(Long userId, Long documentId) {
        Document document = getOwnedDocument(userId, documentId);
        documentChunkRepository.deleteByDocumentId(documentId);
        parentSectionRepository.deleteByDocumentId(documentId);
        documentRepository.delete(document);
        deleteStoredFile(document.getStoragePath());
        log.info("Document deleted userId={} documentId={} filename={}", userId, documentId, document.getOriginalFilename());
    }

    /**
     * PDF 수집 파이프라인: 라인 추출 → 섹션 청킹 → Parent-Child 분할 → Child 임베딩 → 저장.
     */
    private void ingestDocument(Document document, MultipartFile file) throws IOException {
        PdfTextExtractor.PdfExtractionResult extraction = pdfTextExtractor.extract(file.getInputStream());
        document.setPageCount(extraction.pageCount());

        List<ParentSection> parentSections = sectionChunkingService.buildParentSections(extraction.lines());
        if (parentSections.isEmpty()) {
            throw new IllegalStateException("No text extracted from PDF.");
        }

        log.info(
                "Document ingestion prepared documentFilename={} pageCount={} lineCount={} parentCount={} childCount={} sectionTitles={}",
                document.getOriginalFilename(),
                extraction.pageCount(),
                extraction.lines().size(),
                parentSections.size(),
                parentSections.stream().mapToInt(section -> section.children().size()).sum(),
                parentSections.stream().map(ParentSection::sectionTitle).collect(Collectors.toList())
        );

        document = documentRepository.save(document);
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
        log.info(
                "Document parent-child embeddings stored documentId={} parentCount={} childCount={} embeddingDimensions={}",
                document.getId(),
                parentSections.size(),
                savedChildren.size(),
                embeddings.isEmpty() ? 0 : embeddings.getFirst().length
        );
    }

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

    private Path storeFile(Long userId, MultipartFile file) {
        try {
            Path directory = Path.of(ragProperties.storage().path(), String.valueOf(userId));
            Files.createDirectories(directory);
            Path target = directory.resolve(UUID.randomUUID() + ".pdf");
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return target;
        } catch (IOException ex) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    MessageKeys.DOCUMENT_STORAGE_FAILED,
                    ex
            );
        }
    }

    private void deleteStoredFile(String storagePath) {
        if (storagePath == null) {
            return;
        }
        try {
            Files.deleteIfExists(Path.of(storagePath));
        } catch (IOException ignored) {
            // Best effort cleanup.
        }
    }

    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "document.pdf";
        }
        return filename.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
