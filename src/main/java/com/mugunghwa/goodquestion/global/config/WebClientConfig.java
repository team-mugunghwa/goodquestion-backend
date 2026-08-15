package com.mugunghwa.goodquestion.global.config;

import io.netty.channel.ChannelOption;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;

import java.net.ConnectException;
import java.time.Duration;

/**
 * STT/LLM/TTS와 카카오 인가 코드 교환이 공유하는 외부 API 호출 공용 WebClient.
 *
 * <p>타임아웃이 없으면 벤더가 응답하지 않는 동안 호출 스레드가 무한정 매달린다. 발화 한 건에
 * LLM을 두 번 부르는 구조라 그 사이 DB 커넥션까지 함께 묶이고, 커넥션 풀은 앱 전체가
 * 공유하므로 대화와 무관한 요청까지 막힌다. 상한을 두는 것이 이 설정의 목적이다.
 *
 * <p>주의: responseTimeout은 "응답 전체의 마감"이 아니라 "읽기와 읽기 사이의 최대 간격"이다.
 * 한 번에 내려오는 JSON 응답에서는 사실상 마감처럼 동작하지만, 스트리밍으로 바꾸면 조각이
 * 계속 도착하는 한 시간 제한이 걸리지 않는다. 그때는 호출부에서 전체 시간을 따로 걸어야 한다.
 * (ReadTimeoutHandler를 따로 붙이지 않는 이유이기도 하다 - responseTimeout과 역할이 겹치고,
 * 커넥션 단위로 붙이면 풀의 유휴 커넥션에도 타이머가 걸려 오탐을 만든다.)
 *
 * <p>로깅은 메서드/URL/상태 코드까지만 debug로 남긴다. 요청/응답 본문은 남기지 않는다 -
 * 이 클라이언트가 실어 나르는 것은 아이 발화 원문과 벤더 API 키다.
 */
@Slf4j
@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient(
            @Value("${external.http.connect-timeout-ms:3000}") int connectTimeoutMs,
            @Value("${external.http.response-timeout-ms:10000}") long responseTimeoutMs) {

        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMs)
                .responseTimeout(Duration.ofMillis(responseTimeoutMs));

        return WebClient.builder()
                // TTS 응답은 MP3 본문 전체를 메모리로 받는다. Spring 기본 한도 256KB로는
                // 긴 내레이션 한 문장도 넘겨 DataBufferLimitException이 난다.
                .codecs(c -> c.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .filter(logRequest())
                .filter(logResponse())
                .filter(retryOnConnectFailure())
                .build();
    }

    /**
     * 연결 자체가 실패했을 때만 한 번 다시 시도한다.
     *
     * <p>응답 타임아웃은 재시도하지 않는다. 서버가 요청을 이미 받아 처리 중일 수 있어 같은
     * 발화가 두 번 처리될 수 있고, LLM은 호출당 과금이며, 이 빈을 공유하는 카카오 인가 코드
     * 교환은 코드가 1회용이라 재시도하면 "이미 쓴 코드" 거절이 원래 원인을 가린다.
     *
     * <p>연결 실패는 그 우려들에 해당하지 않는다. TCP 연결이 수립되지 않아 HTTP 요청 자체가
     * 전송되지 않았으므로, 발화도 인가 코드도 소비되지 않은 상태다. 배포 중 순간 단절이
     * 여기 해당한다.
     */
    private ExchangeFilterFunction retryOnConnectFailure() {
        return (request, next) -> next.exchange(request)
                .retryWhen(Retry.max(1)
                        .filter(WebClientConfig::isConnectFailure)
                        // 재시도까지 실패하면 원래 예외를 그대로 올린다. 감싸면 호출부가 원인을 잃는다.
                        .onRetryExhaustedThrow((spec, signal) -> signal.failure()));
    }

    /** 연결 타임아웃(io.netty.channel.ConnectTimeoutException)도 ConnectException의 하위 타입이다. */
    private static boolean isConnectFailure(Throwable error) {
        return error instanceof WebClientRequestException
                && error.getCause() instanceof ConnectException;
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
