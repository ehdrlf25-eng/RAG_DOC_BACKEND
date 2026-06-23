package com.ragdoc.platform.rag.retrieval;

import static org.assertj.core.api.Assertions.assertThat;

import com.ragdoc.platform.rag.vector.ChunkSearchResult;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReciprocalRankFusionTest {

    private final ReciprocalRankFusion fusion = new ReciprocalRankFusion();

    @Test
    void boostsDocumentsPresentInBothLists() {
        ChunkSearchResult shared = result(1L, 0.9);
        ChunkSearchResult denseOnly = result(2L, 0.8);
        ChunkSearchResult keywordOnly = result(3L, 0.7);

        List<ChunkSearchResult> fused = fusion.fuse(
                List.of(shared, denseOnly),
                List.of(shared, keywordOnly),
                60
        );

        assertThat(fused.getFirst().chunkId()).isEqualTo(1L);
        assertThat(fused.getFirst().score()).isGreaterThan(fused.get(1).score());
    }

    private ChunkSearchResult result(Long chunkId, double score) {
        return new ChunkSearchResult(
                chunkId,
                10L,
                2L,
                "doc.pdf",
                0,
                0,
                "Section",
                "child",
                "parent",
                score
        );
    }
}
