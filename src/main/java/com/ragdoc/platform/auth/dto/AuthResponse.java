package com.ragdoc.platform.auth.dto;

/** 로그인·회원가입 성공 시 JWT와 사용자 정보 응답 DTO. */
public record AuthResponse(
        String accessToken,
        String tokenType,
        UserResponse user
) {
    /** Bearer 토큰과 사용자 정보로 인증 응답을 생성한다. */
    public static AuthResponse of(String accessToken, UserResponse user) {
        return new AuthResponse(accessToken, "Bearer", user);
    }
}
