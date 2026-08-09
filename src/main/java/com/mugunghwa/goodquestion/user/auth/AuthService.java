package com.mugunghwa.goodquestion.user.auth;

import com.mugunghwa.goodquestion.global.error.BusinessException;
import com.mugunghwa.goodquestion.global.error.ErrorCode;
import com.mugunghwa.goodquestion.global.security.JwtProvider;
import com.mugunghwa.goodquestion.user.auth.dto.AuthResponse;
import com.mugunghwa.goodquestion.user.auth.dto.SocialAuthResponse;
import com.mugunghwa.goodquestion.user.auth.dto.SocialLoginRequest;
import com.mugunghwa.goodquestion.user.auth.dto.LoginRequest;
import com.mugunghwa.goodquestion.user.auth.dto.SignUpRequest;
import com.mugunghwa.goodquestion.user.auth.kakao.KakaoClient;
import com.mugunghwa.goodquestion.user.auth.kakao.KakaoProfile;
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

    @Transactional
    public AuthResponse signUp(SignUpRequest request) {
        if (parentRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }
        Parent parent = parentRepository.save(
                Parent.ofLocal(request.email(), passwordEncoder.encode(request.password()), request.name()));
        return AuthResponse.of(jwtProvider.issue(parent.getId()), jwtProvider.getExpiresInSeconds(), parent);
    }

    public AuthResponse login(LoginRequest request) {
        Parent parent = parentRepository.findByEmail(request.email())
                .filter(Parent::isLocal)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));
        if (!passwordEncoder.matches(request.password(), parent.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        return AuthResponse.of(jwtProvider.issue(parent.getId()), jwtProvider.getExpiresInSeconds(), parent);
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

        return SocialAuthResponse.of(
                jwtProvider.issue(parent.getId()), jwtProvider.getExpiresInSeconds(), parent, isNewUser);
    }
}
