package com.mugunghwa.goodquestion.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/** STT/LLM/TTS 외부 API 호출 공용 WebClient */
@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient() {
        // TODO: 타임아웃·재시도·로깅 설정
        return WebClient.builder().build();
    }
}
