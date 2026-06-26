package com.ragdoc.platform.rag.provider;

/**
 * LLM 텍스트 생성 Provider 인터페이스.
 * <p>
 * RAG 채팅 응답 생성 및 LLM 리랭킹에서 사용된다.
 */
public interface LlmProvider {

    String complete(String systemPrompt, String userPrompt);
}
