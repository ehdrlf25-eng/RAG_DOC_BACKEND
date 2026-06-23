package com.ragdoc.platform.rag.provider;

public interface LlmProvider {

    String complete(String systemPrompt, String userPrompt);
}
