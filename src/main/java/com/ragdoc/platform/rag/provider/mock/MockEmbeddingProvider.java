package com.ragdoc.platform.rag.provider.mock;

import com.ragdoc.platform.config.RagProperties;
import com.ragdoc.platform.rag.provider.EmbeddingProvider;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.rag.embedding", name = "provider", havingValue = "mock", matchIfMissing = true)
public class MockEmbeddingProvider implements EmbeddingProvider {

    private final int dimensions;

    public MockEmbeddingProvider(RagProperties ragProperties) {
        this.dimensions = ragProperties.embedding().dimensions();
    }

    @Override
    public float[] embed(String text) {
        return vectorize(text);
    }

    @Override
    public List<float[]> embedAll(List<String> texts) {
        List<float[]> vectors = new ArrayList<>(texts.size());
        for (String text : texts) {
            vectors.add(vectorize(text));
        }
        return vectors;
    }

    private float[] vectorize(String text) {
        float[] vector = new float[dimensions];
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        for (int i = 0; i < dimensions; i++) {
            vector[i] = bytes.length == 0 ? 0f : (bytes[i % bytes.length] & 0xff) / 255f;
        }
        return normalize(vector);
    }

    private float[] normalize(float[] vector) {
        double sum = 0;
        for (float value : vector) {
            sum += value * value;
        }
        if (sum == 0) {
            return vector;
        }
        double norm = Math.sqrt(sum);
        float[] normalized = new float[vector.length];
        for (int i = 0; i < vector.length; i++) {
            normalized[i] = (float) (vector[i] / norm);
        }
        return normalized;
    }
}
