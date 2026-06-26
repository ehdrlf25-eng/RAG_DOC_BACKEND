package com.ragdoc.platform.conversation.dto;

import java.util.List;

/** 메시지 전송 API 응답. 사용자 질문, LLM 답변, 참조 출처를 함께 담는다. */
public record ChatReplyResponse(
        ChatMessageResponse userMessage,
        ChatMessageResponse assistantMessage,
        List<SourceChunkResponse> sources
) {
}
