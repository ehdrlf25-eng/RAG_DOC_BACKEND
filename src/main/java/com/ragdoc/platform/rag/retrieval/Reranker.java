package com.ragdoc.platform.rag.retrieval;

import com.ragdoc.platform.rag.vector.ChunkSearchResult;
import java.util.List;

public interface Reranker {

    List<ChunkSearchResult> rerank(String query, List<ChunkSearchResult> candidates);
}
