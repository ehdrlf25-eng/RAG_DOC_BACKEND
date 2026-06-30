package com.ragdoc.platform.kafka.consumer;

import com.ragdoc.platform.config.KafkaProperties;
import com.ragdoc.platform.document.DocumentIngestionService;
import com.ragdoc.platform.kafka.event.DocumentUploadedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** document.uploaded 이벤트를 수신해 비동기 ingest를 실행한다. */
@Component
public class DocumentUploadedConsumer {

    private static final Logger log = LoggerFactory.getLogger(DocumentUploadedConsumer.class);

    private final DocumentIngestionService documentIngestionService;

    public DocumentUploadedConsumer(DocumentIngestionService documentIngestionService) {
        this.documentIngestionService = documentIngestionService;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.document-uploaded}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "documentUploadedListenerContainerFactory"
    )
    public void onDocumentUploaded(DocumentUploadedEvent event) {
        log.info(
                "DocumentUploaded event received eventId={} documentId={} userId={}",
                event.eventId(),
                event.documentId(),
                event.userId()
        );
        documentIngestionService.ingestUploadedDocument(event.documentId());
    }
}
