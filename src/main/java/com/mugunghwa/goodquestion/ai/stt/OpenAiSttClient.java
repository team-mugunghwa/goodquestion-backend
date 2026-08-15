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
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

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
    private final String prompt;

    public OpenAiSttClient(WebClient webClient,
                           @Value("${external.llm.base-url:https://api.openai.com/v1}") String baseUrl,
                           @Value("${external.stt.api-key}") String apiKey,
                           @Value("${external.stt.model:gpt-4o-mini-transcribe}") String model,
                           @Value("${external.stt.vocabulary-hint:}") String vocabularyHint,
                           @Value("${external.stt.prompt:}") String prompt) {
        this.webClient = webClient;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.model = model;
        this.vocabularyHint = vocabularyHint;
        this.prompt = prompt;
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
        //
        // 어휘를 쉼표 나열로 보내면 무음/뭉개진 오디오에서 모델이 목록을 그대로 복창하는
        // 환각을 잘 유발한다 - 같은 어휘를 이야기 문장에 녹인 prompt가 있으면 그쪽을
        // 우선한다. 에코 판정(isVocabularyEcho)은 계속 vocabularyHint의 어휘 목록으로
        // 한다 - 문장형 프롬프트를 복창해도 어휘 등장 비율(재조합 판정)에 걸린다.
        String promptHint = !prompt.isBlank() ? prompt : vocabularyHint;
        if (!promptHint.isBlank()) {
            body.part("prompt", promptHint);
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

        String text = response.path("text").asText(null);
        // 무음이거나 인식할 발화가 없으면 모델이 prompt로 준 어휘 힌트를 그대로 돌려준다.
        // 그대로 통과시키면 아이 말로 저장되고 캐릭터가 엉뚱하게 반응한다 - 빈 결과로 돌려
        // SpeechService의 STT_EMPTY_TEXT(422) 경로를 타게 한다.
        if (isVocabularyEcho(text)) {
            return new SttResult(null, null);
        }
        return new SttResult(text, confidenceFrom(response.path("logprobs")));
    }

    /**
     * 재조합 에코 판정 기준. 서로 다른 힌트 단어가 이 비율 이상 한 발화에 등장하면
     * 에코로 본다. 힌트는 여러 장면에 걸친 어휘의 합집합이라, 진짜 아이 발화가 이만큼
     * 폭넓게 쓸 일이 없다(실측 재조합 에코는 9개 중 8개, 정상 발화는 2개 수준이었다).
     */
    private static final double ECHO_COVERAGE_THRESHOLD = 2.0 / 3;

    /**
     * 결과가 어휘 힌트의 반복(에코)인지.
     *
     * <p>무음이거나 알아들을 수 없는 소리(샘플레이트 불일치로 뭉개진 녹음 등)가 들어오면
     * 모델이 오디오 대신 prompt의 어휘 힌트에 기대어 출력을 만드는 환각이 있다. 실측된
     * 형태가 셋이다: 힌트 전체를 그대로 반복, 일부만 나열, 힌트 단어들을 조사와 서술어로
     * 이어 문장처럼 재조합("며느리와 시아버지가 방귀를 뀌는 사이, ...").
     *
     * <p>판정은 두 갈래다.
     * <ul>
     *   <li>나열 에코 - 결과에서 힌트 단어를 전부 지웠을 때 아무것도 남지 않으면 힌트 말고는
     *       내용이 없는 것이다. 전체/부분/순서 바뀜을 한 번에 잡는다. 아이가 한 단어로
     *       답하는 경우("방귀!")를 지키기 위해 힌트 단어 2개 이상일 때만 적용한다.</li>
     *   <li>재조합 에코 - 서로 다른 힌트 단어의 등장 비율이 기준 이상이면 에코로 본다.
     *       조사와 서술어가 붙어 나열 판정을 빠져나가는 형태를 잡는다.</li>
     * </ul>
     *
     * <p>오판(정상 발화를 에코로 봄)의 비용은 "다시 말해 볼까?" 안내 한 번이라 크지 않지만,
     * 놓침의 비용은 아이가 하지 않은 말이 저장되고 보호자 리포트 후보에 오르는 것이다.
     */
    boolean isVocabularyEcho(String text) {
        if (text == null || vocabularyHint.isBlank()) {
            return false;
        }
        // 긴 단어부터 지워야 한다 - 짧은 단어가 긴 단어의 일부면 조각이 남는다.
        List<String> hintWords = Arrays.stream(vocabularyHint.split("[,\\s]+"))
                .filter(word -> !word.isBlank())
                .distinct()
                .sorted(Comparator.comparingInt(String::length).reversed())
                .toList();
        if (hintWords.isEmpty()) {
            return false;
        }

        String normalized = normalize(text);
        long matched = hintWords.stream().filter(normalized::contains).count();

        if (matched >= 2) {
            String remainder = normalized;
            for (String word : hintWords) {
                remainder = remainder.replace(word, "");
            }
            if (remainder.isBlank()) {
                return true;
            }
        }
        return hintWords.size() >= 3
                && matched >= Math.ceil(hintWords.size() * ECHO_COVERAGE_THRESHOLD);
    }

    private static String normalize(String value) {
        return value.replaceAll("[\\s,.·!?'\"]", "");
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
