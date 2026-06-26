package com.ragdoc.platform.rag.vector;

/**
 * 벡터·키워드 검색 공통 결과 DTO.
 * <p>
 * Child chunk 내용과 Parent section 메타데이터를 함께 담아 하이브리드 검색·리랭킹에 전달한다.
 */
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
