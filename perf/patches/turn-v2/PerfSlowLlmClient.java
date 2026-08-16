package com.mugunghwa.goodquestion.ai.llm;

import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

/**
 * [perf 하네스 대역] perf 프로파일에서만 켜지는 느린 LLM. v2(0feceae)는 LlmClient
 * 포트가 있어 이 파일 하나를 얹으면 어댑터(analysis/character)가 그대로 이 대역을
 * 탄다 - 기존 코드는 한 줄도 바꾸지 않는다.
 *
 * 응답 JSON은 분석/캐릭터 어댑터가 읽는 경로의 합집합이다. 각 어댑터는 자기
 * 필드만 꺼내 가므로 하나의 고정 응답으로 두 스키마를 모두 감당한다.
 * 지연은 PERF_LLM_DELAY_MS(기본 2000ms)로 조절한다.
 */
@Component
@Primary
@Profile("perf")
public class PerfSlowLlmClient implements LlmClient {

    private static final long DELAY_MS =
            Long.parseLong(System.getenv().getOrDefault("PERF_LLM_DELAY_MS", "2000"));

    private static final String CANNED_JSON = """
            {
              "childIntent": "이야기에 대답한다",
              "mainPoint": "측정용 발화",
              "detectedElements": [],
              "utteranceValidity": "VALID",
              "text": "그랬구나! 정말 신기했겠다.",
              "emotion": "HAPPY"
            }
            """;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public LlmJsonResult completeJson(String systemPrompt, String userPrompt,
                                      String schemaName, Map<String, Object> schema) {
        try {
            Thread.sleep(DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return new LlmJsonResult(objectMapper.readTree(CANNED_JSON), "perf-stub");
    }
}
