package com.mugunghwa.goodquestion.ai.tts;

import com.mugunghwa.goodquestion.ai.tts.GeminiTtsClient.GeminiVoiceProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.client.reactive.MockClientHttpRequest;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * <b>벤더와 맺은 요청 계약을 재는 유일한 자리다.</b>
 *
 * <p>{@link TtsVendorRoutingTest} 는 어느 벤더가 뽑히는지만 보고 {@link GeminiTtsClient} 안쪽을
 * 통째로 건너뛴다. 그래서 URL 조립을 틀리거나 본문에서 {@code role} 을 빼도 그쪽은 전부
 * 초록이고, 운영에서만 404/400 이 난다.
 *
 * <p>여기서는 {@code ExchangeFunction} 을 끼워 <b>실제로 나간 요청</b>을 뜯어본다.
 * 네트워크는 타지 않고, 새 의존성도 쓰지 않는다.
 */
class GeminiTtsRequestTest {

    /** 24kHz 16bit mono PCM 한 조각. 내용은 상관없고 디코드만 되면 된다. */
    private static final String PCM_BASE64 = Base64.getEncoder().encodeToString(new byte[64]);

    private static final String AUDIO_JSON = """
            {"candidates":[{"content":{"parts":[
              {"inlineData":{"mimeType":"audio/L16;codec=pcm;rate=24000","data":"%s"}}
            ]}}]}
            """.formatted(PCM_BASE64);

    /**
     * 나간 요청을 담아 두는 자리.
     *
     * <p><b>본문은 여기서 펴지 않는다.</b> 재시도가 걸리면 재구독이 parallel 스케줄러에서
     * 일어나는데, 그 안에서 {@code block()} 을 부르면 리액터가 막는다
     * ("blocking is not supported in thread parallel-1"). 요청만 담고 본문은
     * 테스트 본체에서 {@link #bodyOf} 로 편다.
     */
    private final List<ClientRequest> requests = new ArrayList<>();

    /**
     * 주어진 상태코드를 순서대로 돌려주는 가짜 서버. 마지막 것을 다 쓰면 그대로 반복한다.
     * 재시도를 재려면 "429 두 번 뒤 200" 같은 대본이 필요하다.
     */
    private GeminiTtsClient client(String baseUrl, String model, HttpStatus... script) {
        AtomicInteger turn = new AtomicInteger();
        WebClient webClient = WebClient.builder()
                .exchangeFunction(request -> {
                    requests.add(request);
                    HttpStatus status = script.length == 0
                            ? HttpStatus.OK
                            : script[Math.min(turn.getAndIncrement(), script.length - 1)];
                    if (status != HttpStatus.OK) {
                        return Mono.just(ClientResponse.create(status).body("{}").build());
                    }
                    return Mono.just(ClientResponse.create(HttpStatus.OK)
                            .header("Content-Type", "application/json")
                            .body(AUDIO_JSON)
                            .build());
                })
                .build();

        GeminiVoiceProperties voices = new GeminiVoiceProperties(
                "Kore", "따뜻하게 말해줘:",
                Map.of("방귀쟁이 며느리", "Leda"),
                Map.of());
        return new GeminiTtsClient(webClient, baseUrl, "test-key", model, 5_000, voices);
    }

    /**
     * WebClient 의 요청 본문은 스트림이라 그대로는 못 읽는다. 가짜 요청에 흘려 넣어 편다.
     */
    private static String bodyOf(ClientRequest request) {
        MockClientHttpRequest sink = new MockClientHttpRequest(request.method(), request.url());
        request.body().insert(sink, new BodyInserter.Context() {
            @Override
            public List<HttpMessageWriter<?>> messageWriters() {
                return ExchangeStrategies.withDefaults().messageWriters();
            }

            @Override
            public Optional<ServerHttpRequest> serverRequest() {
                return Optional.empty();
            }

            @Override
            public Map<String, Object> hints() {
                return Map.of();
            }
        }).block();
        return sink.getBodyAsString().block();
    }

    @Test
    @DisplayName("Vertex 설정을 주면 publishers 경로와 GA 모델로 나간다")
    void buildsVertexUrl() {
        // Railway 에 넣을 값 그대로다. 둘 중 하나만 바꾸면 없는 모델을 부른다.
        client("https://aiplatform.googleapis.com/v1/publishers/google",
                "gemini-2.5-flash-tts")
                .synthesize("안녕", "방귀쟁이 며느리");

        assertThat(requests.getFirst().url().toString()).isEqualTo(
                "https://aiplatform.googleapis.com/v1/publishers/google"
                        + "/models/gemini-2.5-flash-tts:generateContent");
    }

    @Test
    @DisplayName("대조군 - AI Studio 설정이면 예전 경로 그대로다")
    void buildsAiStudioUrl() {
        client("https://generativelanguage.googleapis.com/v1beta",
                "gemini-2.5-flash-preview-tts")
                .synthesize("안녕", "방귀쟁이 며느리");

        assertThat(requests.getFirst().url().toString()).isEqualTo(
                "https://generativelanguage.googleapis.com/v1beta"
                        + "/models/gemini-2.5-flash-preview-tts:generateContent");
    }

    @Test
    @DisplayName("키는 쿼리스트링이 아니라 헤더로 나간다 - 장애 로그에 키가 새면 안 된다")
    void sendsKeyInHeader() {
        client("https://aiplatform.googleapis.com/v1/publishers/google",
                "gemini-2.5-flash-tts")
                .synthesize("안녕", "방귀쟁이 며느리");

        ClientRequest request = requests.getFirst();
        assertThat(request.headers().getFirst("x-goog-api-key")).isEqualTo("test-key");
        assertThat(request.url().toString()).doesNotContain("test-key");
    }

    @Test
    @DisplayName("본문에 role:user 를 싣는다 - Vertex 는 없으면 400 이다")
    void sendsRole() {
        client("https://aiplatform.googleapis.com/v1/publishers/google",
                "gemini-2.5-flash-tts")
                .synthesize("안녕", "방귀쟁이 며느리");

        String body = bodyOf(requests.getFirst());
        assertThat(body).contains("\"role\":\"user\"");
        // 캐릭터 보이스와 연기 지시도 같은 본문에 실려야 한 사람으로 들린다.
        assertThat(body).contains("Leda").contains("따뜻하게 말해줘:");
    }

    @Test
    @DisplayName("429 는 물러섰다 다시 친다 - 동시 요청에 한도가 걸려도 그 턴이 죽지 않는다")
    void retriesOnRateLimit() {
        SynthesizedAudio result = client(
                "https://aiplatform.googleapis.com/v1/publishers/google",
                "gemini-2.5-flash-tts",
                HttpStatus.TOO_MANY_REQUESTS, HttpStatus.TOO_MANY_REQUESTS, HttpStatus.OK)
                .synthesize("안녕", "방귀쟁이 며느리");

        assertThat(result.audioUrl()).startsWith("data:audio/wav;base64,");
        assertThat(requests).hasSize(3);
    }

    @Test
    @DisplayName("대조군 - 401 은 다시 치지 않는다 (키가 틀린 것을 재시도로 덮으면 안 된다)")
    void doesNotRetryOnAuthFailure() {
        GeminiTtsClient client = client(
                "https://aiplatform.googleapis.com/v1/publishers/google",
                "gemini-2.5-flash-tts",
                HttpStatus.UNAUTHORIZED);

        assertThatThrownBy(() -> client.synthesize("안녕", "방귀쟁이 며느리"))
                .isInstanceOf(Exception.class);

        assertThat(requests).hasSize(1);
    }
}
