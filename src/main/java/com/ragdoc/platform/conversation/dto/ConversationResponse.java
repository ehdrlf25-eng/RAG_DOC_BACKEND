package com.ragdoc.platform.conversation.dto;

import com.ragdoc.platform.conversation.Conversation;
import java.time.Instant;

/** 대화 목록·상세 API 응답 DTO. */
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
