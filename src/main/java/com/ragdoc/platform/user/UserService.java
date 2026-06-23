package com.ragdoc.platform.user;

import com.ragdoc.platform.auth.dto.UserResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserCacheService userCacheService;

    public UserService(UserRepository userRepository, UserCacheService userCacheService) {
        this.userRepository = userRepository;
        this.userCacheService = userCacheService;
    }

    public UserResponse getById(Long userId) {
        return userCacheService.getById(userId);
    }

    @Transactional
    public User create(User user) {
        User saved = userRepository.save(user);
        userCacheService.put(saved);
        return saved;
    }

    @Transactional
    public User update(User user) {
        User saved = userRepository.save(user);
        userCacheService.evict(user.getId());
        return saved;
    }
}
