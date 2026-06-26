package com.ragdoc.platform.auth.dto;

import com.ragdoc.platform.user.User;

/** 인증된 사용자 정보 API 응답 DTO. */
public record UserResponse(
        Long id,
        String email,
        String name
) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getName());
    }
}
