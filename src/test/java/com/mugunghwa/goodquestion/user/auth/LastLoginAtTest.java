package com.mugunghwa.goodquestion.user.auth;

import com.mugunghwa.goodquestion.support.IntegrationTest;
import com.mugunghwa.goodquestion.user.auth.dto.AuthResponse;
import com.mugunghwa.goodquestion.user.auth.dto.LoginRequest;
import com.mugunghwa.goodquestion.user.auth.dto.SignUpRequest;
import com.mugunghwa.goodquestion.user.parent.Parent;
import com.mugunghwa.goodquestion.user.parent.ParentRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** 마지막 접속 시각 기록 - 로그인과 리프레시 재발급이 모두 갱신한다. */
@IntegrationTest
@Transactional
class LastLoginAtTest {

    private static final String PASSWORD = "password123";
    private static final String IP = "127.0.0.1";

    @Autowired
    private AuthService authService;

    @Autowired
    private ParentRepository parentRepository;

    @Autowired
    private EntityManager entityManager;

    private AuthResponse 가입한다(String email) {
        return authService.signUp(new SignUpRequest(email, PASSWORD, "김보호"), IP);
    }

    /** 영속성 컨텍스트에 남은 인스턴스가 아니라 DB에 실제로 쓰인 값을 본다. */
    private Parent DB에서_다시_읽는다(UUID parentId) {
        entityManager.flush();
        entityManager.clear();
        return parentRepository.findById(parentId).orElseThrow();
    }

    @Test
    void 가입하면_접속_시각이_남는다() {
        // timestamptz는 마이크로초까지만 저장한다. 자바 쪽 나노초를 그대로 비교하면
        // 잘려 나간 만큼 앞선 값으로 읽혀 간헐적으로 깨진다.
        OffsetDateTime 가입_직전 = OffsetDateTime.now().truncatedTo(ChronoUnit.MICROS);

        UUID parentId = 가입한다("last-login-a@test.com").parent().id();

        assertThat(DB에서_다시_읽는다(parentId).getLastLoginAt())
                .isNotNull()
                .isAfterOrEqualTo(가입_직전);
    }

    @Test
    void 로그인하면_접속_시각이_남는다() {
        UUID parentId = 가입한다("last-login-b@test.com").parent().id();
        OffsetDateTime 가입_시각 = DB에서_다시_읽는다(parentId).getLastLoginAt();

        authService.login(new LoginRequest("last-login-b@test.com", PASSWORD), IP);

        assertThat(DB에서_다시_읽는다(parentId).getLastLoginAt()).isAfterOrEqualTo(가입_시각);
    }

    /**
     * 재발급도 접속으로 센다. 이걸 빠뜨리면 한 번 로그인하고 2주 동안 앱을 쓰는 사용자가
     * 마지막 접속 시각에서는 2주째 그대로 멈춰 있다.
     */
    @Test
    void 재발급해도_접속_시각이_갱신된다() {
        AuthResponse 가입 = 가입한다("last-login-c@test.com");
        UUID parentId = 가입.parent().id();
        OffsetDateTime 가입_시각 = DB에서_다시_읽는다(parentId).getLastLoginAt();

        authService.refresh(가입.tokens().refreshToken());

        assertThat(DB에서_다시_읽는다(parentId).getLastLoginAt()).isAfterOrEqualTo(가입_시각);
    }

    /** 재발급은 자격 증명을 다시 확인한 것이 아니므로 로그인 위치는 그대로 둔다. */
    @Test
    void 재발급은_로그인_IP를_바꾸지_않는다() {
        AuthResponse 가입 = 가입한다("last-login-d@test.com");
        UUID parentId = 가입.parent().id();

        authService.refresh(가입.tokens().refreshToken());

        assertThat(DB에서_다시_읽는다(parentId).getLastLoginIp()).isEqualTo(IP);
    }
}
