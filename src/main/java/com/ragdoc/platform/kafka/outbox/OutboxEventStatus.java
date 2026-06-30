package com.ragdoc.platform.kafka.outbox;

/** Outbox 이벤트 발행 상태. */
public enum OutboxEventStatus {
    PENDING,
    PUBLISHED,
    FAILED
}
