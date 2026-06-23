package com.ragdoc.platform.conversation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateConversationRequest(
        @NotBlank(message = "{validation.conversation.title.required}")
        @Size(max = 200, message = "{validation.conversation.title.size}")
        String title
) {
}
