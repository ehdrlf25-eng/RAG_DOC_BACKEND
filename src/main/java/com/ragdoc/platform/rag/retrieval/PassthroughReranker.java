package com.ragdoc.platform.rag.retrieval;

import com.ragdoc.platform.rag.vector.ChunkSearchResult;
import java.util.Comparator;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.rag.retrieval", name = "reranker-provider", havingValue = "passthrough")
public class PassthroughReranker implements Reranker {

    @Override
    public List<ChunkSearchResult> rerank(String query, List<ChunkSearchResult> candidates) {
        return candidates.stream()
                .sorted(Comparator.comparingDouble(ChunkSearchResult::score).reversed())
                .toList();
    }
}
