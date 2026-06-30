package com.ragdoc.platform.kafka.event;

import java.time.Instant;
import java.util.UUID;

/** 문서 ingest(청킹·임베딩) 성공 이벤트. */
public record DocumentProcessedEvent(
        UUID eventId,
        Long documentId,
        Long userId,
        int chunkCount,
        Integer pageCount,
        Instant occurredAt
) {
}
