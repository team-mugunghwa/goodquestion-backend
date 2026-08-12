package com.mugunghwa.goodquestion.user.auth;

import com.mugunghwa.goodquestion.global.error.BusinessException;
import com.mugunghwa.goodquestion.support.IntegrationTest;
import com.mugunghwa.goodquestion.user.auth.dto.AuthResponse;
import com.mugunghwa.goodquestion.user.auth.dto.LoginRequest;
import com.mugunghwa.goodquestion.user.auth.dto.SignUpRequest;
import com.mugunghwa.goodquestion.user.auth.dto.TokenResponse;
import com.mugunghwa.goodquestion.user.parent.Parent;
import com.mugunghwa.goodquestion.user.parent.ParentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 리프레시 토큰 회전·무효화(계정-05)와 로그인 잠금. */
@IntegrationTest
@Transactional
class AuthServiceTest {

    private static final String PASSWORD = "password123";
    private static final String IP = "127.0.0.1";

    @Autowired
    private AuthService authService;

    @Autowired
    private ParentRepository parentRepository;

    private AuthResponse 가입한다(String email) {
        return authService.signUp(new SignUpRequest(email, PASSWORD, "김보호"), IP);
    }

    private void 비밀번호를_틀린다(String email) {
        assertThatThrownBy(() -> authService.login(new LoginRequest(email, "wrong-password"), IP))
                .isInstanceOf(BusinessException.class);
    }

    // ----- 리프레시 토큰 -----

    @Test
    void 가입하면_액세스와_리프레시가_함께_발급된다() {
        AuthResponse response = 가입한다("a@test.com");

        assertThat(response.tokens().accessToken()).isNotBlank();
        assertThat(response.tokens().refreshToken()).isNotBlank();
        assertThat(response.tokens().accessTokenExpiresIn()).isPositive();
    }

    @Test
    void 재발급하면_리프레시도_새_값으로_바뀐다() {
        String issued = 가입한다("b@test.com").tokens().refreshToken();

        TokenResponse rotated = authService.refresh(issued);

        assertThat(rotated.refreshToken()).isNotBlank();
        assertThat(rotated.refreshToken()).isNotEqualTo(issued);
    }

    /** 회전의 핵심 — 쓴 토큰이 계속 통하면 탈취본과 정상 사용자를 구분할 수 없다. */
    @Test
    void 한_번_쓴_리프레시는_다시_쓸_수_없다() {
        String issued = 가입한다("c@test.com").tokens().refreshToken();
        authService.refresh(issued);

        assertThatThrownBy(() -> authService.refresh(issued))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void 로그아웃하면_해당_리프레시가_끊긴다() {
        String issued = 가입한다("d@test.com").tokens().refreshToken();

        authService.logout(issued);

        assertThatThrownBy(() -> authService.refresh(issued))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void 없는_리프레시로는_재발급할_수_없다() {
        assertThatThrownBy(() -> authService.refresh("존재하지-않는-토큰"))
                .isInstanceOf(BusinessException.class);
    }

    /** 이미 끊긴 토큰으로 다시 로그아웃해도 오류를 내지 않는다 — 결과가 이미 목적대로다. */
    @Test
    void 로그아웃은_여러_번_불러도_안전하다() {
        String issued = 가입한다("e@test.com").tokens().refreshToken();

        authService.logout(issued);
        authService.logout(issued);
    }

    // ----- 로그인 잠금 -----

    @Test
    void 다섯_번_틀리면_비밀번호가_맞아도_막힌다() {
        가입한다("lock@test.com");
        for (int i = 0; i < 5; i++) {
            비밀번호를_틀린다("lock@test.com");
        }

        assertThatThrownBy(() -> authService.login(new LoginRequest("lock@test.com", PASSWORD), IP))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("잠");
    }

    @Test
    void 네_번까지는_잠기지_않는다() {
        가입한다("ok@test.com");
        for (int i = 0; i < 4; i++) {
            비밀번호를_틀린다("ok@test.com");
        }

        AuthResponse response = authService.login(new LoginRequest("ok@test.com", PASSWORD), IP);

        assertThat(response.tokens().accessToken()).isNotBlank();
    }

    @Test
    void 로그인에_성공하면_실패_횟수가_초기화된다() {
        가입한다("reset@test.com");
        비밀번호를_틀린다("reset@test.com");

        authService.login(new LoginRequest("reset@test.com", PASSWORD), IP);

        Parent parent = parentRepository.findByEmail("reset@test.com").orElseThrow();
        assertThat(parent.getFailedLoginAttempts()).isZero();
        assertThat(parent.isLocked()).isFalse();
    }

    @Test
    void 로그인_IP가_기록된다() {
        가입한다("ip@test.com");

        authService.login(new LoginRequest("ip@test.com", PASSWORD), "203.0.113.7");

        assertThat(parentRepository.findByEmail("ip@test.com").orElseThrow().getLastLoginIp())
                .isEqualTo("203.0.113.7");
    }
}