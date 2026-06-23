package com.ragdoc.platform.rag.retrieval;

import com.ragdoc.platform.document.Document;
import com.ragdoc.platform.document.DocumentChunk;
import com.ragdoc.platform.document.DocumentChunkRepository;
import com.ragdoc.platform.document.DocumentRepository;
import com.ragdoc.platform.document.DocumentStatus;
import com.ragdoc.platform.document.ParentSection;
import com.ragdoc.platform.document.ParentSectionRepository;
import com.ragdoc.platform.rag.vector.ChunkSearchResult;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("test")
public class InMemoryKeywordSearchStore implements KeywordSearchStore {

    private final DocumentChunkRepository documentChunkRepository;
    private final DocumentRepository documentRepository;
    private final ParentSectionRepository parentSectionRepository;

    public InMemoryKeywordSearchStore(
            DocumentChunkRepository documentChunkRepository,
            DocumentRepository documentRepository,
            ParentSectionRepository parentSectionRepository
    ) {
        this.documentChunkRepository = documentChunkRepository;
        this.documentRepository = documentRepository;
        this.parentSectionRepository = parentSectionRepository;
    }

    @Override
    public List<ChunkSearchResult> search(Long userId, String query, int limit) {
        String normalizedQuery = query.toLowerCase(Locale.ROOT);
        List<ChunkSearchResult> results = new ArrayList<>();

        for (Document document : documentRepository.findByUserIdOrderByCreatedAtDesc(userId)) {
            if (document.getStatus() != DocumentStatus.READY) {
                continue;
            }

            Map<Long, ParentSection> parentsById = new HashMap<>();
            for (ParentSection parent : parentSectionRepository.findByDocumentIdOrderBySectionIndexAsc(document.getId())) {
                parentsById.put(parent.getId(), parent);
            }

            for (DocumentChunk chunk : documentChunkRepository.findByDocumentIdOrderByChunkIndexAsc(document.getId())) {
                if (chunk.getParentSectionId() == null) {
                    continue;
                }
                ParentSection parent = parentsById.get(chunk.getParentSectionId());
                if (parent == null) {
                    continue;
                }

                if (!matches(normalizedQuery, chunk.getContent(), parent.getTitle(), parent.getFullContent())) {
                    continue;
                }

                results.add(new ChunkSearchResult(
                        chunk.getId(),
                        parent.getId(),
                        document.getId(),
                        document.getOriginalFilename(),
                        parent.getSectionIndex(),
                        chunk.getChunkIndex(),
                        parent.getTitle(),
                        chunk.getContent(),
                        parent.getFullContent(),
                        1.0
                ));
            }
        }

        return results.stream()
                .sorted(Comparator.comparingLong(ChunkSearchResult::chunkId))
                .limit(limit)
                .toList();
    }

    private boolean matches(String query, String childContent, String parentTitle, String parentContent) {
        return contains(childContent, query)
                || contains(parentTitle, query)
                || contains(parentContent, query);
    }

    private boolean contains(String text, String query) {
        return text != null && text.toLowerCase(Locale.ROOT).contains(query);
    }
}
