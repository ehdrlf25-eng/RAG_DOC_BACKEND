package com.ragdoc.platform.rag.vector;

import com.ragdoc.platform.document.Document;
import com.ragdoc.platform.document.DocumentChunk;
import com.ragdoc.platform.document.DocumentChunkRepository;
import com.ragdoc.platform.document.DocumentRepository;
import com.ragdoc.platform.document.DocumentStatus;
import com.ragdoc.platform.document.ParentSection;
import com.ragdoc.platform.document.ParentSectionRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 테스트 프로필용 인메모리 벡터 저장소.
 * <p>
 * HashMap에 임베딩을 보관하고 Java 코사인 유사도로 밀집 검색을 시뮬레이션한다.
 */
@Component
@Profile("test")
public class InMemoryVectorStore implements VectorStore {

    private final DocumentChunkRepository documentChunkRepository;
    private final DocumentRepository documentRepository;
    private final ParentSectionRepository parentSectionRepository;
    private final Map<Long, float[]> embeddings = new HashMap<>();

    public InMemoryVectorStore(
            DocumentChunkRepository documentChunkRepository,
            DocumentRepository documentRepository,
            ParentSectionRepository parentSectionRepository
    ) {
        this.documentChunkRepository = documentChunkRepository;
        this.documentRepository = documentRepository;
        this.parentSectionRepository = parentSectionRepository;
    }

    @Override
    public void saveEmbedding(Long chunkId, float[] embedding) {
        embeddings.put(chunkId, embedding);
    }

    @Override
    public List<ChunkSearchResult> searchSimilar(
            Long userId,
            float[] queryEmbedding,
            int topK,
            double minSimilarity
    ) {
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

                float[] embedding = embeddings.get(chunk.getId());
                if (embedding == null) {
                    continue;
                }

                ParentSection parent = parentsById.get(chunk.getParentSectionId());
                if (parent == null) {
                    continue;
                }

                double score = cosineSimilarity(queryEmbedding, embedding);
                if (score < minSimilarity) {
                    continue; // pgvector minSimilarity 필터와 동일한 동작
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
                        score
                ));
            }
        }

        return results.stream()
                .sorted(Comparator.comparingDouble(ChunkSearchResult::score).reversed())
                .limit(topK)
                .toList();
    }

    private double cosineSimilarity(float[] left, float[] right) {
        int length = Math.min(left.length, right.length);
        double dot = 0;
        double leftNorm = 0;
        double rightNorm = 0;
        for (int i = 0; i < length; i++) {
            dot += left[i] * right[i];
            leftNorm += left[i] * left[i];
            rightNorm += right[i] * right[i];
        }
        if (leftNorm == 0 || rightNorm == 0) {
            return 0;
        }
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }
}
