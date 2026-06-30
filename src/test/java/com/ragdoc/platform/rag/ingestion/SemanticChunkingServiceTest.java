package com.ragdoc.platform.rag.ingestion;

import static org.assertj.core.api.Assertions.assertThat;

import com.ragdoc.platform.config.RagProperties;
import com.ragdoc.platform.rag.parentchild.ChildChunk;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SemanticChunkingServiceTest {

    private SemanticChunkingService semanticChunkingService;

    @BeforeEach
    void setUp() {
        semanticChunkingService = new SemanticChunkingService(testProperties(30));
    }

    @Test
    void keepsShortTextAsSingleChild() {
        List<ChildChunk> children = semanticChunkingService.chunk("Short section body.");
        assertThat(children).hasSize(1);
        assertThat(children.getFirst().content()).isEqualTo("Short section body.");
    }

    @Test
    void groupsSentencesWithoutSplittingMidSentence() {
        String text = "First sentence here. Second sentence follows. Third sentence ends.";
        List<ChildChunk> children = semanticChunkingService.chunk(text);

        assertThat(children).hasSize(1);
        assertThat(children.getFirst().content()).isEqualTo(text);
    }

    @Test
    void startsNewChunkWhenSoftTokenLimitExceeded() {
        String text = """
                Alpha sentence one. Beta sentence two. Gamma sentence three.
                Delta sentence four. Epsilon sentence five. Zeta sentence six.
                """;
        List<ChildChunk> children = semanticChunkingService.chunk(text);

        assertThat(children.size()).isGreaterThan(1);
        for (ChildChunk child : children) {
            assertThat(child.content()).matches(".*[.!?。！？].*");
        }
    }

    @Test
    void prefersParagraphBoundaryWhenNearSoftLimit() {
        SemanticChunkingService service = new SemanticChunkingService(testProperties(20));
        String text = """
                Sentence one in first paragraph. Sentence two in first paragraph. Sentence three in first paragraph.
                
                Sentence one in second paragraph. Sentence two in second paragraph.
                """;

        List<ChildChunk> children = service.chunk(text);

        assertThat(children.size()).isGreaterThanOrEqualTo(2);
        assertThat(children.get(0).content()).contains("first paragraph");
        assertThat(children.get(1).content()).contains("second paragraph");
    }

    @Test
    void keepsOversizedSingleSentenceIntact() {
        String longSentence = "Word ".repeat(80).trim() + ".";
        List<ChildChunk> children = semanticChunkingService.chunk(longSentence);

        assertThat(children).hasSize(1);
        assertThat(children.getFirst().content()).isEqualTo(longSentence);
    }

    private RagProperties testProperties(int maxTokens) {
        return new RagProperties(
                new RagProperties.Storage("local", "./storage", "", "ap-northeast-2"),
                new RagProperties.Retrieval(5, 10, 0.8, 40, 0.3, 60, 20, 5, "passthrough"),
                new RagProperties.Chunking(maxTokens),
                new RagProperties.Embedding("mock", 768, null, null),
                new RagProperties.Llm("mock", null, null)
        );
    }
}
