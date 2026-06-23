package com.ragdoc.platform.conversation.dto;

import java.util.List;

public record ChatReplyResponse(
        ChatMessageResponse userMessage,
        ChatMessageResponse assistantMessage,
        List<SourceChunkResponse> sources
) {
}
