package com.mugunghwa.goodquestion.global.config;

import com.mugunghwa.goodquestion.global.security.JwtAuthFilter;
import com.mugunghwa.goodquestion.global.security.RestAccessDeniedHandler;
import com.mugunghwa.goodquestion.global.security.RestAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                // CorsConfig 의 빈을 체인에 건다. 시큐리티 7 은 UrlBasedCorsConfigurationSource
                // 타입 빈이 있으면 이 줄이 없어도 자동으로 걸어주지만
                // (HttpSecurityConfiguration.applyCorsIfAvailable), 빈 타입이 그 클래스가
                // 아니게 되는 순간 조용히 꺼진다. 명시해서 그 규칙에 기대지 않는다.
                //
                // CorsFilter 가 preflight 를 인가 단계 전에 처리하고 체인을 끊으므로
                // OPTIONS 를 permitAll 로 열어줄 필요는 없다.
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health/**").permitAll()
                        // 회원가입·로그인은 토큰 발급 이전 단계라 인증 없이 허용
                        .requestMatchers("/api/auth/**").permitAll()
                        // 이야기 정적 에셋(장면/미션 이미지). 개인정보가 없는 공개 콘텐츠라
                        // 인증 없이 연다 - 이미지 태그가 Authorization 헤더를 못 싣는 환경도 있다.
                        .requestMatchers("/stories/**").permitAll()
                        // 사전 렌더 음성(장면 낭독·고정 대사·선택지)과 행성 아이템 모델도 같다.
                        // <audio src>와 GLTF 로더는 이미지 태그와 마찬가지로 헤더를 못 싣고,
                        // 행성은 별도 웹앱이라 토큰을 들고 있지도 않다. 둘 다 공개 콘텐츠다.
                        .requestMatchers("/tts/**", "/items/**").permitAll()
                        .anyRequest().authenticated())
                // 기본값은 401·403 모두 403 + 빈 본문이라 만료와 권한 없음을 구분할 수 없다.
                // 리프레시 토큰이 없어 만료 복구가 재로그인뿐이므로 둘을 반드시 갈라야 한다.
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(authenticationEntryPoint)   // 미인증 → 401
                        .accessDeniedHandler(accessDeniedHandler))            // 권한 없음 → 403
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
