package com.ragdoc.platform.rag.retrieval;

import static org.assertj.core.api.Assertions.assertThat;

import com.ragdoc.platform.rag.vector.ChunkSearchResult;
import java.util.List;
import org.junit.jupiter.api.Test;

class ParentExpansionServiceTest {

    private final ParentExpansionService parentExpansionService = new ParentExpansionService();

    @Test
    void expandsOnlyFirstDistinctParentsUpToLimit() {
        List<ChunkSearchResult> chunks = List.of(
                result(1L, 10L, 0.9),
                result(2L, 10L, 0.85),
                result(3L, 20L, 0.8)
        );

        List<RetrievalResult> expanded = parentExpansionService.applyParentExpansion(chunks, 2);

        assertThat(expanded).hasSize(3);
        assertThat(expanded.get(0).parentExpanded()).isTrue();
        assertThat(expanded.get(1).parentExpanded()).isFalse();
        assertThat(expanded.get(2).parentExpanded()).isTrue();
    }

    private ChunkSearchResult result(Long chunkId, Long parentId, double score) {
        return new ChunkSearchResult(
                chunkId,
                parentId,
                1L,
                "doc.pdf",
                0,
                0,
                "Section",
                "child",
                "parent full",
                score
        );
    }
}
