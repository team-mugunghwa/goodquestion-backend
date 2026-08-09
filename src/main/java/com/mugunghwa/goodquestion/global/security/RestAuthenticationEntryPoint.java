package com.mugunghwa.goodquestion.global.security;

import tools.jackson.databind.ObjectMapper;
import com.mugunghwa.goodquestion.global.error.ErrorCode;
import com.mugunghwa.goodquestion.global.error.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 인증되지 않은 요청에 401을 돌려준다.
 *
 * <p>기본 동작은 403 + 빈 본문이라 <b>토큰 만료와 권한 없음을 구분할 수 없다.</b>
 * 리프레시 토큰이 없는 지금은 만료 시 유일한 복구 경로가 재로그인이므로,
 * 클라이언트가 "로그인 화면으로 보낼 상황"을 상태 코드로 판별할 수 있어야 한다.
 *
 * <p>만료·위조·헤더 없음을 모두 401 {@code UNAUTHORIZED}로 합친다 —
 * 어느 쪽이든 클라이언트가 할 일은 재로그인 하나뿐이고,
 * 사유를 자세히 알리면 토큰 탐색에 단서를 준다.
 */
@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        write(response, objectMapper, ErrorCode.UNAUTHORIZED);
    }

    /** 401·403 응답을 도메인 오류와 같은 {@code {code, message}} 형태로 맞춘다. */
    static void write(HttpServletResponse response, ObjectMapper objectMapper, ErrorCode errorCode)
            throws IOException {
        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(),
                ErrorResponse.of(errorCode, errorCode.getDefaultMessage()));
    }
}
