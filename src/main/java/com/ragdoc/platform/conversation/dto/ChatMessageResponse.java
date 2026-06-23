package com.ragdoc.platform.conversation.dto;

import com.ragdoc.platform.conversation.ChatMessage;
import com.ragdoc.platform.conversation.MessageRole;
import java.time.Instant;

public record ChatMessageResponse(
        Long id,
        MessageRole role,
        String content,
        Instant createdAt
) {

    public static ChatMessageResponse from(ChatMessage message) {
        return new ChatMessageResponse(
                message.getId(),
                message.getRole(),
                message.getContent(),
                message.getCreatedAt()
        );
    }
}
