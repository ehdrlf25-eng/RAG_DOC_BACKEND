package com.ragdoc.platform.kafka.outbox;

/** Outbox에 저장되는 도메인 이벤트 유형. */
public enum OutboxEventType {
    DOCUMENT_UPLOADED,
    DOCUMENT_PROCESSED,
    DOCUMENT_PROCESSING_FAILED
}
