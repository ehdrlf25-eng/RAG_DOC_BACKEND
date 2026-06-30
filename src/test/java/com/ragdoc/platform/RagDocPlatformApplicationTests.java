package com.ragdoc.platform;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(
        partitions = 1,
        topics = {
                "document.uploaded",
                "document.processed",
                "document.processing-failed",
                "document.uploaded.dlq"
        }
)
class RagDocPlatformApplicationTests {

    @Test
    void contextLoads() {
    }
}
