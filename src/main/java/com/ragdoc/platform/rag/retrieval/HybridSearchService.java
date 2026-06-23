package com.ragdoc.platform.rag.retrieval;

import com.ragdoc.platform.config.RagProperties;
import com.ragdoc.platform.rag.vector.ChunkSearchResult;
import com.ragdoc.platform.rag.vector.VectorStore;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class HybridSearchService {

    private static final Logger log = LoggerFactory.getLogger(HybridSearchService.class);

    private final VectorStore vectorStore;
    private final KeywordSearchStore keywordSearchStore;
    private final ReciprocalRankFusion reciprocalRankFusion;
    private final RagProperties ragProperties;

    public HybridSearchService(
            VectorStore vectorStore,
            KeywordSearchStore keywordSearchStore,
            ReciprocalRankFusion reciprocalRankFusion,
            RagProperties ragProperties
    ) {
        this.vectorStore = vectorStore;
        this.keywordSearchStore = keywordSearchStore;
        this.reciprocalRankFusion = reciprocalRankFusion;
        this.ragProperties = ragProperties;
    }

    public List<ChunkSearchResult> search(Long userId, String query, float[] queryEmbedding) {
        int candidateLimit = ragProperties.retrieval().hybridCandidateLimit();
        double denseMinSimilarity = ragProperties.retrieval().hybridDenseMinSimilarity();
        int rrfK = ragProperties.retrieval().rrfK();

        List<ChunkSearchResult> denseResults = vectorStore.searchSimilar(
                userId,
                queryEmbedding,
                candidateLimit,
                denseMinSimilarity
        );
        List<ChunkSearchResult> keywordResults = keywordSearchStore.search(userId, query, candidateLimit);
        List<ChunkSearchResult> fusedResults = reciprocalRankFusion.fuse(denseResults, keywordResults, rrfK);

        log.info(
                "Hybrid search completed userId={} denseCount={} keywordCount={} fusedCount={} rrfK={} topFusedScores={}",
                userId,
                denseResults.size(),
                keywordResults.size(),
                fusedResults.size(),
                rrfK,
                fusedResults.stream()
                        .limit(5)
                        .map(result -> String.format("%.4f", result.score()))
                        .collect(Collectors.joining(", ", "[", "]"))
        );
        return fusedResults;
    }
}
