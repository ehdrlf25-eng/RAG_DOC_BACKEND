package com.ragdoc.platform.conversation.dto;

import jakarta.validation.constraints.NotBlank;

public record SendMessageRequest(
        @NotBlank(message = "{validation.chat.content.required}")
        String content
) {
}
