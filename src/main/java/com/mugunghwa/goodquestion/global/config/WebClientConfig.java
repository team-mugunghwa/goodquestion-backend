package com.mugunghwa.goodquestion.global.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

/**
 * STT/LLM/TTS 외부 API 호출 공용 WebClient.
 *
 * <p>연결 3초, 응답 10초로 막는다. 벤더가 오래 매달리면 발화 1건에 LLM을 2번 부르는 구조상
 * 요청 스레드가 금방 마른다 - 언젠가 실패할 호출이라면 빨리 실패해야 한다.
 *
 * <p>재시도는 여기 넣지 않는다. 이 빈을 공유하는 카카오 인가 코드 교환({@code DefaultKakaoClient})은
 * 코드가 1회용이라, 실패 후 재시도하면 카카오가 "이미 쓴 코드"로 거절해 원래 원인을 가린다.
 * 재시도가 안전한 호출(예: STT/TTS 벤더 연동 시)은 호출부에서 개별적으로 붙인다.
 */
@Slf4j
@Configuration
public class WebClientConfig {

    private static final int CONNECT_TIMEOUT_MS = 3000;
    private static final int RESPONSE_TIMEOUT_SEC = 10;

    @Bean
    public WebClient webClient() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT_MS)
                .responseTimeout(Duration.ofSeconds(RESPONSE_TIMEOUT_SEC))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(RESPONSE_TIMEOUT_SEC))
                        .addHandlerLast(new WriteTimeoutHandler(RESPONSE_TIMEOUT_SEC)));

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .filter(logRequest())
                .filter(logResponse())
                .build();
    }

    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(request -> {
            log.debug("외부 API 요청: {} {}", request.method(), request.url());
            return Mono.just(request);
        });
    }

    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(response -> {
            log.debug("외부 API 응답: {}", response.statusCode());
            return Mono.just(response);
        });
    }
}
