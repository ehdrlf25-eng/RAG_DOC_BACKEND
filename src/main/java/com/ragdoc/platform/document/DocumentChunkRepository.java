package com.ragdoc.platform.document;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** 문서 청크 {@link DocumentChunk} JPA 저장소. */
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {

    List<DocumentChunk> findByDocumentIdOrderByChunkIndexAsc(Long documentId);

    void deleteByDocumentId(Long documentId);
}
