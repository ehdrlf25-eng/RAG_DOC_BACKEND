package com.ragdoc.platform.rag.ingestion;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 문장·문단 분리 유틸리티.
 * <p>
 * {@link SemanticChunkingService}에서 섹션 본문을 문장 단위로 분해할 때 사용한다.
 */
final class SentenceSplitter {

    private static final Pattern SENTENCE_BOUNDARY = Pattern.compile("(?<=[.!?。！？])\\s+");

    private SentenceSplitter() {
    }

    static List<String> splitIntoParagraphs(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        String[] parts = text.trim().split("\\n\\n+");
        List<String> paragraphs = new ArrayList<>(parts.length);
        for (String part : parts) {
            String normalized = part.replace('\n', ' ').replaceAll("\\s+", " ").trim();
            if (!normalized.isEmpty()) {
                paragraphs.add(normalized);
            }
        }
        return paragraphs.isEmpty() ? List.of(text.trim()) : paragraphs;
    }

    static List<String> splitIntoSentences(String paragraph) {
        if (paragraph == null || paragraph.isBlank()) {
            return List.of();
        }

        String trimmed = paragraph.trim();
        String[] parts = SENTENCE_BOUNDARY.split(trimmed);
        List<String> sentences = new ArrayList<>(parts.length);
        for (String part : parts) {
            String sentence = part.trim();
            if (!sentence.isEmpty()) {
                sentences.add(sentence);
            }
        }
        return sentences.isEmpty() ? List.of(trimmed) : sentences;
    }
}
