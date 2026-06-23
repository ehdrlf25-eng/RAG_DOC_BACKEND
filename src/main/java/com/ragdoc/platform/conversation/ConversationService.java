package com.ragdoc.platform.conversation;

import com.ragdoc.platform.common.MessageKeys;
import com.ragdoc.platform.config.RagProperties;
import com.ragdoc.platform.conversation.dto.ChatMessageResponse;
import com.ragdoc.platform.conversation.dto.ChatReplyResponse;
import com.ragdoc.platform.conversation.dto.ConversationResponse;
import com.ragdoc.platform.conversation.dto.CreateConversationRequest;
import com.ragdoc.platform.conversation.dto.SendMessageRequest;
import com.ragdoc.platform.conversation.dto.SourceChunkResponse;
import com.ragdoc.platform.rag.PromptBuilder;
import com.ragdoc.platform.rag.provider.LlmProvider;
import com.ragdoc.platform.rag.retrieval.RetrievalResult;
import com.ragdoc.platform.rag.retrieval.RetrievalService;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ConversationService {

    private static final Logger log = LoggerFactory.getLogger(ConversationService.class);

    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final RetrievalService retrievalService;
    private final LlmProvider llmProvider;
    private final PromptBuilder promptBuilder;
    private final RagProperties ragProperties;

    public ConversationService(
            ConversationRepository conversationRepository,
            ChatMessageRepository chatMessageRepository,
            RetrievalService retrievalService,
            LlmProvider llmProvider,
            PromptBuilder promptBuilder,
            RagProperties ragProperties
    ) {
        this.conversationRepository = conversationRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.retrievalService = retrievalService;
        this.llmProvider = llmProvider;
        this.promptBuilder = promptBuilder;
        this.ragProperties = ragProperties;
    }

    @Transactional(readOnly = true)
    public List<ConversationResponse> listConversations(Long userId) {
        return conversationRepository.findByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(ConversationResponse::from)
                .toList();
    }

    @Transactional
    public ConversationResponse createConversation(Long userId, CreateConversationRequest request) {
        Conversation conversation = new Conversation();
        conversation.setUserId(userId);
        conversation.setTitle(request.title().trim());
        return ConversationResponse.from(conversationRepository.save(conversation));
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> listMessages(Long userId, Long conversationId) {
        getOwnedConversation(userId, conversationId);
        return chatMessageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId).stream()
                .map(ChatMessageResponse::from)
                .toList();
    }

    @Transactional
    public ChatReplyResponse sendMessage(Long userId, Long conversationId, SendMessageRequest request) {
        Conversation conversation = getOwnedConversation(userId, conversationId);
        String question = request.content().trim();

        log.info(
                "RAG chat started userId={} conversationId={} questionLength={} topK={} reranker={}",
                userId,
                conversationId,
                question.length(),
                ragProperties.retrieval().topK(),
                ragProperties.retrieval().rerankerProvider()
        );

        ChatMessage userMessage = saveMessage(conversationId, MessageRole.USER, question);

        List<ChatMessage> history = chatMessageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
        List<ChatMessage> historyBeforeCurrent = history.stream()
                .filter(message -> !message.getId().equals(userMessage.getId()))
                .toList();
        List<ChatMessage> recentHistory = trimHistory(historyBeforeCurrent);

        List<RetrievalResult> retrievalResults = retrievalService.retrieve(userId, question);

        log.info(
                "RAG retrieval completed userId={} conversationId={} historyMessageCount={} resultCount={} results={}",
                userId,
                conversationId,
                recentHistory.size(),
                retrievalResults.size(),
                formatRetrievalSummary(retrievalResults)
        );

        String systemPrompt = promptBuilder.buildSystemPrompt();
        String userPrompt = promptBuilder.buildUserPrompt(question, recentHistory, retrievalResults);

        log.info(
                "RAG prompt prepared userId={} conversationId={} systemPromptLength={} userPromptLength={} "
                        + "retrievalResultCount={} historyMessageCount={} question={}",
                userId,
                conversationId,
                systemPrompt.length(),
                userPrompt.length(),
                retrievalResults.size(),
                recentHistory.size(),
                truncateForLog(question, 300)
        );
        if (log.isDebugEnabled()) {
            log.debug(
                    "RAG system prompt userId={} conversationId={}:\n--- SYSTEM PROMPT ---\n{}\n--- END SYSTEM PROMPT ---",
                    userId,
                    conversationId,
                    systemPrompt
            );
            log.debug(
                    "RAG user prompt userId={} conversationId={}:\n--- USER PROMPT ---\n{}\n--- END USER PROMPT ---",
                    userId,
                    conversationId,
                    userPrompt
            );
        }

        String answer = llmProvider.complete(systemPrompt, userPrompt);

        ChatMessage assistantMessage = saveMessage(conversationId, MessageRole.ASSISTANT, answer);
        conversation.setTitle(conversation.getTitle());
        conversationRepository.save(conversation);

        log.info(
                "RAG chat completed userId={} conversationId={} userMessageId={} assistantMessageId={} answerLength={}",
                userId,
                conversationId,
                userMessage.getId(),
                assistantMessage.getId(),
                answer.length()
        );

        return new ChatReplyResponse(
                ChatMessageResponse.from(userMessage),
                ChatMessageResponse.from(assistantMessage),
                retrievalResults.stream().map(this::toSource).toList()
        );
    }

    @Transactional
    public void deleteConversation(Long userId, Long conversationId) {
        Conversation conversation = getOwnedConversation(userId, conversationId);
        chatMessageRepository.deleteAll(
                chatMessageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId)
        );
        conversationRepository.delete(conversation);
    }

    private ChatMessage saveMessage(Long conversationId, MessageRole role, String content) {
        ChatMessage message = new ChatMessage();
        message.setConversationId(conversationId);
        message.setRole(role);
        message.setContent(content);
        return chatMessageRepository.save(message);
    }

    private List<ChatMessage> trimHistory(List<ChatMessage> history) {
        int maxMessages = ragProperties.retrieval().maxHistoryMessages();
        if (history.size() <= maxMessages) {
            return history;
        }
        return new ArrayList<>(history.subList(history.size() - maxMessages, history.size()));
    }

    private SourceChunkResponse toSource(RetrievalResult result) {
        return new SourceChunkResponse(
                result.documentId(),
                result.documentFilename(),
                result.sectionIndex(),
                result.chunkIndex(),
                result.parentTitle(),
                result.content(),
                result.parentContent(),
                result.score()
        );
    }

    private String formatRetrievalSummary(List<RetrievalResult> results) {
        if (results.isEmpty()) {
            return "[]";
        }
        return results.stream()
                .map(result -> String.format(
                        "{documentId=%d, sectionIndex=%d, chunkIndex=%d, score=%.4f, parentExpanded=%s, section=%s}",
                        result.documentId(),
                        result.sectionIndex(),
                        result.chunkIndex(),
                        result.score(),
                        result.parentExpanded(),
                        result.parentTitle()
                ))
                .collect(Collectors.joining(", ", "[", "]"));
    }

    private String truncateForLog(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...(truncated)";
    }

    private Conversation getOwnedConversation(Long userId, Long conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        MessageKeys.CONVERSATION_NOT_FOUND
                ));

        if (!conversation.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, MessageKeys.CONVERSATION_ACCESS_DENIED);
        }

        return conversation;
    }
}
