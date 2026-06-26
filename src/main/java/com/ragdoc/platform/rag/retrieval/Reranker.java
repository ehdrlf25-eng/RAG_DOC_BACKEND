package com.ragdoc.platform.rag.retrieval;

import com.ragdoc.platform.rag.vector.ChunkSearchResult;
import java.util.List;

/**
 * 검색 후보 재정렬 인터페이스.
 * <p>
 * 하이브리드 검색(RRF) 결과를 쿼리 관련도 기준으로 재정렬한다.
 */
public interface Reranker {

    List<ChunkSearchResult> rerank(String query, List<ChunkSearchResult> candidates);
}
