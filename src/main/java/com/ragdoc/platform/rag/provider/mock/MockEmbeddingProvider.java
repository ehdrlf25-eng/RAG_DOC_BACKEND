package com.ragdoc.platform.rag.provider.mock;

import com.ragdoc.platform.config.RagProperties;
import com.ragdoc.platform.rag.provider.EmbeddingProvider;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 테스트·로컬 개발용 결정론적 임베딩 Provider.
 * <p>
 * 외부 API 없이 텍스트 바이트를 기반으로 정규화된 벡터를 생성한다.
 * 기본 embedding provider({@code matchIfMissing = true})로 사용된다.
 */
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

    /**
     * 텍스트 바이트를 순환 매핑하여 결정론적 벡터를 생성하고 L2 정규화한다.
     * 동일 입력은 항상 동일 벡터를 반환하여 테스트 재현성을 보장한다.
     */
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
