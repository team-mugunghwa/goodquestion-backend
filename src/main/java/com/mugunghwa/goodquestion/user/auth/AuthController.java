package com.mugunghwa.goodquestion.user.auth;

import com.mugunghwa.goodquestion.user.auth.dto.AuthResponse;
import com.mugunghwa.goodquestion.user.auth.dto.KakaoLoginRequest;
import com.mugunghwa.goodquestion.user.auth.dto.LoginRequest;
import com.mugunghwa.goodquestion.user.auth.dto.SignUpRequest;
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
     * <p>NOTE: 명세는 {@code {authorizationCode, redirectUri}}(서버가 인가 코드를 교환)를 규정하지만
     * 현재 구현은 모바일 카카오 SDK가 발급한 액세스 토큰을 받는다. 두 방식 모두 유효하므로
     * 어느 쪽을 계약으로 삼을지는 팀 확인 필요 — 경로만 명세에 맞춰 두었다.
     */
    @PostMapping("/social/{provider}")
    public AuthResponse loginWithSocial(@PathVariable String provider,
                                        @Valid @RequestBody KakaoLoginRequest request) {
        if (!"kakao".equalsIgnoreCase(provider)) {
            throw new UnsupportedOperationException("지원하지 않는 소셜 로그인 공급자입니다: " + provider);
        }
        return authService.loginWithKakao(request);
    }
}
