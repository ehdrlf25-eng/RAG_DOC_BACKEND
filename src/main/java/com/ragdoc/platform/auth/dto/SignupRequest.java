package com.ragdoc.platform.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank(message = "{validation.email.required}")
        @Email(message = "{validation.email.invalid}")
        String email,
        @NotBlank(message = "{validation.password.required}")
        @Size(min = 8, max = 100, message = "{validation.password.size}")
        String password,
        @NotBlank(message = "{validation.name.required}")
        @Size(min = 2, max = 100, message = "{validation.name.size}")
        String name
) {
}
