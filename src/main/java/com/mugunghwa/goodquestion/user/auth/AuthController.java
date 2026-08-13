package com.mugunghwa.goodquestion.user.auth;

import com.mugunghwa.goodquestion.global.security.ClientIpResolver;
import com.mugunghwa.goodquestion.user.auth.dto.AuthResponse;
import com.mugunghwa.goodquestion.user.auth.dto.SocialAuthResponse;
import com.mugunghwa.goodquestion.user.auth.dto.SocialLoginRequest;
import com.mugunghwa.goodquestion.user.auth.dto.FindEmailRequest;
import com.mugunghwa.goodquestion.user.auth.dto.FindEmailResponse;
import com.mugunghwa.goodquestion.user.auth.dto.LoginRequest;
import com.mugunghwa.goodquestion.user.auth.dto.LogoutRequest;
import com.mugunghwa.goodquestion.user.auth.dto.PasswordResetConfirmRequest;
import com.mugunghwa.goodquestion.user.auth.dto.PasswordResetRequest;
import com.mugunghwa.goodquestion.user.auth.dto.SignUpRequest;
import com.mugunghwa.goodquestion.user.auth.dto.TokenRefreshRequest;
import com.mugunghwa.goodquestion.user.auth.dto.TokenResponse;
import jakarta.servlet.http.HttpServletRequest;
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
    private final FindEmailService findEmailService;

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse signUp(@Valid @RequestBody SignUpRequest request,
                               HttpServletRequest servletRequest) {
        return authService.signUp(request, ClientIpResolver.resolve(servletRequest));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request,
                              HttpServletRequest servletRequest) {
        return authService.login(request, ClientIpResolver.resolve(servletRequest));
    }

    /** 비밀번호 재설정 메일 발송(계정-06). 계정 존재 여부를 노출하지 않으려고 항상 202를 돌려준다. */
    @PostMapping("/password-reset/request")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        passwordResetService.request(request.email());
    }

    /** 비밀번호 재설정 확정(계정-06). 토큰은 1회용이다. */
    @PostMapping("/password-reset/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
        passwordResetService.confirm(request.token(), request.newPassword());
    }

    /** 이메일(ID) 찾기(계정-07). 매치가 없어도 200과 빈 리스트 — 존재 여부를 에러로 구분하지 않는다. */
    @PostMapping("/find-email")
    public FindEmailResponse findEmail(@Valid @RequestBody FindEmailRequest request) {
        return findEmailService.find(request);
    }

    /**
     * 소셜 로그인. 현재 지원 공급자는 kakao 뿐이며 나머지는 501을 반환한다(미결-02).
     *
     * <p>서버가 인가 코드를 제공자 토큰으로 교환한 뒤 프로필을 조회한다(계정-04).
     * 교환에 카카오 REST API 키가 필요하므로 {@code KAKAO_CLIENT_ID}를 설정해야 한다.
     */
    @PostMapping("/social/{provider}")
    public SocialAuthResponse loginWithSocial(@PathVariable String provider,
                                              @Valid @RequestBody SocialLoginRequest request,
                                              HttpServletRequest servletRequest) {
        if (!"kakao".equalsIgnoreCase(provider)) {
            throw new UnsupportedOperationException("지원하지 않는 소셜 로그인 공급자입니다: " + provider);
        }
        return authService.loginWithKakao(request, ClientIpResolver.resolve(servletRequest));
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
