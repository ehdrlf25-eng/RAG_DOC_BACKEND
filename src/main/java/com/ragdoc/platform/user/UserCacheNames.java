package com.ragdoc.platform.user;

/**
 * Spring Cache 캐시 이름 상수.
 * {@code user} 캐시는 userId를 키로 {@link com.ragdoc.platform.auth.dto.UserResponse}를 저장한다.
 */
public final class UserCacheNames {

    /** 사용자 프로필 캐시 이름 */
    public static final String USER = "user";

    private UserCacheNames() {
    }
}
