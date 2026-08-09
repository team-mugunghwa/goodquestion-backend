package com.mugunghwa.goodquestion.global.security;

import tools.jackson.databind.ObjectMapper;
import com.mugunghwa.goodquestion.global.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 인증은 됐지만 권한이 없는 요청에 403을 돌려준다.
 *
 * <p>본문 형식을 도메인 오류({@code BusinessException(FORBIDDEN)})와 맞춰
 * 프론트가 한 가지 파싱 경로만 쓰게 한다.
 */
@Component
@RequiredArgsConstructor
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        RestAuthenticationEntryPoint.write(response, objectMapper, ErrorCode.FORBIDDEN);
    }
}
