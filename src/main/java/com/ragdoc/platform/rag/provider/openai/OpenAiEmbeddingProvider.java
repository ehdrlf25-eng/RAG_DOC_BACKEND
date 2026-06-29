package com.ragdoc.platform.rag.provider.openai;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ragdoc.platform.config.RagProperties;
import com.ragdoc.platform.rag.provider.EmbeddingProvider;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * OpenAI API 기반 임베딩 Provider.
 * <p>
 * {@code app.rag.embedding.provider=openai} 설정 시 활성화된다.
 * {@code text-embedding-3-small} 등은 {@code dimensions} 파라미터로 DB vector 차원과 맞출 수 있다.
 */
@Component
@ConditionalOnProperty(prefix = "app.rag.embedding", name = "provider", havingValue = "openai")
public class OpenAiEmbeddingProvider implements EmbeddingProvider {

    private final RestClient restClient;
    private final String model;
    private final int expectedDimensions;

    public OpenAiEmbeddingProvider(RagProperties ragProperties) {
        RagProperties.OpenAi openAi = ragProperties.embedding().openai();
        requireApiKey(openAi.apiKey());
        this.model = openAi.model();
        this.expectedDimensions = ragProperties.embedding().dimensions();
        this.restClient = RestClient.builder()
                .baseUrl(openAi.baseUrl())
                .defaultHeader("Authorization", "Bearer " + openAi.apiKey())
                .build();
    }

    @Override
    public float[] embed(String text) {
        return embedAll(List.of(text)).getFirst();
    }

    @Override
    public List<float[]> embedAll(List<String> texts) {
        EmbeddingResponse response = restClient.post()
                .uri("/embeddings")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new EmbeddingRequest(model, texts, expectedDimensions))
                .retrieve()
                .body(EmbeddingResponse.class);

        if (response == null || response.data() == null) {
            throw new IllegalStateException("OpenAI embedding response was empty.");
        }

        List<float[]> vectors = new ArrayList<>(response.data().size());
        for (EmbeddingData item : response.data()) {
            float[] vector = toFloatArray(item.embedding());
            validateDimensions(vector);
            vectors.add(vector);
        }
        return vectors;
    }

    private void validateDimensions(float[] vector) {
        if (vector.length != expectedDimensions) {
            throw new IllegalStateException(
                    "OpenAI embedding dimension mismatch. expected="
                            + expectedDimensions
                            + ", actual="
                            + vector.length
                            + ". Update app.rag.embedding.dimensions or OPENAI_EMBEDDING_MODEL."
            );
        }
    }

    private float[] toFloatArray(List<Double> values) {
        float[] vector = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            vector[i] = values.get(i).floatValue();
        }
        return vector;
    }

    private static void requireApiKey(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "OPENAI_API_KEY is required when app.rag.embedding.provider=openai"
            );
        }
    }

    private record EmbeddingRequest(String model, List<String> input, int dimensions) {
    }

    private record EmbeddingResponse(List<EmbeddingData> data) {
    }

    private record EmbeddingData(@JsonProperty("embedding") List<Double> embedding) {
    }
}
