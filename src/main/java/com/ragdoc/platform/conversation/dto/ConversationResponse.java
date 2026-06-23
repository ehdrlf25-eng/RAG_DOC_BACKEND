package com.ragdoc.platform.conversation.dto;

import com.ragdoc.platform.conversation.Conversation;
import java.time.Instant;

public record ConversationResponse(
        Long id,
        String title,
        Instant createdAt,
        Instant updatedAt
) {

    public static ConversationResponse from(Conversation conversation) {
        return new ConversationResponse(
                conversation.getId(),
                conversation.getTitle(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt()
        );
    }
}
