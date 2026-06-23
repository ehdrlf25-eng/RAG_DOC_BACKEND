package com.ragdoc.platform.rag.ingestion;

import com.ragdoc.platform.config.RagProperties;
import com.ragdoc.platform.rag.parentchild.ChildChunk;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 문장 인식 기반 semantic child chunking.
 * embedding/LLM/vector DB 없이 ingestion 단계에서만 동작한다.
 */
@Service
public class SemanticChunkingService {

    private static final double PARAGRAPH_BREAK_THRESHOLD_RATIO = 0.75;

    private static final Logger log = LoggerFactory.getLogger(SemanticChunkingService.class);

    private final RagProperties ragProperties;

    public SemanticChunkingService(RagProperties ragProperties) {
        this.ragProperties = ragProperties;
    }

    /**
     * 섹션 본문을 semantic child chunk로 분할한다.
     */
    public List<ChildChunk> chunk(String sectionContent) {
        if (sectionContent == null || sectionContent.isBlank()) {
            return List.of();
        }

        int maxTokens = ragProperties.chunking().childMaxTokens();
        List<String> groupedTexts = groupSentences(sectionContent.trim(), maxTokens);
        List<ChildChunk> children = new ArrayList<>(groupedTexts.size());
        for (int i = 0; i < groupedTexts.size(); i++) {
            children.add(ChildChunk.of(groupedTexts.get(i), i));
        }

        log.debug(
                "Semantic chunking completed inputLength={} maxTokens={} childCount={} avgChunkChars={}",
                sectionContent.length(),
                maxTokens,
                children.size(),
                averageChunkChars(children)
        );
        return children;
    }

    private List<String> groupSentences(String text, int maxTokens) {
        List<IndexedSentence> sentences = collectSentences(text);
        if (sentences.isEmpty()) {
            return List.of(text);
        }
        if (sentences.size() == 1) {
            return List.of(sentences.getFirst().text());
        }

        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int currentTokens = 0;
        int paragraphBreakThreshold = (int) (maxTokens * PARAGRAPH_BREAK_THRESHOLD_RATIO);

        for (IndexedSentence sentence : sentences) {
            int sentenceTokens = estimateTokens(sentence.text());

            if (!current.isEmpty()) {
                boolean wouldExceedSoftLimit = currentTokens + sentenceTokens > maxTokens;
                boolean preferParagraphBreak = sentence.paragraphStart()
                        && currentTokens >= paragraphBreakThreshold;

                if (wouldExceedSoftLimit || preferParagraphBreak) {
                    chunks.add(current.toString().trim());
                    current = new StringBuilder();
                    currentTokens = 0;
                }
            }

            if (!current.isEmpty()) {
                current.append(' ');
            }
            current.append(sentence.text());
            currentTokens += sentenceTokens;
        }

        if (!current.isEmpty()) {
            chunks.add(current.toString().trim());
        }

        return chunks.isEmpty() ? List.of(text) : chunks;
    }

    private List<IndexedSentence> collectSentences(String text) {
        List<String> paragraphs = SentenceSplitter.splitIntoParagraphs(text);
        List<IndexedSentence> sentences = new ArrayList<>();
        int index = 0;

        for (String paragraph : paragraphs) {
            List<String> paragraphSentences = SentenceSplitter.splitIntoSentences(paragraph);
            for (int sentenceIndex = 0; sentenceIndex < paragraphSentences.size(); sentenceIndex++) {
                sentences.add(new IndexedSentence(
                        index++,
                        sentenceIndex == 0,
                        paragraphSentences.get(sentenceIndex)
                ));
            }
        }
        return sentences;
    }

    static int estimateTokens(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return Math.max(1, (int) Math.ceil(text.length() / 4.0));
    }

    private int averageChunkChars(List<ChildChunk> children) {
        if (children.isEmpty()) {
            return 0;
        }
        int totalChars = children.stream().mapToInt(child -> child.content().length()).sum();
        return totalChars / children.size();
    }

    private record IndexedSentence(int index, boolean paragraphStart, String text) {
    }
}
