package com.mugunghwa.goodquestion.user.auth;

import com.mugunghwa.goodquestion.global.error.BusinessException;
import com.mugunghwa.goodquestion.support.IntegrationTest;
import com.mugunghwa.goodquestion.user.auth.dto.AuthResponse;
import com.mugunghwa.goodquestion.user.auth.dto.LoginRequest;
import com.mugunghwa.goodquestion.user.auth.dto.SignUpRequest;
import com.mugunghwa.goodquestion.user.parent.Parent;
import com.mugunghwa.goodquestion.user.parent.ParentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 로그인 실패 잠금.
 *
 * <p>@Transactional을 쓰지 않는다 — 실패 기록은 REQUIRES_NEW로 별도 커밋되므로
 * 테스트가 트랜잭션 안에 있으면 그 커밋을 보지 못한다. 트랜잭션 경계 자체가 검증 대상이라
 * 실제로 커밋되는지를 봐야 한다. 대신 뒷정리를 직접 한다.
 */
@IntegrationTest
class LoginLockTest {

    private static final String PASSWORD = "password123";
    private static final String IP = "127.0.0.1";

    @Autowired
    private AuthService authService;

    @Autowired
    private ParentRepository parentRepository;

    @AfterEach
    void 뒷정리() {
        parentRepository.findByEmail(email()).ifPresent(parentRepository::delete);
    }

    /** 테스트마다 다른 계정을 쓰면 뒷정리가 복잡해져 하나로 고정한다. */
    private String email() {
        return "lock-test@test.com";
    }

    private void 가입한다() {
        authService.signUp(new SignUpRequest(email(), PASSWORD, "김보호"), IP);
    }

    private void 비밀번호를_틀린다() {
        assertThatThrownBy(() -> authService.login(new LoginRequest(email(), "wrong-password"), IP))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void 다섯_번_틀리면_비밀번호가_맞아도_막힌다() {
        가입한다();
        for (int i = 0; i < 5; i++) {
            비밀번호를_틀린다();
        }

        assertThatThrownBy(() -> authService.login(new LoginRequest(email(), PASSWORD), IP))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("잠");
    }

    @Test
    void 네_번까지는_잠기지_않는다() {
        가입한다();
        for (int i = 0; i < 4; i++) {
            비밀번호를_틀린다();
        }

        AuthResponse response = authService.login(new LoginRequest(email(), PASSWORD), IP);

        assertThat(response.tokens().accessToken()).isNotBlank();
    }

    @Test
    void 로그인에_성공하면_실패_횟수가_초기화된다() {
        가입한다();
        비밀번호를_틀린다();

        authService.login(new LoginRequest(email(), PASSWORD), IP);

        Parent parent = parentRepository.findByEmail(email()).orElseThrow();
        assertThat(parent.getFailedLoginAttempts()).isZero();
        assertThat(parent.isLocked()).isFalse();
    }

    @Test
    void 로그인_IP가_기록된다() {
        가입한다();

        authService.login(new LoginRequest(email(), PASSWORD), "203.0.113.7");

        assertThat(parentRepository.findByEmail(email()).orElseThrow().getLastLoginIp())
                .isEqualTo("203.0.113.7");
    }
}