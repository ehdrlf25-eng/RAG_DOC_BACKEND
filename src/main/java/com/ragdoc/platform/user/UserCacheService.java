package com.ragdoc.platform.user;

import com.ragdoc.platform.common.MessageKeys;
import com.ragdoc.platform.auth.dto.UserResponse;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * 사용자 정보 Redis 캐시 (Cache Aside).
 * 캐시 이름·TTL은 {@link com.ragdoc.platform.config.CacheConfig}에서 설정한다.
 */
@Service
public class UserCacheService {

    private final UserRepository userRepository;

    public UserCacheService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** DB 조회 결과를 UserResponse DTO로 캐시한다. 비밀번호는 DTO에 포함되지 않는다. */
    @Cacheable(cacheNames = UserCacheNames.USER, key = "#userId")
    public UserResponse getById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, MessageKeys.USER_NOT_FOUND));
        return UserResponse.from(user);
    }

    @CachePut(cacheNames = UserCacheNames.USER, key = "#user.id")
    public UserResponse put(User user) {
        return UserResponse.from(user);
    }

    @CacheEvict(cacheNames = UserCacheNames.USER, key = "#userId")
    public void evict(Long userId) {
        // @CacheEvict가 실제 무효화를 수행한다.
    }
}
