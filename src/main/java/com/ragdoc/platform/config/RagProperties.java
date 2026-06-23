package com.ragdoc.platform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rag")
public record RagProperties(
        Storage storage,
        Retrieval retrieval,
        Chunking chunking,
        Embedding embedding,
        Llm llm
) {

    public record Storage(String path) {
    }

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

    public record Chunking(int childMaxTokens) {
    }

    public record Embedding(
            String provider,
            int dimensions,
            OpenAi openai,
            Ollama ollama
    ) {
    }

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
