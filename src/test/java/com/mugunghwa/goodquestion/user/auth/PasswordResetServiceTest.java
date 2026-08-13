package com.mugunghwa.goodquestion.user.auth;

import com.mugunghwa.goodquestion.global.error.BusinessException;
import com.mugunghwa.goodquestion.support.IntegrationTest;
import com.mugunghwa.goodquestion.user.auth.dto.SignUpRequest;
import com.mugunghwa.goodquestion.user.parent.Parent;
import com.mugunghwa.goodquestion.user.parent.ParentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 비밀번호 재설정 (계정-06).
 *
 * <p>실제 메일이 나가면 안 되므로 JavaMailSender를 목으로 대체한다.
 */
@IntegrationTest
@Transactional
class PasswordResetServiceTest {

    private static final String PASSWORD = "password123";
    private static final String IP = "127.0.0.1";

    @Autowired
    private AuthService authService;

    @Autowired
    private PasswordResetService passwordResetService;

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @Autowired
    private ParentRepository parentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private JavaMailSender mailSender;

    private void 가입한다(String email) {
        authService.signUp(new SignUpRequest(email, PASSWORD, "김보호"), IP);
    }

    private String 발급된_토큰을_가져온다(String email) {
        Parent parent = parentRepository.findByEmail(email).orElseThrow();
        // 원문은 저장하지 않으므로, 실제 흐름처럼 새 토큰을 발급해 원문을 직접 반환하는 대신
        // request()가 저장한 해시와 매칭되는 원문을 얻으려면 request() 호출 지점에서 가로채야
        // 하지만 이 테스트는 서비스 바깥에서 원문을 알 수 없다는 게 핵심 보안 속성이라,
        // request()를 호출한 뒤 메일 발송 인자에서 링크를 읽어 토큰을 뽑는다.
        var captor = org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        String text = captor.getValue().getText();
        String marker = "token=";
        int start = text.indexOf(marker) + marker.length();
        int end = text.indexOf('\n', start);
        return text.substring(start, end < 0 ? text.length() : end).trim();
    }

    @Test
    void 존재하는_계정으로_요청하면_토큰이_저장되고_메일이_발송된다() {
        가입한다("reset-a@test.com");

        passwordResetService.request("reset-a@test.com");

        assertThat(tokenRepository.count()).isEqualTo(1);
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void 존재하지_않는_이메일이면_조용히_무시한다() {
        passwordResetService.request("no-such-account@test.com");

        assertThat(tokenRepository.count()).isZero();
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void 올바른_토큰으로_확인하면_비밀번호가_바뀌고_토큰이_재사용되지_않는다() {
        가입한다("reset-b@test.com");
        passwordResetService.request("reset-b@test.com");
        String token = 발급된_토큰을_가져온다("reset-b@test.com");

        passwordResetService.confirm(token, "new-password123");

        Parent parent = parentRepository.findByEmail("reset-b@test.com").orElseThrow();
        assertThat(passwordEncoder.matches("new-password123", parent.getPasswordHash())).isTrue();

        assertThatThrownBy(() -> passwordResetService.confirm(token, "another-password123"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void 잘못된_토큰이면_예외() {
        assertThatThrownBy(() -> passwordResetService.confirm("존재하지-않는-토큰", "new-password123"))
                .isInstanceOf(BusinessException.class);
    }
}
