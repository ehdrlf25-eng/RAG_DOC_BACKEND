package com.ragdoc.platform.rag.retrieval;

import com.ragdoc.platform.rag.vector.ChunkSearchResult;
import java.util.List;

public interface KeywordSearchStore {

    List<ChunkSearchResult> search(Long userId, String query, int limit);
}
