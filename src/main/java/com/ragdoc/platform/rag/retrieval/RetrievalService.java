package com.ragdoc.platform.rag.retrieval;

import com.ragdoc.platform.config.RagProperties;
import com.ragdoc.platform.rag.provider.EmbeddingProvider;
import com.ragdoc.platform.rag.vector.ChunkSearchResult;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * RAG 검색 파이프라인의 오케스트레이터.
 * <p>
 * 쿼리 전처리 → 임베딩 → 하이브리드 검색(RRF) → 리랭킹 → Top-K 선별 → Parent 확장 순으로
 * 최종 {@link RetrievalResult} 목록을 반환한다.
 */
@Service
public class RetrievalService {

    private static final Logger log = LoggerFactory.getLogger(RetrievalService.class);

    private final QueryPreprocessor queryPreprocessor;
    private final EmbeddingProvider embeddingProvider;
    private final HybridSearchService hybridSearchService;
    private final Reranker reranker;
    private final ParentExpansionService parentExpansionService;
    private final RagProperties ragProperties;

    public RetrievalService(
            QueryPreprocessor queryPreprocessor,
            EmbeddingProvider embeddingProvider,
            HybridSearchService hybridSearchService,
            Reranker reranker,
            ParentExpansionService parentExpansionService,
            RagProperties ragProperties
    ) {
        this.queryPreprocessor = queryPreprocessor;
        this.embeddingProvider = embeddingProvider;
        this.hybridSearchService = hybridSearchService;
        this.reranker = reranker;
        this.parentExpansionService = parentExpansionService;
        this.ragProperties = ragProperties;
    }

    /**
     * 사용자 질의에 대해 전체 retrieval 파이프라인을 실행한다.
     *
     * @param userId 현재 사용자 ID (문서 소유권 필터에 사용)
     * @param query  원본 사용자 질의
     * @return 리랭킹 및 parent 확장이 적용된 검색 결과
     */
    public List<RetrievalResult> retrieve(Long userId, String query) {
        String processedQuery = queryPreprocessor.preprocess(query);
        if (processedQuery.isBlank()) {
            return List.of();
        }

        float[] queryEmbedding = embeddingProvider.embed(processedQuery);
        List<ChunkSearchResult> fusedCandidates = hybridSearchService.search(userId, processedQuery, queryEmbedding);

        // LLM 리랭킹 비용 절감: RRF 후보 중 상위 N건만 리랭커에 전달
        int rerankLimit = ragProperties.retrieval().rerankCandidateLimit();
        List<ChunkSearchResult> rerankInput = fusedCandidates.stream().limit(rerankLimit).toList();
        List<ChunkSearchResult> reranked = reranker.rerank(processedQuery, rerankInput);

        int finalTopK = ragProperties.retrieval().topK();
        List<ChunkSearchResult> topChunks = reranked.stream().limit(finalTopK).toList();

        // 동일 parent section은 제한된 횟수만 전체 본문으로 확장
        int parentExpansionLimit = ragProperties.retrieval().parentExpansionLimit();
        List<RetrievalResult> results = parentExpansionService.applyParentExpansion(topChunks, parentExpansionLimit);

        log.info(
                "Retrieval pipeline completed userId={} fusedCount={} rerankedCount={} finalCount={} parentExpandedCount={} topScores={}",
                userId,
                fusedCandidates.size(),
                reranked.size(),
                results.size(),
                results.stream().filter(RetrievalResult::parentExpanded).count(),
                results.stream()
                        .map(result -> String.format("%.4f", result.score()))
                        .collect(Collectors.joining(", ", "[", "]"))
        );
        return results;
    }
}
