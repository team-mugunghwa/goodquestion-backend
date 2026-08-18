package com.mugunghwa.goodquestion.ai.analysis;

import org.springframework.stereotype.Component;

/**
 * [perf 하네스 대역] v1(57fb36f) 시점의 501 스텁을 "느린 벤더" 대역으로 교체한 것.
 *
 * 트러블슈팅_턴_처리_커넥션_점유의 회귀 테스트가 쓴 것과 같은 기법이다 - 당시 LLM
 * 어댑터가 붙기 전이라, 벤더 지연을 흉내 내야 트랜잭션 안 호출의 커넥션 점유가
 * HTTP 계층에서 재현된다. 지연은 PERF_LLM_DELAY_MS(기본 2000ms)로 조절한다.
 */
@Component
public class AnalysisLlmClient {

    private static final long DELAY_MS =
            Long.parseLong(System.getenv().getOrDefault("PERF_LLM_DELAY_MS", "2000"));

    public AnalysisLlmResult analyze(AnalysisLlmInput input) {
        try {
            Thread.sleep(DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return new AnalysisLlmResult("이야기에 대답한다", "측정용 발화",
                java.util.List.of(), "VALID", "perf-stub");
    }

    public record AnalysisLlmInput(String sceneContext, String goal, String previousCharacterMessage,
                                   String childUtterance, java.util.List<String> targetElements,
                                   java.util.Map<String, String> elementCriteria) {}

    public record AnalysisLlmResult(String childIntent, String mainPoint,
                                    java.util.List<DetectedElementDto> detectedElements,
                                    String utteranceValidity, String modelId) {
        public record DetectedElementDto(String type, String evidence) {}
    }
}
