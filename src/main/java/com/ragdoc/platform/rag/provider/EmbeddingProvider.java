package com.ragdoc.platform.rag.provider;

import java.util.List;

/**
 * 텍스트 임베딩 Provider 인터페이스.
 * <p>
 * 문서 ingestion 시 child chunk 임베딩 생성 및 retrieval 시 쿼리 임베딩 생성에 사용된다.
 */
public interface EmbeddingProvider {

    float[] embed(String text);

    List<float[]> embedAll(List<String> texts);
}
