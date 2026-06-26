package com.ragdoc.platform.auth;

import com.ragdoc.platform.auth.dto.AuthResponse;
import com.ragdoc.platform.auth.dto.LoginRequest;
import com.ragdoc.platform.auth.dto.SignupRequest;
import com.ragdoc.platform.auth.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증 API.
 * {@code /signup}, {@code /login}은 공개 경로이며, {@code /me}는 JWT 인증이 필요하다.
 */
@Tag(name = "Auth", description = "인증 API")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "회원가입", description = "새 사용자를 등록하고 JWT Access Token을 발급합니다.")
    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse signup(@Valid @RequestBody SignupRequest request) {
        return authService.signup(request);
    }

    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인하고 JWT Access Token을 발급합니다.")
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @Operation(
            summary = "현재 사용자 조회",
            description = "JWT 토큰으로 인증된 현재 사용자 정보를 반환합니다.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal Long userId) {
        return authService.getCurrentUser(userId);
    }
}
