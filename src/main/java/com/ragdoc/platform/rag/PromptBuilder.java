package com.ragdoc.platform.rag;

import com.ragdoc.platform.conversation.ChatMessage;
import com.ragdoc.platform.conversation.MessageRole;
import com.ragdoc.platform.rag.retrieval.RetrievalResult;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * RAG 채팅용 LLM 프롬프트 조립 컴포넌트.
 * <p>
 * Retrieval 결과와 대화 이력을 결합하여 system/user 프롬프트를 생성한다.
 */
@Component
public class PromptBuilder {

    /** LLM 행동 지침: 컨텍스트 기반 답변, 정보 부족 시 명시, 사용자 언어로 응답. */
    public String buildSystemPrompt() {
        return """
                You are a helpful document assistant.
                Answer the user's question using only the provided context and conversation history.
                If the answer is not in the context, say you do not have enough information.
                Respond in the same language as the user's question.
                """;
    }

    /**
     * 검색 컨텍스트, 대화 이력, 사용자 질문을 하나의 user 프롬프트로 조립한다.
     * <p>
     * Parent 확장이 적용된 결과는 {@link RetrievalResult#contextContent()}로 전체 섹션 본문을 사용한다.
     */
    public String buildUserPrompt(
            String question,
            List<ChatMessage> history,
            List<RetrievalResult> retrievalResults
    ) {
        StringBuilder builder = new StringBuilder();

        builder.append("Context:\n");
        if (retrievalResults.isEmpty()) {
            builder.append("(no relevant document chunks found)\n");
        } else {
            for (int i = 0; i < retrievalResults.size(); i++) {
                RetrievalResult result = retrievalResults.get(i);
                builder.append("[")
                        .append(i + 1)
                        .append("] ")
                        .append(result.documentFilename())
                        .append(" — ")
                        .append(result.parentTitle())
                        .append(" (section ")
                        .append(result.sectionIndex())
                        .append(")\n")
                        .append(result.contextContent())
                        .append("\n---\n");
            }
        }

        builder.append("\nConversation history:\n");
        if (history.isEmpty()) {
            builder.append("(none)\n");
        } else {
            for (ChatMessage message : history) {
                builder.append(roleLabel(message.getRole()))
                        .append(": ")
                        .append(message.getContent())
                        .append('\n');
            }
        }

        builder.append("\nUser question:\n")
                .append(question);

        return builder.toString();
    }

    private String roleLabel(MessageRole role) {
        return role == MessageRole.USER ? "User" : "Assistant";
    }
}
