package com.ragdoc.platform.conversation.dto;

/** RAG 답변의 출처 청크 정보(parent/child 내용 및 유사도 점수). */
public record SourceChunkResponse(
        Long documentId,
        String documentFilename,
        int sectionIndex,
        int chunkIndex,
        String sectionTitle,
        String childContent,
        String parentContent,
        double score
) {
}
