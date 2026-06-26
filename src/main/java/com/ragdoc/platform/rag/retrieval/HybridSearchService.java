package com.ragdoc.platform.rag.retrieval;

import com.ragdoc.platform.config.RagProperties;
import com.ragdoc.platform.rag.vector.ChunkSearchResult;
import com.ragdoc.platform.rag.vector.VectorStore;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 밀집(dense) 벡터 검색과 키워드(희소) 검색을 결합하는 하이브리드 검색 서비스.
 * <p>
 * 두 검색 결과를 {@link ReciprocalRankFusion}으로 융합하여 단일 순위 목록을 생성한다.
 */
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

    /**
     * 벡터 유사도 검색과 키워드 FTS 검색을 병렬 수행한 뒤 RRF로 융합한다.
     *
     * @param userId          문서 소유권 필터
     * @param query           전처리된 텍스트 쿼리 (키워드 검색용)
     * @param queryEmbedding  쿼리 임베딩 벡터 (밀집 검색용)
     */
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
