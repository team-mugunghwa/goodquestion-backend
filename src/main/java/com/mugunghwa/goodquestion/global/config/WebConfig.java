package com.mugunghwa.goodquestion.global.config;

import com.mugunghwa.goodquestion.global.security.CurrentParentIdArgumentResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
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

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // TODO: 운영 도메인으로 제한
        registry.addMapping("/**").allowedOriginPatterns("*").allowedMethods("*");
    }
}
