package com.ragdoc.platform.user;

import com.ragdoc.platform.auth.dto.UserResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 조회·생성·수정.
 * 조회는 Redis 캐시를 경유하고, 변경 시 캐시를 갱신하거나 무효화한다.
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserCacheService userCacheService;

    public UserService(UserRepository userRepository, UserCacheService userCacheService) {
        this.userRepository = userRepository;
        this.userCacheService = userCacheService;
    }

    /** 캐시 우선 조회. 미스 시 DB에서 로드 후 캐시에 적재된다. */
    public UserResponse getById(Long userId) {
        return userCacheService.getById(userId);
    }

    /** 회원가입 등 신규 저장 후 캐시에 즉시 반영한다. */
    @Transactional
    public User create(User user) {
        User saved = userRepository.save(user);
        userCacheService.put(saved);
        return saved;
    }

    /** 프로필 변경 시 stale 캐시를 제거해 다음 조회에서 DB 값을 다시 읽는다. */
    @Transactional
    public User update(User user) {
        User saved = userRepository.save(user);
        userCacheService.evict(user.getId());
        return saved;
    }
}
