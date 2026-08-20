package com.mugunghwa.goodquestion.user.auth;

import com.mugunghwa.goodquestion.global.security.JwtProvider;
import com.mugunghwa.goodquestion.user.auth.dto.SocialAuthResponse;
import com.mugunghwa.goodquestion.user.auth.dto.SocialLoginRequest;
import com.mugunghwa.goodquestion.user.auth.google.GoogleClient;
import com.mugunghwa.goodquestion.user.auth.google.GoogleProfile;
import com.mugunghwa.goodquestion.user.auth.kakao.KakaoClient;
import com.mugunghwa.goodquestion.user.parent.Parent;
import com.mugunghwa.goodquestion.user.parent.ParentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceGoogleTest {

    @Mock
    private ParentRepository parentRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtProvider jwtProvider;
    @Mock
    private KakaoClient kakaoClient;
    @Mock
    private GoogleClient googleClient;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private LoginAttemptService loginAttemptService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AuthService authService;

    @Test
    void 기존_구글_계정은_서비스_JWT를_발급한다() {
        UUID parentId = UUID.randomUUID();
        Parent parent = org.mockito.Mockito.mock(Parent.class);
        SocialLoginRequest request = new SocialLoginRequest("authorization-code", "http://localhost:7357/auth.html");

        when(googleClient.exchangeCodeForToken(request.authorizationCode(), request.redirectUri()))
                .thenReturn("google-access-token");
        when(googleClient.getProfile("google-access-token"))
                .thenReturn(new GoogleProfile("google-sub", "parent@example.com", "보호자"));
        when(parentRepository.findByProviderAndProviderId(AuthProvider.GOOGLE, "google-sub"))
                .thenReturn(Optional.of(parent));
        when(parent.getId()).thenReturn(parentId);
        when(parent.getEmail()).thenReturn("parent@example.com");
        when(parent.getName()).thenReturn("보호자");
        when(parent.isLocal()).thenReturn(false);
        when(parent.getProvider()).thenReturn(AuthProvider.GOOGLE);
        when(jwtProvider.issue(parentId)).thenReturn("service-jwt");
        when(refreshTokenService.issue(parent)).thenReturn("refresh-token");
        when(jwtProvider.getExpiresInSeconds()).thenReturn(3600L);

        SocialAuthResponse response = authService.loginWithGoogle(request, "127.0.0.1");

        assertThat(response.tokens().accessToken()).isEqualTo("service-jwt");
        assertThat(response.parent().provider()).isEqualTo(AuthProvider.GOOGLE);
        assertThat(response.isNewUser()).isFalse();
        verify(parentRepository).findByProviderAndProviderId(AuthProvider.GOOGLE, "google-sub");
    }
}
