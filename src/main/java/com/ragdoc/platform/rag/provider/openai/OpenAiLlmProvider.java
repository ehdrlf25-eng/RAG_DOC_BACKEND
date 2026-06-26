package com.ragdoc.platform.rag.provider.openai;

import com.ragdoc.platform.config.RagProperties;
import com.ragdoc.platform.rag.provider.LlmProvider;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * OpenAI API 기반 LLM Provider.
 * <p>
 * {@code app.rag.llm.provider=openai} 설정 시 활성화된다.
 */
@Component
@ConditionalOnProperty(prefix = "app.rag.llm", name = "provider", havingValue = "openai")
public class OpenAiLlmProvider implements LlmProvider {

    private final RestClient restClient;
    private final String model;

    public OpenAiLlmProvider(RagProperties ragProperties) {
        RagProperties.OpenAi openAi = ragProperties.llm().openai();
        this.model = openAi.model();
        this.restClient = RestClient.builder()
                .baseUrl(openAi.baseUrl())
                .defaultHeader("Authorization", "Bearer " + openAi.apiKey())
                .build();
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        ChatResponse response = restClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ChatRequest(
                        model,
                        List.of(
                                new ChatMessage("system", systemPrompt),
                                new ChatMessage("user", userPrompt)
                        )
                ))
                .retrieve()
                .body(ChatResponse.class);

        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new IllegalStateException("OpenAI chat response was empty.");
        }

        return response.choices().getFirst().message().content();
    }

    private record ChatRequest(String model, List<ChatMessage> messages) {
    }

    private record ChatMessage(String role, String content) {
    }

    private record ChatResponse(List<Choice> choices) {
    }

    private record Choice(ChatMessage message) {
    }
}
