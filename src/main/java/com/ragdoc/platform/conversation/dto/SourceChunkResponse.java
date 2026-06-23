package com.ragdoc.platform.conversation.dto;

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
