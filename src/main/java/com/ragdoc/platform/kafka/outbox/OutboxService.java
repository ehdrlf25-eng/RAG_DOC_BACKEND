package com.ragdoc.platform.kafka.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ragdoc.platform.document.Document;
import com.ragdoc.platform.kafka.event.DocumentProcessedEvent;
import com.ragdoc.platform.kafka.event.DocumentProcessingFailedEvent;
import com.ragdoc.platform.kafka.event.DocumentUploadedEvent;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Outbox 이벤트 적재. Upload/ingest 트랜잭션과 함께 커밋된다. */
@Service
public class OutboxService {

    private static final String AGGREGATE_TYPE_DOCUMENT = "document";

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public OutboxService(OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void enqueueDocumentUploaded(Document document) {
        DocumentUploadedEvent event = new DocumentUploadedEvent(
                UUID.randomUUID(),
                document.getId(),
                document.getUserId(),
                document.getStoragePath(),
                document.getOriginalFilename(),
                Instant.now()
        );
        save(OutboxEventType.DOCUMENT_UPLOADED, document.getId(), event);
    }

    @Transactional
    public void enqueueDocumentProcessed(Document document) {
        DocumentProcessedEvent event = new DocumentProcessedEvent(
                UUID.randomUUID(),
                document.getId(),
                document.getUserId(),
                document.getChunkCount(),
                document.getPageCount(),
                Instant.now()
        );
        save(OutboxEventType.DOCUMENT_PROCESSED, document.getId(), event);
    }

    @Transactional
    public void enqueueDocumentProcessingFailed(Document document, String reason) {
        DocumentProcessingFailedEvent event = new DocumentProcessingFailedEvent(
                UUID.randomUUID(),
                document.getId(),
                document.getUserId(),
                reason,
                Instant.now()
        );
        save(OutboxEventType.DOCUMENT_PROCESSING_FAILED, document.getId(), event);
    }

    private void save(OutboxEventType eventType, Long documentId, Object payload) {
        OutboxEvent outboxEvent = new OutboxEvent();
        outboxEvent.setEventId(extractEventId(payload));
        outboxEvent.setAggregateType(AGGREGATE_TYPE_DOCUMENT);
        outboxEvent.setAggregateId(documentId);
        outboxEvent.setEventType(eventType);
        outboxEvent.setPayload(writePayload(payload));
        outboxEvent.setStatus(OutboxEventStatus.PENDING);
        outboxEvent.setRetryCount(0);
        outboxEventRepository.save(outboxEvent);
    }

    private UUID extractEventId(Object payload) {
        if (payload instanceof DocumentUploadedEvent uploaded) {
            return uploaded.eventId();
        }
        if (payload instanceof DocumentProcessedEvent processed) {
            return processed.eventId();
        }
        if (payload instanceof DocumentProcessingFailedEvent failed) {
            return failed.eventId();
        }
        throw new IllegalArgumentException("Unsupported outbox payload type: " + payload.getClass().getName());
    }

    private String writePayload(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize outbox payload", ex);
        }
    }
}
