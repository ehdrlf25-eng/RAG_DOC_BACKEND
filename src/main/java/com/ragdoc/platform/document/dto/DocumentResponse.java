package com.ragdoc.platform.document.dto;

import com.ragdoc.platform.document.Document;
import com.ragdoc.platform.document.DocumentStatus;
import java.time.Instant;

/** 문서 목록·상세 API 응답 DTO. */
public record DocumentResponse(
        Long id,
        String originalFilename,
        DocumentStatus status,
        Integer pageCount,
        int chunkCount,
        Instant createdAt,
        Instant updatedAt
) {

    public static DocumentResponse from(Document document) {
        return new DocumentResponse(
                document.getId(),
                document.getOriginalFilename(),
                document.getStatus(),
                document.getPageCount(),
                document.getChunkCount(),
                document.getCreatedAt(),
                document.getUpdatedAt()
        );
    }
}
