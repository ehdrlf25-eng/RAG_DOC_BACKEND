package com.ragdoc.platform.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** 로그인 요청 DTO. */
public record LoginRequest(
        @NotBlank(message = "{validation.email.required}")
        @Email(message = "{validation.email.invalid}")
        String email,
        @NotBlank(message = "{validation.password.required}")
        String password
) {
}
