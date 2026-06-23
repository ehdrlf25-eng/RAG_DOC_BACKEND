package com.ragdoc.platform.rag.vector;

import java.util.List;

public interface VectorStore {

    void saveEmbedding(Long chunkId, float[] embedding);

    List<ChunkSearchResult> searchSimilar(
            Long userId,
            float[] queryEmbedding,
            int topK,
            double minSimilarity
    );
}
