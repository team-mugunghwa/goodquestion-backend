package com.mugunghwa.goodquestion.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * 브라우저에서 오는 교차 출처 요청 설정.
 *
 * <p>이 빈은 SecurityConfig 의 {@code .cors()} 가 찾아서 쓴다. WebMvcConfigurer 의
 * addCorsMappings 로 두면 안 된다 - 시큐리티 필터 체인이 DispatcherServlet 보다
 * 먼저 돌기 때문에, Authorization 헤더가 없는 preflight(OPTIONS) 요청이 MVC 까지
 * 가지도 못하고 401 로 잘린다. 실제로 그 상태였고, curl 은 되는데 브라우저에서만
 * 전부 실패하는 형태로 나타났다.
 *
 * <p>동작 확인은 CorsConfigTest 가 한다. 설정이 깨져도 서버 로그는 멀쩡해 보이므로
 * 손으로 확인하려 들지 말고 그 테스트를 돌린다.
 */
@Configuration
public class CorsConfig {

    private final List<String> allowedOriginPatterns;

    public CorsConfig(
            @Value("${cors.allowed-origin-patterns}") List<String> allowedOriginPatterns) {
        this.allowedOriginPatterns = allowedOriginPatterns;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // setAllowedOrigins 가 아니라 패턴을 쓰는 이유: Vercel 프리뷰 배포는
        // 커밋마다 주소가 바뀌어서 정확한 값을 미리 적을 수 없다.
        config.setAllowedOriginPatterns(allowedOriginPatterns);

        // 컨트롤러가 실제로 쓰는 메서드만 연다. PUT 은 쓰는 곳이 없다.
        config.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Idempotency-Key"));

        // 쿠키를 쓰지 않는다. 토큰은 Authorization 헤더로 보내고 프론트의
        // 오리진별 저장소에 둔다. 남의 오리진이 이 토큰을 읽을 수 없으므로
        // 오리진 패턴이 다소 넓어도 인증된 요청이 새지 않는다.
        config.setAllowCredentials(false);

        // preflight 결과를 1시간 캐시한다. 화면 전환마다 OPTIONS 가 한 번씩
        // 더 나가는 것을 줄인다.
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
