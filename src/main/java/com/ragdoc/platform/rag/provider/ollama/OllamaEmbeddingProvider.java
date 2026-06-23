package com.ragdoc.platform.rag.provider.ollama;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ragdoc.platform.config.RagProperties;
import com.ragdoc.platform.rag.provider.EmbeddingProvider;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(prefix = "app.rag.embedding", name = "provider", havingValue = "ollama")
public class OllamaEmbeddingProvider implements EmbeddingProvider {

    private final RestClient restClient;
    private final String model;
    private final int expectedDimensions;

    public OllamaEmbeddingProvider(RagProperties ragProperties) {
        RagProperties.Ollama ollama = ragProperties.embedding().ollama();
        this.model = ollama.model();
        this.expectedDimensions = ragProperties.embedding().dimensions();
        this.restClient = RestClient.builder()
                .baseUrl(ollama.baseUrl())
                .build();
    }

    @Override
    public float[] embed(String text) {
        return embedAll(List.of(text)).getFirst();
    }

    @Override
    public List<float[]> embedAll(List<String> texts) {
        if (texts.isEmpty()) {
            return List.of();
        }

        EmbedResponse response = restClient.post()
                .uri("/api/embed")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new EmbedRequest(model, texts))
                .retrieve()
                .body(EmbedResponse.class);

        if (response == null || response.embeddings() == null || response.embeddings().isEmpty()) {
            throw new IllegalStateException("Ollama embedding response was empty.");
        }

        List<float[]> vectors = new ArrayList<>(response.embeddings().size());
        for (List<Double> embedding : response.embeddings()) {
            float[] vector = toFloatArray(embedding);
            validateDimensions(vector);
            vectors.add(vector);
        }
        return vectors;
    }

    private void validateDimensions(float[] vector) {
        if (vector.length != expectedDimensions) {
            throw new IllegalStateException(
                    "Ollama embedding dimension mismatch. expected="
                            + expectedDimensions
                            + ", actual="
                            + vector.length
                            + ". Update app.rag.embedding.dimensions or choose a matching model."
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

    private record EmbedRequest(String model, List<String> input) {
    }

    private record EmbedResponse(@JsonProperty("embeddings") List<List<Double>> embeddings) {
    }
}
