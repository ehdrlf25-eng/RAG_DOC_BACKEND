package com.ragdoc.platform.kafka.event;

import java.time.Instant;
import java.util.UUID;

/** PDF 업로드 접수 후 비동기 ingest를 트리거하는 이벤트. */
public record DocumentUploadedEvent(
        UUID eventId,
        Long documentId,
        Long userId,
        String storagePath,
        String originalFilename,
        Instant occurredAt
) {
}
