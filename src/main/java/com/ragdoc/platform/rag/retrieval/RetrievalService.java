package com.ragdoc.platform.rag.retrieval;

import com.ragdoc.platform.config.RagProperties;
import com.ragdoc.platform.rag.provider.EmbeddingProvider;
import com.ragdoc.platform.rag.vector.ChunkSearchResult;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

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
     * Query preprocessing → hybrid search → RRF → rerank → top-K → parent expansion.
     */
    public List<RetrievalResult> retrieve(Long userId, String query) {
        String processedQuery = queryPreprocessor.preprocess(query);
        if (processedQuery.isBlank()) {
            return List.of();
        }

        float[] queryEmbedding = embeddingProvider.embed(processedQuery);
        List<ChunkSearchResult> fusedCandidates = hybridSearchService.search(userId, processedQuery, queryEmbedding);

        int rerankLimit = ragProperties.retrieval().rerankCandidateLimit();
        List<ChunkSearchResult> rerankInput = fusedCandidates.stream().limit(rerankLimit).toList();
        List<ChunkSearchResult> reranked = reranker.rerank(processedQuery, rerankInput);

        int finalTopK = ragProperties.retrieval().topK();
        List<ChunkSearchResult> topChunks = reranked.stream().limit(finalTopK).toList();

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
