package com.ragdoc.platform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * RAG 파이프라인 설정 ({@code app.rag.*}).
 * 저장 경로, 검색 파라미터, 청킹, 임베딩·LLM 프로바이더를 바인딩한다.
 */
@ConfigurationProperties(prefix = "app.rag")
public record RagProperties(
        Storage storage,
        Retrieval retrieval,
        Chunking chunking,
        Embedding embedding,
        Llm llm
) {

    /** PDF 원본 저장 설정 (local: path, cloud: S3 bucket/region) */
    public record Storage(
            String provider,
            String path,
            String bucket,
            String region
    ) {
    }

    /** 벡터·하이브리드 검색 및 대화 이력 제한 */
    public record Retrieval(
            int topK,
            int maxHistoryMessages,
            double minSimilarity,
            int hybridCandidateLimit,
            double hybridDenseMinSimilarity,
            int rrfK,
            int rerankCandidateLimit,
            int parentExpansionLimit,
            String rerankerProvider
    ) {
    }

    /** Child 청크 최대 토큰 수 */
    public record Chunking(int childMaxTokens) {
    }

    /** 임베딩 프로바이더 (ollama | openai) 및 차원 */
    public record Embedding(
            String provider,
            int dimensions,
            OpenAi openai,
            Ollama ollama
    ) {
    }

    /** 채팅 LLM 프로바이더 (ollama | openai) */
    public record Llm(
            String provider,
            OpenAi openai,
            Ollama ollama
    ) {
    }

    public record OpenAi(String apiKey, String baseUrl, String model) {
    }

    public record Ollama(String baseUrl, String model) {
    }
}
