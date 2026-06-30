package com.ragdoc.platform.kafka.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ragdoc.platform.config.KafkaProperties;
import com.ragdoc.platform.kafka.event.DocumentProcessedEvent;
import com.ragdoc.platform.kafka.event.DocumentProcessingFailedEvent;
import com.ragdoc.platform.kafka.event.DocumentUploadedEvent;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** PENDING Outbox 이벤트를 Kafka로 발행한다. */
@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaProperties kafkaProperties;
    private final ObjectMapper objectMapper;

    public OutboxPublisher(
            OutboxEventRepository outboxEventRepository,
            KafkaTemplate<String, Object> kafkaTemplate,
            KafkaProperties kafkaProperties,
            ObjectMapper objectMapper
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaProperties = kafkaProperties;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${app.kafka.outbox.publish-interval-ms:1000}")
    @Transactional
    public void publishPendingEvents() {
        for (OutboxEvent outboxEvent : outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc(OutboxEventStatus.PENDING)) {
            try {
                publish(outboxEvent);
                outboxEvent.setStatus(OutboxEventStatus.PUBLISHED);
                outboxEvent.setPublishedAt(Instant.now());
            } catch (Exception ex) {
                outboxEvent.setRetryCount(outboxEvent.getRetryCount() + 1);
                if (outboxEvent.getRetryCount() >= kafkaProperties.outbox().maxPublishRetries()) {
                    outboxEvent.setStatus(OutboxEventStatus.FAILED);
                    log.error(
                            "Outbox publish permanently failed eventId={} eventType={} aggregateId={} reason={}",
                            outboxEvent.getEventId(),
                            outboxEvent.getEventType(),
                            outboxEvent.getAggregateId(),
                            ex.getMessage(),
                            ex
                    );
                } else {
                    log.warn(
                            "Outbox publish retry eventId={} eventType={} retryCount={} reason={}",
                            outboxEvent.getEventId(),
                            outboxEvent.getEventType(),
                            outboxEvent.getRetryCount(),
                            ex.getMessage()
                    );
                }
            }
        }
    }

    private void publish(OutboxEvent outboxEvent) throws Exception {
        String topic = resolveTopic(outboxEvent.getEventType());
        Object payload = readPayload(outboxEvent);
        String key = String.valueOf(outboxEvent.getAggregateId());
        kafkaTemplate.send(topic, key, payload).get();
        log.info(
                "Outbox event published eventId={} eventType={} topic={} aggregateId={}",
                outboxEvent.getEventId(),
                outboxEvent.getEventType(),
                topic,
                outboxEvent.getAggregateId()
        );
    }

    private String resolveTopic(OutboxEventType eventType) {
        return switch (eventType) {
            case DOCUMENT_UPLOADED -> kafkaProperties.topics().documentUploaded();
            case DOCUMENT_PROCESSED -> kafkaProperties.topics().documentProcessed();
            case DOCUMENT_PROCESSING_FAILED -> kafkaProperties.topics().documentProcessingFailed();
        };
    }

    private Object readPayload(OutboxEvent outboxEvent) throws Exception {
        return switch (outboxEvent.getEventType()) {
            case DOCUMENT_UPLOADED -> objectMapper.readValue(outboxEvent.getPayload(), DocumentUploadedEvent.class);
            case DOCUMENT_PROCESSED -> objectMapper.readValue(outboxEvent.getPayload(), DocumentProcessedEvent.class);
            case DOCUMENT_PROCESSING_FAILED -> objectMapper.readValue(outboxEvent.getPayload(), DocumentProcessingFailedEvent.class);
        };
    }
}
