package com.ragdoc.platform.conversation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ragdoc.platform.config.RagProperties;
import com.ragdoc.platform.conversation.dto.ChatReplyResponse;
import com.ragdoc.platform.conversation.dto.SendMessageRequest;
import com.ragdoc.platform.rag.PromptBuilder;
import com.ragdoc.platform.rag.provider.LlmProvider;
import com.ragdoc.platform.rag.retrieval.RetrievalResult;
import com.ragdoc.platform.rag.retrieval.RetrievalService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private RetrievalService retrievalService;

    @Mock
    private LlmProvider llmProvider;

    @Mock
    private PromptBuilder promptBuilder;

    @Mock
    private RagProperties ragProperties;

    @InjectMocks
    private ConversationService conversationService;

    @BeforeEach
    void setUp() {
        when(ragProperties.retrieval()).thenReturn(testRetrieval());
    }

    @Test
    void sendMessageRunsRagPipeline() {
        Conversation conversation = new Conversation();
        conversation.setUserId(1L);
        conversation.setTitle("Test");

        RetrievalResult retrievalResult = new RetrievalResult(
                1L, 10L, 2L, "doc.pdf", 0, 0, "child", 0.9, "Intro", "parent", true
        );

        when(conversationRepository.findById(10L)).thenReturn(Optional.of(conversation));
        when(chatMessageRepository.findByConversationIdOrderByCreatedAtAsc(10L)).thenReturn(List.of());
        when(retrievalService.retrieve(1L, "hello")).thenReturn(List.of(retrievalResult));
        when(promptBuilder.buildSystemPrompt()).thenReturn("system");
        when(promptBuilder.buildUserPrompt(eq("hello"), any(), any())).thenReturn("user");
        when(llmProvider.complete("system", "user")).thenReturn("answer");
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage message = invocation.getArgument(0);
            if (message.getRole() == MessageRole.USER) {
                message.setContent("hello");
            } else {
                message.setContent("answer");
            }
            return message;
        });

        ChatReplyResponse response = conversationService.sendMessage(
                1L,
                10L,
                new SendMessageRequest("hello")
        );

        assertThat(response.assistantMessage().content()).isEqualTo("answer");
        assertThat(response.sources()).hasSize(1);

        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageRepository, org.mockito.Mockito.times(2)).save(messageCaptor.capture());
        assertThat(messageCaptor.getAllValues())
                .extracting(ChatMessage::getRole)
                .containsExactly(MessageRole.USER, MessageRole.ASSISTANT);
    }

    private RagProperties.Retrieval testRetrieval() {
        return new RagProperties.Retrieval(2, 5, 0.8, 20, 0.3, 60, 10, 3, "passthrough");
    }
}
