package com.ragdoc.platform.document;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** 업로드 문서 {@link Document} JPA 저장소. */
public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findByUserIdOrderByCreatedAtDesc(Long userId);
}
