package com.ragdoc.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * RAG Doc Platform Spring Boot 진입점.
 */
@SpringBootApplication
@EnableScheduling
public class RagDocPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(RagDocPlatformApplication.class, args);
    }
}
