package com.ragdoc.platform.conversation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 새 대화 생성 요청 DTO. */
public record CreateConversationRequest(
        @NotBlank(message = "{validation.conversation.title.required}")
        @Size(max = 200, message = "{validation.conversation.title.size}")
        String title
) {
}
