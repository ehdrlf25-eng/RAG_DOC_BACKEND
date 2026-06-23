package com.ragdoc.platform.rag.provider.ollama;

import com.ragdoc.platform.config.RagProperties;
import com.ragdoc.platform.rag.provider.LlmProvider;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(prefix = "app.rag.llm", name = "provider", havingValue = "ollama")
public class OllamaLlmProvider implements LlmProvider {

    private final RestClient restClient;
    private final String model;

    public OllamaLlmProvider(RagProperties ragProperties) {
        RagProperties.Ollama ollama = ragProperties.llm().ollama();
        this.model = ollama.model();
        this.restClient = RestClient.builder()
                .baseUrl(ollama.baseUrl())
                .build();
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        ChatResponse response = restClient.post()
                .uri("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ChatRequest(
                        model,
                        List.of(
                                new ChatMessage("system", systemPrompt),
                                new ChatMessage("user", userPrompt)
                        ),
                        false
                ))
                .retrieve()
                .body(ChatResponse.class);

        if (response == null || response.message() == null || response.message().content() == null) {
            throw new IllegalStateException("Ollama chat response was empty.");
        }

        return response.message().content();
    }

    private record ChatRequest(String model, List<ChatMessage> messages, boolean stream) {
    }

    private record ChatMessage(String role, String content) {
    }

    private record ChatResponse(ChatMessage message) {
    }
}
