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

@Component
@ConditionalOnProperty(prefix = "app.rag.embedding", name = "provider", havingValue = "openai")
public class OpenAiEmbeddingProvider implements EmbeddingProvider {

    private final RestClient restClient;
    private final String model;

    public OpenAiEmbeddingProvider(RagProperties ragProperties) {
        RagProperties.OpenAi openAi = ragProperties.embedding().openai();
        this.model = openAi.model();
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
                .body(new EmbeddingRequest(model, texts))
                .retrieve()
                .body(EmbeddingResponse.class);

        if (response == null || response.data() == null) {
            throw new IllegalStateException("OpenAI embedding response was empty.");
        }

        List<float[]> vectors = new ArrayList<>(response.data().size());
        for (EmbeddingData item : response.data()) {
            vectors.add(toFloatArray(item.embedding()));
        }
        return vectors;
    }

    private float[] toFloatArray(List<Double> values) {
        float[] vector = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            vector[i] = values.get(i).floatValue();
        }
        return vector;
    }

    private record EmbeddingRequest(String model, List<String> input) {
    }

    private record EmbeddingResponse(List<EmbeddingData> data) {
    }

    private record EmbeddingData(@JsonProperty("embedding") List<Double> embedding) {
    }
}
