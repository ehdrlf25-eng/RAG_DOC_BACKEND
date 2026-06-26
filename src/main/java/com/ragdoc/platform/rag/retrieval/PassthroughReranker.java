package com.ragdoc.platform.rag.retrieval;

import com.ragdoc.platform.rag.vector.ChunkSearchResult;
import java.util.Comparator;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 리랭킹을 생략하고 기존 점수(RRF) 순서를 유지하는 패스스루 리랭커.
 * <p>
 * {@code app.rag.retrieval.reranker-provider=passthrough} 설정 시 활성화된다.
 */
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
