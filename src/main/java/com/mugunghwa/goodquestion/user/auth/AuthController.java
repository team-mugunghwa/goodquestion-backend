package com.mugunghwa.goodquestion.user.auth;

import com.mugunghwa.goodquestion.user.auth.dto.AuthResponse;
import com.mugunghwa.goodquestion.user.auth.dto.SocialAuthResponse;
import com.mugunghwa.goodquestion.user.auth.dto.SocialLoginRequest;
import com.mugunghwa.goodquestion.user.auth.dto.LoginRequest;
import com.mugunghwa.goodquestion.user.auth.dto.LogoutRequest;
import com.mugunghwa.goodquestion.user.auth.dto.SignUpRequest;
import com.mugunghwa.goodquestion.user.auth.dto.TokenRefreshRequest;
import com.mugunghwa.goodquestion.user.auth.dto.TokenResponse;
import com.mugunghwa.goodquestion.user.auth.dto.PasswordResetConfirmRequest;
import com.mugunghwa.goodquestion.user.auth.dto.PasswordResetRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse signUp(@Valid @RequestBody SignUpRequest request) {
        return authService.signUp(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/password-reset/request")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        passwordResetService.request(request.email());
    }

    @PostMapping("/password-reset/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
        passwordResetService.confirm(request.token(), request.newPassword());
    }

    /**
     * 소셜 로그인. 카카오와 구글을 지원한다.
     *
     * <p>서버가 인가 코드를 제공자 토큰으로 교환한 뒤 프로필을 조회한다(계정-04).
     * 공급자별 client ID와 웹 클라이언트 secret을 환경 변수로 설정해야 한다.
     */
    @PostMapping("/social/{provider}")
    public SocialAuthResponse loginWithSocial(@PathVariable String provider,
                                              @Valid @RequestBody SocialLoginRequest request) {
        return switch (provider.toLowerCase()) {
            case "kakao" -> authService.loginWithKakao(request);
            case "google" -> authService.loginWithGoogle(request);
            default -> throw new UnsupportedOperationException(
                    "지원하지 않는 소셜 로그인 공급자입니다: " + provider);
        };
    }

    /** 리프레시 토큰 회전 재발급(계정-05). */
    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody TokenRefreshRequest request) {
        return authService.refresh(request.refreshToken());
    }

    /** 리프레시 토큰 무효화(계정-05). */
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request.refreshToken());
    }
}
