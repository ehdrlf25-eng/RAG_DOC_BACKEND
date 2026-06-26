package com.ragdoc.platform.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 사용자 {@link User} JPA 저장소. */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
