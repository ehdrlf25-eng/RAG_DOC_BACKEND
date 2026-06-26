package com.ragdoc.platform.conversation;

import com.ragdoc.platform.conversation.dto.ChatMessageResponse;
import com.ragdoc.platform.conversation.dto.ChatReplyResponse;
import com.ragdoc.platform.conversation.dto.ConversationResponse;
import com.ragdoc.platform.conversation.dto.CreateConversationRequest;
import com.ragdoc.platform.conversation.dto.SendMessageRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * RAG 채팅 대화 API.
 * 메시지 전송 시 백엔드에서 검색·LLM 호출이 동기적으로 수행된다.
 */
@Tag(name = "Conversations", description = "RAG 채팅 대화 API")
@RestController
@RequestMapping("/api/conversations")
@SecurityRequirement(name = "Bearer Authentication")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @Operation(summary = "대화 목록 조회")
    @GetMapping
    public List<ConversationResponse> listConversations(@AuthenticationPrincipal Long userId) {
        return conversationService.listConversations(userId);
    }

    @Operation(summary = "대화 생성")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConversationResponse createConversation(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CreateConversationRequest request
    ) {
        return conversationService.createConversation(userId, request);
    }

    @Operation(summary = "대화 메시지 목록 조회")
    @GetMapping("/{conversationId}/messages")
    public List<ChatMessageResponse> listMessages(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long conversationId
    ) {
        return conversationService.listMessages(userId, conversationId);
    }

    @Operation(summary = "메시지 전송", description = "질문을 임베딩·벡터 검색 후 LLM으로 답변을 생성합니다.")
    @PostMapping("/{conversationId}/messages")
    public ChatReplyResponse sendMessage(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long conversationId,
            @Valid @RequestBody SendMessageRequest request
    ) {
        return conversationService.sendMessage(userId, conversationId, request);
    }

    @Operation(summary = "대화 삭제")
    @DeleteMapping("/{conversationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteConversation(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long conversationId
    ) {
        conversationService.deleteConversation(userId, conversationId);
    }
}
