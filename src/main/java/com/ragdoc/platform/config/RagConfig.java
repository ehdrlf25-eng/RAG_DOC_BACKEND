package com.ragdoc.platform.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** {@link RagProperties}를 Spring 빈으로 등록한다. */
/**
 * RAG 관련 Spring 설정.
 * <p>
 * {@link RagProperties}를 활성화하여 chunking, retrieval, embedding, LLM 설정을 주입한다.
 */
@Configuration
@EnableConfigurationProperties(RagProperties.class)
public class RagConfig {
}
