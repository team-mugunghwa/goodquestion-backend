package com.mugunghwa.goodquestion.user.auth;

import com.mugunghwa.goodquestion.global.error.BusinessException;
import com.mugunghwa.goodquestion.global.error.ErrorCode;
import com.mugunghwa.goodquestion.global.security.JwtProvider;
import com.mugunghwa.goodquestion.user.auth.dto.*;
import com.mugunghwa.goodquestion.user.auth.kakao.KakaoClient;
import com.mugunghwa.goodquestion.user.auth.kakao.KakaoProfile;
import com.mugunghwa.goodquestion.user.auth.google.GoogleClient;
import com.mugunghwa.goodquestion.user.auth.google.GoogleProfile;
import com.mugunghwa.goodquestion.user.parent.Parent;
import com.mugunghwa.goodquestion.user.parent.ParentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 이메일/비밀번호 자체 인증 + 카카오 소셜 로그인. 토큰 전략은 Access 토큰 단일(MVP). */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final ParentRepository parentRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final KakaoClient kakaoClient;
    private final RefreshTokenService refreshTokenService;
    private final GoogleClient googleClient;
    private final LoginAttemptStore loginAttemptStore;

    @Transactional
    public AuthResponse signUp(SignUpRequest request) {
        if (parentRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }
        Parent parent = parentRepository.save(
                Parent.ofLocal(request.email(), passwordEncoder.encode(request.password()), request.name()));
        return AuthResponse.of(issueTokens(parent), parent);
    }

    public AuthResponse login(LoginRequest request) {
        loginAttemptStore.assertLoginAllowed(request.email());
        Parent parent = parentRepository.findByEmail(request.email())
                .filter(Parent::isLocal)
                .orElse(null);
        if (parent == null) {
            loginAttemptStore.recordFailure(request.email());
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        if (!passwordEncoder.matches(request.password(), parent.getPasswordHash())) {
            loginAttemptStore.recordFailure(request.email());
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        loginAttemptStore.reset(request.email());
        return AuthResponse.of(issueTokens(parent), parent);
    }

    /**
     * 소셜 로그인. 인가 코드를 서버가 제공자 토큰으로 교환한 뒤 프로필을 조회하고,
     * 최초 로그인이면 가입까지 함께 처리한다(find-or-create).
     */
    @Transactional
    public SocialAuthResponse loginWithKakao(SocialLoginRequest request) {
        String kakaoAccessToken =
                kakaoClient.exchangeCodeForToken(request.authorizationCode(), request.redirectUri());
        KakaoProfile profile = kakaoClient.getProfile(kakaoAccessToken);

        Parent existing = parentRepository
                .findByProviderAndProviderId(AuthProvider.KAKAO, profile.providerId())
                .orElse(null);
        boolean isNewUser = existing == null;
        Parent parent = isNewUser
                ? parentRepository.save(Parent.ofKakao(profile.providerId(), profile.email(), profile.nickname()))
                : existing;

        return SocialAuthResponse.of(issueTokens(parent), parent, isNewUser);
    }

    /** 리프레시 토큰 회전 재발급(계정-05). 쓴 토큰은 폐기되고 매번 새 값이 나간다. */
    @Transactional
    public TokenResponse refresh(String refreshToken) {
        RefreshTokenService.RotationResult rotated = refreshTokenService.rotate(refreshToken);
        return TokenResponse.of(
                jwtProvider.issue(rotated.parent().getId()),
                rotated.refreshToken(),
                jwtProvider.getExpiresInSeconds());
    }

    /** 로그아웃 — 리프레시 토큰을 무효화한다. Access 토큰은 만료까지 유효하므로 짧게 유지한다. */
    @Transactional
    public void logout(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
    }

    private TokenResponse issueTokens(Parent parent) {
        return TokenResponse.of(
                jwtProvider.issue(parent.getId()),
                refreshTokenService.issue(parent),
                jwtProvider.getExpiresInSeconds());
    }

    @Transactional
    public SocialAuthResponse loginWithGoogle(SocialLoginRequest request) {
        String googleAccessToken =
                googleClient.exchangeCodeForToken(request.authorizationCode(), request.redirectUri());
        GoogleProfile profile = googleClient.getProfile(googleAccessToken);

        Parent existing = parentRepository
                .findByProviderAndProviderId(AuthProvider.GOOGLE, profile.providerId())
                .orElse(null);
        boolean isNewUser = existing == null;
        Parent parent = isNewUser
                ? parentRepository.save(Parent.ofGoogle(profile.providerId(), profile.email(), profile.name()))
                : existing;

        return SocialAuthResponse.of(issueTokens(parent), parent, isNewUser);
    }
}
