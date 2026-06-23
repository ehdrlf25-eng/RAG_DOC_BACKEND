package com.ragdoc.platform.rag.retrieval;

import com.ragdoc.platform.rag.vector.ChunkSearchResult;

/**
 * Hybrid search → rerank → parent expansion 이후 최종 retrieval 결과.
 */
public record RetrievalResult(
        Long chunkId,
        Long parentSectionId,
        Long documentId,
        String documentFilename,
        int sectionIndex,
        int chunkIndex,
        String content,
        double score,
        String parentTitle,
        String parentContent,
        boolean parentExpanded
) {

    public static RetrievalResult from(ChunkSearchResult chunk, boolean parentExpanded) {
        return new RetrievalResult(
                chunk.chunkId(),
                chunk.parentSectionId(),
                chunk.documentId(),
                chunk.documentFilename(),
                chunk.sectionIndex(),
                chunk.chunkIndex(),
                chunk.childContent(),
                chunk.score(),
                chunk.sectionTitle(),
                chunk.parentContent(),
                parentExpanded
        );
    }

    public RetrievalResult withScore(double score) {
        return new RetrievalResult(
                chunkId,
                parentSectionId,
                documentId,
                documentFilename,
                sectionIndex,
                chunkIndex,
                content,
                score,
                parentTitle,
                parentContent,
                parentExpanded
        );
    }

    public RetrievalResult withParentExpanded(boolean expanded) {
        return new RetrievalResult(
                chunkId,
                parentSectionId,
                documentId,
                documentFilename,
                sectionIndex,
                chunkIndex,
                content,
                score,
                parentTitle,
                parentContent,
                expanded
        );
    }

    /** LLM context: parent expansion이 적용된 경우 parent 전체, 아니면 child snippet. */
    public String contextContent() {
        if (parentExpanded && parentContent != null && !parentContent.isBlank()) {
            return parentContent;
        }
        return content;
    }
}
