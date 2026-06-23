package com.ragdoc.platform.rag.ingestion;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class SentenceSplitterTest {

    @Test
    void splitsParagraphsOnBlankLines() {
        List<String> paragraphs = SentenceSplitter.splitIntoParagraphs("First block.\n\nSecond block.");
        assertThat(paragraphs).containsExactly("First block.", "Second block.");
    }

    @Test
    void splitsSentencesOnPunctuation() {
        List<String> sentences = SentenceSplitter.splitIntoSentences(
                "Hello world. How are you? I am fine!"
        );
        assertThat(sentences).containsExactly(
                "Hello world.",
                "How are you?",
                "I am fine!"
        );
    }
}
