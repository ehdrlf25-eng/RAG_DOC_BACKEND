package com.ragdoc.platform.document;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParentSectionRepository extends JpaRepository<ParentSection, Long> {

    List<ParentSection> findByDocumentIdOrderBySectionIndexAsc(Long documentId);

    void deleteByDocumentId(Long documentId);
}
