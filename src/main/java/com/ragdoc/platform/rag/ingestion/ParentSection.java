package com.ragdoc.platform.rag.ingestion;

import com.ragdoc.platform.rag.parentchild.ChildChunk;
import java.util.List;

/**
 * RAG ingestion 파이프라인용 Parent(Section) 모델.
 */
public record ParentSection(
        String sectionTitle,
        int sectionIndex,
        String fullContent,
        List<ChildChunk> children,
        boolean isOrphan
) {

    public static ParentSection of(
            String sectionTitle,
            String bodyContent,
            int sectionIndex,
            List<ChildChunk> children
    ) {
        return of(sectionTitle, bodyContent, sectionIndex, children, false);
    }

    public static ParentSection of(
            String sectionTitle,
            String bodyContent,
            int sectionIndex,
            List<ChildChunk> children,
            boolean isOrphan
    ) {
        return new ParentSection(
                sectionTitle,
                sectionIndex,
                buildFullContent(sectionTitle, bodyContent),
                children,
                isOrphan
        );
    }

    private static String buildFullContent(String sectionTitle, String bodyContent) {
        if (sectionTitle == null || sectionTitle.isBlank()) {
            return bodyContent == null ? "" : bodyContent.trim();
        }
        if (bodyContent == null || bodyContent.isBlank()) {
            return sectionTitle.trim();
        }
        return sectionTitle.trim() + "\n\n" + bodyContent.trim();
    }
}
