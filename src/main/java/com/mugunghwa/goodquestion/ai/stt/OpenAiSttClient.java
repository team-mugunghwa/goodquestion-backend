package com.mugunghwa.goodquestion.ai.stt;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.JsonNode;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * OpenAI 음성 인식 (gpt-4o-mini-transcribe).
 *
 * <p>벤더 비교를 위한 실측 구현이다(미결-01: 아동 한국어 인식률 검증 필수). 다른 벤더로
 * 확정되면 이 구현체만 교체한다 - SttClient 인터페이스가 그 경계다.
 *
 * <p>원본 음성은 벤더 호출에만 쓰고 저장하지 않는다(음성-07). 로그에도 남기지 않는다.
 */
@Component
public class OpenAiSttClient implements SttClient {

    private final WebClient webClient;
    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final String vocabularyHint;

    public OpenAiSttClient(WebClient webClient,
                           @Value("${external.llm.base-url:https://api.openai.com/v1}") String baseUrl,
                           @Value("${external.stt.api-key}") String apiKey,
                           @Value("${external.stt.model:gpt-4o-mini-transcribe}") String model,
                           @Value("${external.stt.vocabulary-hint:}") String vocabularyHint) {
        this.webClient = webClient;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.model = model;
        this.vocabularyHint = vocabularyHint;
    }

    @Override
    public SttResult transcribe(MultipartFile audio) {
        MultipartBodyBuilder body = new MultipartBodyBuilder();
        body.part("file", toResource(audio))
                .contentType(MediaType.parseMediaType(
                        audio.getContentType() != null ? audio.getContentType() : "audio/wav"));
        body.part("model", model);
        // 아동 발화는 언어 자동 감지가 흔들린다. 서비스가 한국어 전용이므로 고정한다.
        body.part("language", "ko");
        // gpt-4o 계열 transcribe는 세그먼트 통계(avg_logprob 등)가 없고 토큰 logprob만
        // 준다. json 포맷에서만 include가 동작한다 - 신뢰도는 서버가 집계한다.
        body.part("response_format", "json");
        body.part("include[]", "logprobs");
        // 이야기 어휘 힌트. 실측에서 "방귀를 뀌어서"가 "방비를 끼어서"로 오인식됐다 -
        // 흔치 않은 단어일수록 힌트 효과가 크다. 장면별 proper_nouns를 요청에 실어 보내는
        // 구조가 정석이지만 /api/stt 계약에 장면 정보가 없어, 우선 서비스 전체 어휘를
        // 설정으로 준다(이야기가 1편이라 가능한 타협이다).
        if (!vocabularyHint.isBlank()) {
            body.part("prompt", vocabularyHint);
        }

        JsonNode response = webClient.post()
                .uri(baseUrl + "/audio/transcriptions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(body.build()))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        if (response == null) {
            return new SttResult(null, null);
        }
        return new SttResult(response.path("text").asText(null),
                confidenceFrom(response.path("logprobs")));
    }

    /**
     * 발화 신뢰도 = exp(토큰 logprob 평균), 즉 토큰 확률의 기하평균(0~1).
     * ASR 통용 집계다 - 문장 단위 요약값이 없어 서버가 직접 계산한다.
     * logprob이 없거나 비어 있으면 null - 낮음(0)과 모름(null)을 구분해야
     * 판정(SttConfidencePolicy)이 모름을 걸러내지 않을 수 있다.
     */
    static BigDecimal confidenceFrom(JsonNode logprobs) {
        if (logprobs == null || !logprobs.isArray() || logprobs.isEmpty()) {
            return null;
        }
        double sum = 0;
        int count = 0;
        for (JsonNode entry : logprobs) {
            JsonNode logprob = entry.path("logprob");
            if (logprob.isNumber()) {
                sum += logprob.asDouble();
                count++;
            }
        }
        if (count == 0) {
            return null;
        }
        double confidence = Math.exp(sum / count);
        return BigDecimal.valueOf(Math.min(1.0, confidence)).setScale(3, RoundingMode.HALF_UP);
    }

    /** 파일명이 있어야 벤더가 포맷을 인식한다. MultipartFile 원본 이름을 유지한다. */
    private ByteArrayResource toResource(MultipartFile audio) {
        try {
            byte[] bytes = audio.getBytes();
            String filename = audio.getOriginalFilename() != null
                    ? audio.getOriginalFilename() : "audio.wav";
            return new ByteArrayResource(bytes) {
                @Override
                public String getFilename() {
                    return filename;
                }
            };
        } catch (IOException e) {
            throw new UncheckedIOException("업로드 음성을 읽지 못했습니다", e);
        }
    }
}
