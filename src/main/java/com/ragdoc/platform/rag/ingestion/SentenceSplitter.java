package com.ragdoc.platform.rag.ingestion;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

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
