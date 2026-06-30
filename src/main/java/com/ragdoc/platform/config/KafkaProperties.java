package com.ragdoc.platform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Kafka 토픽·Consumer retry·Outbox 발행 설정 ({@code app.kafka.*}). */
@ConfigurationProperties(prefix = "app.kafka")
public record KafkaProperties(
        Topics topics,
        Consumer consumer,
        Outbox outbox
) {

    public record Topics(
            String documentUploaded,
            String documentProcessed,
            String documentProcessingFailed,
            String documentUploadedDlq
    ) {
    }

    public record Consumer(long retryIntervalMs, int maxAttempts) {
    }

    public record Outbox(long publishIntervalMs, int maxPublishRetries) {
    }
}
