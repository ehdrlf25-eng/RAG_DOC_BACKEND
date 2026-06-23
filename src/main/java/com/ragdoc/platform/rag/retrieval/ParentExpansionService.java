package com.ragdoc.platform.rag.retrieval;

import com.ragdoc.platform.rag.vector.ChunkSearchResult;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class ParentExpansionService {

    public List<RetrievalResult> applyParentExpansion(
            List<ChunkSearchResult> rankedChunks,
            int parentExpansionLimit
    ) {
        if (rankedChunks == null || rankedChunks.isEmpty()) {
            return List.of();
        }

        Set<Long> expandedParentIds = new LinkedHashSet<>();
        List<RetrievalResult> results = new ArrayList<>(rankedChunks.size());

        for (ChunkSearchResult chunk : rankedChunks) {
            boolean expandParent = chunk.parentSectionId() != null
                    && expandedParentIds.size() < parentExpansionLimit
                    && expandedParentIds.add(chunk.parentSectionId());
            results.add(RetrievalResult.from(chunk, expandParent));
        }

        return results;
    }
}
