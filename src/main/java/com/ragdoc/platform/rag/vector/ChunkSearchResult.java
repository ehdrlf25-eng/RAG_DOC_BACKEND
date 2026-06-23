package com.ragdoc.platform.rag.vector;

public record ChunkSearchResult(
        Long chunkId,
        Long parentSectionId,
        Long documentId,
        String documentFilename,
        int sectionIndex,
        int chunkIndex,
        String sectionTitle,
        String childContent,
        String parentContent,
        double score
) {

    /** LLM context 구성에 사용할 Parent 전체 내용. */
    public String contextContent() {
        return parentContent;
    }

    public ChunkSearchResult withScore(double newScore) {
        return new ChunkSearchResult(
                chunkId,
                parentSectionId,
                documentId,
                documentFilename,
                sectionIndex,
                chunkIndex,
                sectionTitle,
                childContent,
                parentContent,
                newScore
        );
    }
}
