package com.ragdoc.platform.auth;

import com.ragdoc.platform.auth.dto.AuthResponse;
import com.ragdoc.platform.auth.dto.LoginRequest;
import com.ragdoc.platform.auth.dto.SignupRequest;
import com.ragdoc.platform.auth.dto.UserResponse;
import com.ragdoc.platform.common.MessageKeys;
import com.ragdoc.platform.user.User;
import com.ragdoc.platform.user.UserRepository;
import com.ragdoc.platform.user.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(
            UserRepository userRepository,
            UserService userService,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider
    ) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Transactional
    public AuthResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, MessageKeys.AUTH_EMAIL_ALREADY_EXISTS);
        }

        User user = new User();
        user.setEmail(request.email());
        user.setName(request.name());
        user.setPassword(passwordEncoder.encode(request.password()));

        User saved = userService.create(user);
        String token = jwtTokenProvider.createToken(saved.getId(), saved.getEmail());

        return AuthResponse.of(token, UserResponse.from(saved));
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException(MessageKeys.AUTH_INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BadCredentialsException(MessageKeys.AUTH_INVALID_CREDENTIALS);
        }

        String token = jwtTokenProvider.createToken(user.getId(), user.getEmail());
        return AuthResponse.of(token, UserResponse.from(user));
    }

    public UserResponse getCurrentUser(Long userId) {
        return userService.getById(userId);
    }
}
