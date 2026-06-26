package com.ragdoc.platform.rag.provider.mock;

import com.ragdoc.platform.rag.provider.LlmProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 테스트·로컬 개발용 고정 응답 LLM Provider.
 * <p>
 * 외부 LLM API 없이 RAG 파이프라인 통합 테스트를 지원한다.
 * 기본 LLM provider({@code matchIfMissing = true})로 사용된다.
 */
@Component
@ConditionalOnProperty(prefix = "app.rag.llm", name = "provider", havingValue = "mock", matchIfMissing = true)
public class MockLlmProvider implements LlmProvider {

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        return "Mock LLM response based on your question and retrieved context.";
    }
}
