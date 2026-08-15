package com.mugunghwa.goodquestion.ai.llm;

import reactor.core.publisher.Mono;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClientRequest;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * OpenAI Chat Completions 구현 (gpt-5-mini).
 *
 * <p>Structured Outputs(json_schema, strict)로 응답 형식을 공급자 수준에서 강제한다.
 * 프롬프트로 형식을 부탁하는 것과 달리 스키마 위반 응답 자체가 오지 않는다.
 *
 * <p>reasoning effort는 기본 minimal이다. gpt-5 계열은 추론 모델이라 기본값(medium)으로는
 * 발화 한 건에 수 초씩 생각한다. 이 서비스의 LLM 작업(요소 태깅, 대사 한두 문장)은 깊은
 * 추론이 필요한 종류가 아니고, 아이가 마이크 앞에서 기다리는 시간이 품질보다 비싸다.
 * 판정 품질이 부족하다고 판단되면 LLM_REASONING_EFFORT로 올려서 재본다.
 *
 * <p>타임아웃·연결 재시도는 공용 WebClient 설정을 따른다(WebClientConfig).
 */
@Component
public class OpenAiLlmClient implements LlmClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final String reasoningEffort;

    public OpenAiLlmClient(WebClient webClient, ObjectMapper objectMapper,
                           @Value("${external.llm.base-url:https://api.openai.com/v1}") String baseUrl,
                           @Value("${external.llm.api-key}") String apiKey,
                           @Value("${external.llm.model:gpt-5-mini}") String model,
                           @Value("${external.llm.reasoning-effort:minimal}") String reasoningEffort) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.model = model;
        this.reasoningEffort = reasoningEffort;
    }

    @Override
    public LlmJsonResult completeJson(String systemPrompt, String userPrompt,
                                      String schemaName, Map<String, Object> schema,
                                      Duration timeout) {
        Map<String, Object> body = Map.of(
                "model", model,
                "reasoning_effort", reasoningEffort,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)),
                "response_format", Map.of(
                        "type", "json_schema",
                        "json_schema", Map.of(
                                "name", schemaName,
                                "strict", true,
                                "schema", schema)));

        Mono<JsonNode> mono = webClient.post()
                .uri(baseUrl + "/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                // 요청 단위로 Netty 응답 타임아웃을 덮어쓴다.
                //
                // **reactive .timeout()만으로는 부족하다.** 그것은 HTTP 계층 위에서 도는
                // 상위 타이머라, 공용 responseTimeout(10초)이 더 짧으면 Netty가 먼저 끊는다.
                // 리포트에 30초를 넘겨도 10초에 잘리던 것이 이 때문이었다(08-15 실측).
                .httpRequest(request -> {
                    if (timeout != null) {
                        HttpClientRequest nettyRequest = request.getNativeRequest();
                        nettyRequest.responseTimeout(timeout);
                    }
                })
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode.class);

        JsonNode response = (timeout == null ? mono : mono.timeout(timeout)).block();

        if (response == null) {
            throw new IllegalStateException("LLM 응답이 비어 있습니다");
        }
        String content = response.path("choices").path(0).path("message").path("content").asText(null);
        if (content == null || content.isBlank()) {
            // refusal(안전 거부)이면 content 대신 refusal 필드가 온다. 원문을 로그로 남길 수
            // 없는 도메인이라(아이 발화) 실패 사실만 올린다.
            throw new IllegalStateException("LLM이 내용을 반환하지 않았습니다: "
                    + response.path("choices").path(0).path("message").path("refusal").asText("원인 미상"));
        }
        try {
            return new LlmJsonResult(objectMapper.readTree(content),
                    response.path("model").asText(model));
        } catch (tools.jackson.core.JacksonException e) {
            throw new IllegalStateException("LLM 응답 JSON 파싱 실패", e);
        }
    }
}
