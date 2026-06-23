package com.ragdoc.platform.auth.dto;

import com.ragdoc.platform.user.User;

public record UserResponse(
        Long id,
        String email,
        String name
) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getName());
    }
}
