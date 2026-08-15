package com.mugunghwa.goodquestion.user.parent;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.UUID;

/**
 * 인증된 요청이 지나갈 때 방문을 기록한다.
 *
 * <p>{@code global.config.WebConfig}가 아니라 여기 두는 이유가 있다. global은 도메인을
 * 모르는 공유 커널이라 {@link VisitRecorder}를 참조할 수 없고, ArchUnit 규칙이
 * 그 방향의 import를 막는다. 방문 기록은 보호자에 대한 것이므로 이 패키지의 일이다.
 *
 * <p>{@code WebMvcConfigurer}는 여러 개를 둬도 스프링이 모두 적용한다.
 *
 * <p>로그인 시점이 아니라 요청 시점에 기록하는 이유: 액세스 토큰이 30분, 리프레시가
 * 14일이라 한 번 로그인하고 2주를 쓰는 사용자가 있다. 로그인만 세면 그 사람은 방문자
 * 통계에서 사라진다.
 */
@Configuration
@RequiredArgsConstructor
public class VisitTrackingConfig implements WebMvcConfigurer {

    private final VisitRecorder visitRecorder;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new VisitTrackingInterceptor(visitRecorder))
                .addPathPatterns("/api/**")
                // 인증 전 경로와 헬스체크는 셀 대상이 아니다. 어차피 보호자를 알 수 없다.
                .excludePathPatterns("/api/auth/**");
    }

    /** 컨트롤러가 실행되기 전에 한 번 부른다. 응답 결과와 무관하게 "들어왔다"는 사실만 본다. */
    private record VisitTrackingInterceptor(VisitRecorder visitRecorder) implements HandlerInterceptor {

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                                 Object handler) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof UUID parentId) {
                visitRecorder.record(parentId);
            }
            return true;
        }
    }
}
