package com.ragdoc.platform.rag.retrieval;

import org.springframework.stereotype.Component;

@Component
public class QueryPreprocessor {

    public String preprocess(String query) {
        if (query == null) {
            return "";
        }
        return query.trim().replaceAll("\\s+", " ");
    }
}
