package com.ragdoc.platform.rag.provider.mock;

import com.ragdoc.platform.rag.provider.LlmProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.rag.llm", name = "provider", havingValue = "mock", matchIfMissing = true)
public class MockLlmProvider implements LlmProvider {

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        return "Mock LLM response based on your question and retrieved context.";
    }
}
