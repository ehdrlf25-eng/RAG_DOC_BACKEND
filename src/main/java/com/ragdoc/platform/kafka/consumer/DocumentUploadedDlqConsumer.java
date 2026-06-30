package com.ragdoc.platform.kafka.consumer;

import com.ragdoc.platform.document.DocumentIngestionService;
import com.ragdoc.platform.kafka.event.DocumentUploadedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * document.uploaded.dlq 수신 Consumer.
 * <p>
 * 재시도 소진 후 DLQ로 이동한 메시지에 대해 문서를 FAILED로 마킹한다.
 */
@Component
public class DocumentUploadedDlqConsumer {

    private static final Logger log = LoggerFactory.getLogger(DocumentUploadedDlqConsumer.class);

    private final DocumentIngestionService documentIngestionService;

    public DocumentUploadedDlqConsumer(DocumentIngestionService documentIngestionService) {
        this.documentIngestionService = documentIngestionService;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.document-uploaded-dlq}",
            groupId = "${spring.kafka.consumer.group-id}-dlq",
            containerFactory = "documentUploadedListenerContainerFactory"
    )
    public void onDocumentUploadedDlq(DocumentUploadedEvent event) {
        log.error(
                "DocumentUploaded DLQ received eventId={} documentId={} userId={}",
                event.eventId(),
                event.documentId(),
                event.userId()
        );
        documentIngestionService.markFailedIfProcessing(
                event.documentId(),
                "Consumer retries exhausted; message moved to DLQ"
        );
    }
}
