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

/**
 * RAG 채팅 대화 및 메시지 처리.
 * 질문 시 사용자 소유 문서만 검색하고, LLM 답변과 출처 청크를 함께 반환한다.
 */
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

    /** 현재 사용자의 대화 목록을 최근 수정순으로 반환한다. */
    @Transactional(readOnly = true)
    public List<ConversationResponse> listConversations(Long userId) {
        return conversationRepository.findByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(ConversationResponse::from)
                .toList();
    }

    /** 새 대화 세션 생성. 제목은 클라이언트에서 지정한다. */
    @Transactional
    public ConversationResponse createConversation(Long userId, CreateConversationRequest request) {
        Conversation conversation = new Conversation();
        conversation.setUserId(userId);
        conversation.setTitle(request.title().trim());
        return ConversationResponse.from(conversationRepository.save(conversation));
    }

    /** 대화 메시지 이력 조회. 소유권 검증 후 반환한다. */
    @Transactional(readOnly = true)
    public List<ChatMessageResponse> listMessages(Long userId, Long conversationId) {
        getOwnedConversation(userId, conversationId);
        return chatMessageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId).stream()
                .map(ChatMessageResponse::from)
                .toList();
    }

    /**
     * RAG 채팅 메시지 전송.
     * 흐름: 소유권 검증 → 사용자 메시지 저장 → 벡터 검색 → 프롬프트 조립 → LLM 호출 → 어시스턴트 메시지 저장.
     */
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
        // 현재 질문을 제외한 이전 메시지만 LLM 컨텍스트에 포함
        List<ChatMessage> historyBeforeCurrent = history.stream()
                .filter(message -> !message.getId().equals(userMessage.getId()))
                .toList();
        List<ChatMessage> recentHistory = trimHistory(historyBeforeCurrent);

        // 검색 범위는 retrievalService 내부에서 userId로 제한됨
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
        // updatedAt 갱신을 위해 save 호출 (메시지 추가 시 @PreUpdate 트리거)
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

    /** 대화와 하위 메시지를 함께 삭제한다. */
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

    /** LLM 토큰 한도를 고려해 최근 N개 메시지만 대화 이력으로 사용한다. */
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

    /** 존재하지 않으면 404, 소유자가 다르면 403. */
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
