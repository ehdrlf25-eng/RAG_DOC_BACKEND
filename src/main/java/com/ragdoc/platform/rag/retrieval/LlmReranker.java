package com.ragdoc.platform.rag.retrieval;

import com.ragdoc.platform.rag.provider.LlmProvider;
import com.ragdoc.platform.rag.vector.ChunkSearchResult;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * LLM 기반 리랭커.
 * <p>
 * RRF 융합 후보에 대해 LLM이 쿼리-패시지 관련도(0~10)를 채점하고, 점수순으로 재정렬한다.
 * LLM 호출 실패 시 RRF 순서를 그대로 유지한다.
 */
@Component
@ConditionalOnProperty(prefix = "app.rag.retrieval", name = "reranker-provider", havingValue = "llm", matchIfMissing = true)
public class LlmReranker implements Reranker {

    private static final Logger log = LoggerFactory.getLogger(LlmReranker.class);
    private static final Pattern SCORE_PATTERN = Pattern.compile("\\[\\s*(\\d+)\\s*\\]\\s*[:=]\\s*(\\d+(?:\\.\\d+)?)");

    private final LlmProvider llmProvider;

    public LlmReranker(LlmProvider llmProvider) {
        this.llmProvider = llmProvider;
    }

    /**
     * LLM에게 각 후보의 관련도를 채점받아 재정렬한다.
     * 파싱 실패한 후보는 기존 RRF 점수를 유지한다.
     */
    @Override
    public List<ChunkSearchResult> rerank(String query, List<ChunkSearchResult> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }
        if (candidates.size() == 1) {
            return candidates;
        }

        try {
            String systemPrompt = """
                    You are a relevance scoring assistant.
                    Score each passage from 0 to 10 for relevance to the query.
                    Return only lines in the format: [index] = score
                    Example:
                    [1] = 8
                    [2] = 3
                    """;
            String userPrompt = buildScoringPrompt(query, candidates);
            String response = llmProvider.complete(systemPrompt, userPrompt);
            Map<Integer, Double> scores = parseScores(response, candidates.size());

            List<ChunkSearchResult> reranked = new ArrayList<>(candidates.size());
            for (int i = 0; i < candidates.size(); i++) {
                // LLM이 특정 후보 점수를 누락하면 기존 RRF 점수 유지
                double score = scores.getOrDefault(i + 1, candidates.get(i).score());
                reranked.add(candidates.get(i).withScore(score));
            }

            reranked.sort(Comparator.comparingDouble(ChunkSearchResult::score).reversed());
            log.info(
                    "LLM rerank completed queryLength={} candidateCount={} topScore={}",
                    query.length(),
                    candidates.size(),
                    reranked.isEmpty() ? 0 : reranked.getFirst().score()
            );
            return reranked;
        } catch (Exception ex) {
            // LLM 장애 시 검색 파이프라인 전체를 중단하지 않고 RRF 순서로 폴백
            log.warn("LLM rerank failed, using RRF order reason={}", ex.getMessage());
            return candidates.stream()
                    .sorted(Comparator.comparingDouble(ChunkSearchResult::score).reversed())
                    .toList();
        }
    }

    private String buildScoringPrompt(String query, List<ChunkSearchResult> candidates) {
        StringBuilder builder = new StringBuilder();
        builder.append("Query:\n").append(query).append("\n\nPassages:\n");
        for (int i = 0; i < candidates.size(); i++) {
            ChunkSearchResult candidate = candidates.get(i);
            builder.append('[').append(i + 1).append("] ");
            if (candidate.sectionTitle() != null && !candidate.sectionTitle().isBlank()) {
                builder.append("Section: ").append(candidate.sectionTitle()).append('\n');
            }
            builder.append(truncate(candidate.childContent(), 700)).append("\n---\n");
        }
        return builder.toString();
    }

    private Map<Integer, Double> parseScores(String response, int candidateCount) {
        Map<Integer, Double> scores = new HashMap<>();
        Matcher matcher = SCORE_PATTERN.matcher(response);
        while (matcher.find()) {
            int index = Integer.parseInt(matcher.group(1));
            double score = Double.parseDouble(matcher.group(2));
            if (index >= 1 && index <= candidateCount) {
                scores.put(index, score);
            }
        }
        return scores;
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text == null ? "" : text;
        }
        return text.substring(0, maxLength) + "...";
    }
}
