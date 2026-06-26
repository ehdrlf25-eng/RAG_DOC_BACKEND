package com.ragdoc.platform.rag.retrieval;

import com.ragdoc.platform.rag.vector.ChunkSearchResult;
import java.util.List;

/**
 * 키워드(희소) 검색 저장소 인터페이스.
 * <p>
 * 하이브리드 검색의 키워드 검색 경로에서 child chunk를 텍스트 매칭으로 조회한다.
 */
public interface KeywordSearchStore {

    List<ChunkSearchResult> search(Long userId, String query, int limit);
}
