package com.ragdoc.platform.rag.vector;

import java.util.List;

/**
 * 벡터 임베딩 저장 및 유사도 검색 인터페이스.
 * <p>
 * Child chunk 임베딩을 저장하고, 쿼리 임베딩과의 코사인 유사도로 밀집 검색을 수행한다.
 */
public interface VectorStore {

    void saveEmbedding(Long chunkId, float[] embedding);

    List<ChunkSearchResult> searchSimilar(
            Long userId,
            float[] queryEmbedding,
            int topK,
            double minSimilarity
    );
}
