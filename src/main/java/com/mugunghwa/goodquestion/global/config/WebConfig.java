package com.mugunghwa.goodquestion.global.config;

import com.mugunghwa.goodquestion.global.security.CurrentParentIdArgumentResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final CurrentParentIdArgumentResolver currentParentIdArgumentResolver;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentParentIdArgumentResolver);
    }

    // CORS 는 여기가 아니라 CorsConfig 에 있다. addCorsMappings 는 MVC 계층이라
    // 시큐리티 필터 체인에서 잘리는 preflight 에 손을 못 댄다. 설정이 두 군데로
    // 갈리지 않게 이쪽은 비워 둔다.
}
