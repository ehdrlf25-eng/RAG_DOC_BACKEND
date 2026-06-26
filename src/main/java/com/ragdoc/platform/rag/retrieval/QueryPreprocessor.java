package com.ragdoc.platform.rag.retrieval;

import org.springframework.stereotype.Component;

/**
 * 검색 전 쿼리 정규화 컴포넌트.
 * <p>
 * 앞뒤 공백 제거 및 연속 공백을 단일 공백으로 치환하여 임베딩·키워드 검색 입력을 표준화한다.
 */
@Component
public class QueryPreprocessor {

    public String preprocess(String query) {
        if (query == null) {
            return "";
        }
        return query.trim().replaceAll("\\s+", " ");
    }
}
