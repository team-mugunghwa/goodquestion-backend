package com.mugunghwa.goodquestion.user.auth;

import com.mugunghwa.goodquestion.global.error.BusinessException;
import com.mugunghwa.goodquestion.support.IntegrationTest;
import com.mugunghwa.goodquestion.user.auth.dto.SignUpRequest;
import com.mugunghwa.goodquestion.user.parent.Parent;
import com.mugunghwa.goodquestion.user.parent.ParentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 비밀번호 재설정 (계정-06).
 *
 * <p>실제 메일이 나가면 안 되므로 WebClient(Resend HTTP API 호출)를 목으로 대체한다.
 * 체인의 각 단계를 명시적으로 스텁한다. 처음에는 딥 스텁(RETURNS_DEEP_STUBS)을
 * 썼는데, WebClient의 체이닝 반환형이 자기 참조 제네릭(S extends RequestHeadersSpec&lt;S&gt;)
 * 이라 딥 스텁이 {@code header(String, String...)}에서 null을 돌려줬다 - 서비스가
 * NPE를 먹고 EMAIL_DELIVERY_FAILED로 변환해 테스트가 항상 실패했다.
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
    private WebClient webClient;

    private WebClient.RequestBodyUriSpec uriSpec;
    private WebClient.RequestBodySpec bodySpec;
    private WebClient.RequestHeadersSpec<?> headersSpec;
    private WebClient.ResponseSpec responseSpec;

    @BeforeEach
    void 메일_발송_체인을_스텁한다() {
        uriSpec = mock(WebClient.RequestBodyUriSpec.class);
        bodySpec = mock(WebClient.RequestBodySpec.class);
        headersSpec = mock(WebClient.RequestHeadersSpec.class);
        responseSpec = mock(WebClient.ResponseSpec.class);
        when(webClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(bodySpec);
        when(bodySpec.header(anyString(), anyString())).thenReturn(bodySpec);
        when(bodySpec.contentType(any())).thenReturn(bodySpec);
        // bodyValue의 반환형이 와일드카드(RequestHeadersSpec<?>)라 when-thenReturn은
        // 제네릭 캡처에 걸린다. doReturn은 타입 검사를 우회한다.
        doReturn(headersSpec).when(bodySpec).bodyValue(any());
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity())
                .thenReturn(Mono.just(ResponseEntity.ok().build()));
    }

    private void 가입한다(String email) {
        authService.signUp(new SignUpRequest(email, PASSWORD, "김보호"), IP);
    }

    private String 발급된_토큰을_가져온다(String email) {
        Parent parent = parentRepository.findByEmail(email).orElseThrow();
        // 원문은 저장하지 않으므로, 실제 흐름처럼 새 토큰을 발급해 원문을 직접 반환하는 대신
        // request()가 저장한 해시와 매칭되는 원문을 얻으려면 request() 호출 지점에서 가로채야
        // 하지만 이 테스트는 서비스 바깥에서 원문을 알 수 없다는 게 핵심 보안 속성이라,
        // request()를 호출한 뒤 메일 발송 인자에서 링크를 읽어 토큰을 뽑는다.
        var captor = org.mockito.ArgumentCaptor.forClass(Map.class);
        verify(bodySpec)
                .bodyValue(captor.capture());
        String text = (String) captor.getValue().get("text");
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
        verify(bodySpec).bodyValue(any());
    }

    @Test
    void 존재하지_않는_이메일이면_조용히_무시한다() {
        passwordResetService.request("no-such-account@test.com");

        assertThat(tokenRepository.count()).isZero();
        verify(webClient, never()).post();
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
