package com.ragdoc.platform.document;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 앱 기동 시 {@link DocumentReembeddingService}를 실행한다.
 * <p>
 * V7 마이그레이션 직후 1회성 재임베딩용. {@code app.rag.reembed-on-startup=true} 일 때만 활성화된다.
 */
@Component
@ConditionalOnProperty(prefix = "app.rag", name = "reembed-on-startup", havingValue = "true")
public class DocumentReembeddingRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DocumentReembeddingRunner.class);

    private final DocumentReembeddingService documentReembeddingService;

    public DocumentReembeddingRunner(DocumentReembeddingService documentReembeddingService) {
        this.documentReembeddingService = documentReembeddingService;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Re-embedding on startup is enabled (app.rag.reembed-on-startup=true)");
        documentReembeddingService.reembedAll();
    }
}
