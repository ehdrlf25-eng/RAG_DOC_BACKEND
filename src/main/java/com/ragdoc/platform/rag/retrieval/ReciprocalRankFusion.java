package com.ragdoc.platform.rag.retrieval;

import com.ragdoc.platform.rag.vector.ChunkSearchResult;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Reciprocal Rank Fusion(RRF) 알고리즘 구현.
 * <p>
 * 서로 다른 검색 방식(벡터, 키워드)의 순위를 점수 스케일에 의존하지 않고 통합한다.
 */
@Component
public class ReciprocalRankFusion {

    /**
     * 두 검색 결과 목록을 RRF 점수로 융합한다.
     * <p>
     * 공식: score(d) = Σ 1 / (rank(d) + k)
     *
     * @param dense   밀집(벡터) 검색 결과 (순위순)
     * @param keyword 키워드 검색 결과 (순위순)
     * @param k       RRF 상수 (순위 하위 문서의 기여도 완화)
     */
    public List<ChunkSearchResult> fuse(List<ChunkSearchResult> dense, List<ChunkSearchResult> keyword, int k) {
        Map<Long, Double> fusedScores = new HashMap<>();
        Map<Long, ChunkSearchResult> resultsByChunkId = new LinkedHashMap<>();

        accumulateRankScores(dense, k, fusedScores, resultsByChunkId);
        accumulateRankScores(keyword, k, fusedScores, resultsByChunkId);

        List<Map.Entry<Long, Double>> ranked = new ArrayList<>(fusedScores.entrySet());
        ranked.sort(Map.Entry.<Long, Double>comparingByValue().reversed());

        return ranked.stream()
                .map(entry -> resultsByChunkId.get(entry.getKey()).withScore(entry.getValue()))
                .toList();
    }

    private void accumulateRankScores(
            List<ChunkSearchResult> results,
            int k,
            Map<Long, Double> fusedScores,
            Map<Long, ChunkSearchResult> resultsByChunkId
    ) {
        for (int rank = 0; rank < results.size(); rank++) {
            ChunkSearchResult result = results.get(rank);
            // rank는 0-based이므로 실제 순위는 rank + 1
            double contribution = 1.0d / (rank + 1 + k);
            fusedScores.merge(result.chunkId(), contribution, Double::sum);
            resultsByChunkId.putIfAbsent(result.chunkId(), result);
        }
    }
}
