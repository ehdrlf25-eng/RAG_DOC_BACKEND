package com.ragdoc.platform.document;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** 부모 섹션 {@link ParentSection} JPA 저장소. */
public interface ParentSectionRepository extends JpaRepository<ParentSection, Long> {

    List<ParentSection> findByDocumentIdOrderBySectionIndexAsc(Long documentId);

    void deleteByDocumentId(Long documentId);
}
