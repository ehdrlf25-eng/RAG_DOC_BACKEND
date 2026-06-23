package com.ragdoc.platform.rag;

import com.ragdoc.platform.conversation.ChatMessage;
import com.ragdoc.platform.conversation.MessageRole;
import com.ragdoc.platform.rag.retrieval.RetrievalResult;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PromptBuilder {

    public String buildSystemPrompt() {
        return """
                You are a helpful document assistant.
                Answer the user's question using only the provided context and conversation history.
                If the answer is not in the context, say you do not have enough information.
                Respond in the same language as the user's question.
                """;
    }

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
