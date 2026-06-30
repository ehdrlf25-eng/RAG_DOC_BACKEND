package com.ragdoc.platform.kafka.event;

import java.time.Instant;
import java.util.UUID;

/** 문서 ingest 실패 이벤트. */
public record DocumentProcessingFailedEvent(
        UUID eventId,
        Long documentId,
        Long userId,
        String reason,
        Instant occurredAt
) {
}
