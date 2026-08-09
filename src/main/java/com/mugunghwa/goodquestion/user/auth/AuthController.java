package com.mugunghwa.goodquestion.user.auth;

import com.mugunghwa.goodquestion.user.auth.dto.AuthResponse;
import com.mugunghwa.goodquestion.user.auth.dto.SocialAuthResponse;
import com.mugunghwa.goodquestion.user.auth.dto.SocialLoginRequest;
import com.mugunghwa.goodquestion.user.auth.dto.LoginRequest;
import com.mugunghwa.goodquestion.user.auth.dto.LogoutRequest;
import com.mugunghwa.goodquestion.user.auth.dto.SignUpRequest;
import com.mugunghwa.goodquestion.user.auth.dto.TokenRefreshRequest;
import com.mugunghwa.goodquestion.user.auth.dto.TokenResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse signUp(@Valid @RequestBody SignUpRequest request) {
        return authService.signUp(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /**
     * 소셜 로그인. 현재 지원 공급자는 kakao 뿐이며 나머지는 501을 반환한다(미결-02).
     *
     * <p>서버가 인가 코드를 제공자 토큰으로 교환한 뒤 프로필을 조회한다(계정-04).
     * 교환에 카카오 REST API 키가 필요하므로 {@code KAKAO_CLIENT_ID}를 설정해야 한다.
     */
    @PostMapping("/social/{provider}")
    public SocialAuthResponse loginWithSocial(@PathVariable String provider,
                                              @Valid @RequestBody SocialLoginRequest request) {
        if (!"kakao".equalsIgnoreCase(provider)) {
            throw new UnsupportedOperationException("지원하지 않는 소셜 로그인 공급자입니다: " + provider);
        }
        return authService.loginWithKakao(request);
    }

    /** 리프레시 토큰 회전 재발급(계정-05). TODO: 회전 정책 구현 — 저장소는 이미 있다. */
    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody TokenRefreshRequest request) {
        throw new UnsupportedOperationException("미구현: 토큰 재발급");
    }

    /** 리프레시 토큰 무효화(계정-05). TODO: 무효화 처리 — 저장소는 이미 있다. */
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody LogoutRequest request) {
        throw new UnsupportedOperationException("미구현: 로그아웃");
    }
}
