package com.mugunghwa.goodquestion.global.config;

import io.netty.handler.timeout.ReadTimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 외부 API 호출 타임아웃 검증.
 *
 * <p>응답하지 않는 서버를 실제로 띄워서 호출이 상한에서 끊기는지 본다. 설정값만 확인하면
 * "설정은 했는데 적용은 안 된" 상태를 잡지 못한다 - WebClient는 커넥터를 갈아 끼우지 않으면
 * 타임아웃을 무시하는데 그 실수가 눈에 띄지 않는다.
 *
 * <p>테스트에서는 300밀리초로 줄여 쓴다. 운영 기본값(10초)을 그대로 기다릴 이유가 없다.
 */
class WebClientConfigTest {

    private static final Duration RESPONSE_TIMEOUT = Duration.ofMillis(300);

    private DisposableServer server;

    @AfterEach
    void 서버를_내린다() {
        if (server != null) {
            server.disposeNow();
        }
    }

    @Test
    void 응답이_늦으면_상한에서_끊는다() {
        server = serverDelayingBy(Duration.ofSeconds(5));

        long startedAt = System.nanoTime();
        assertThatThrownBy(() -> get(webClient(), "/slow"))
                .isInstanceOf(WebClientRequestException.class)
                .hasRootCauseInstanceOf(ReadTimeoutException.class);
        Duration elapsed = Duration.ofNanos(System.nanoTime() - startedAt);

        // 5초를 기다리지 않고 상한 근처에서 끊겼는지 본다. 여유는 서버 기동과 스케줄링 몫이다.
        assertThat(elapsed).isLessThan(Duration.ofSeconds(3));
    }

    @Test
    void 제때_응답하면_그대로_받는다() {
        server = serverDelayingBy(Duration.ZERO);

        assertThat(get(webClient(), "/slow")).isEqualTo("ok");
    }

    private WebClient webClient() {
        return new WebClientConfig().webClient(3000, RESPONSE_TIMEOUT.toMillis());
    }

    private String get(WebClient webClient, String path) {
        return webClient.get()
                .uri("http://localhost:" + server.port() + path)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    /** 요청을 받고 지정한 시간만큼 끌다가 응답하는 서버. */
    private DisposableServer serverDelayingBy(Duration delay) {
        return HttpServer.create()
                .port(0)
                .route(routes -> routes.get("/slow", (request, response) ->
                        response.sendString(Mono.just("ok").delayElement(delay))))
                .bindNow();
    }
}
