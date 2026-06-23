package com.ragdoc.platform.rag.retrieval;

import com.ragdoc.platform.rag.vector.ChunkSearchResult;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ReciprocalRankFusion {

    /**
     * RRF: score(d) = sum(1 / (rank + k)) across retrieval lists.
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
            double contribution = 1.0d / (rank + 1 + k);
            fusedScores.merge(result.chunkId(), contribution, Double::sum);
            resultsByChunkId.putIfAbsent(result.chunkId(), result);
        }
    }
}
