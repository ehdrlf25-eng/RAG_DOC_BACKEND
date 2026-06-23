package com.ragdoc.platform.user;

import com.ragdoc.platform.common.MessageKeys;
import com.ragdoc.platform.auth.dto.UserResponse;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserCacheService {

    private final UserRepository userRepository;

    public UserCacheService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

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
        // CacheEvict handles invalidation.
    }
}
