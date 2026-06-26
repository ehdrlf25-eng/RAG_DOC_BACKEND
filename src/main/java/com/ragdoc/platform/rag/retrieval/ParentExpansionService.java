package com.ragdoc.platform.rag.retrieval;

import com.ragdoc.platform.rag.vector.ChunkSearchResult;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Parent-Child 청킹 구조에서 Child 검색 결과를 Parent 섹션 전체로 확장하는 서비스.
 * <p>
 * Child chunk만으로는 맥락이 부족한 경우, 동일 parent section의 전체 본문을
 * LLM 컨텍스트에 포함시켜 답변 품질을 높인다.
 */
@Service
public class ParentExpansionService {

    /**
     * 상위 순위 child chunk에 대해 parent 확장 여부를 결정한다.
     * <p>
     * 동일 parent section은 {@code parentExpansionLimit} 횟수까지만 확장한다.
     *
     * @param rankedChunks         리랭킹 완료된 child chunk 목록 (점수순)
     * @param parentExpansionLimit parent 전체 본문 확장 최대 횟수
     */
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
            // LinkedHashSet.add()는 중복 parent ID에 대해 false를 반환 → 확장 한도 내 최초 등장만 확장
            boolean expandParent = chunk.parentSectionId() != null
                    && expandedParentIds.size() < parentExpansionLimit
                    && expandedParentIds.add(chunk.parentSectionId());
            results.add(RetrievalResult.from(chunk, expandParent));
        }

        return results;
    }
}
