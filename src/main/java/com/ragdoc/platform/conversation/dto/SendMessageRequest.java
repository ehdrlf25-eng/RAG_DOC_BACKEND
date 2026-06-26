package com.ragdoc.platform.conversation.dto;

import jakarta.validation.constraints.NotBlank;

/** 대화에 메시지를 전송할 때 사용하는 요청 DTO. */
public record SendMessageRequest(
        @NotBlank(message = "{validation.chat.content.required}")
        String content
) {
}
