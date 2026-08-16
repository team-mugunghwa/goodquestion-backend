package com.mugunghwa.goodquestion.ai.character;

import org.springframework.stereotype.Component;

/**
 * [perf 하네스 대역] v1(57fb36f) 시점의 501 스텁을 "느린 벤더" 대역으로 교체한 것.
 * 지연 근거는 AnalysisLlmClient 대역의 주석 참고.
 */
@Component
public class CharacterLlmClient {

    private static final long DELAY_MS =
            Long.parseLong(System.getenv().getOrDefault("PERF_LLM_DELAY_MS", "2000"));

    public CharacterLlmResult reply(CharacterLlmInput input) {
        try {
            Thread.sleep(DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return new CharacterLlmResult("그랬구나! 정말 신기했겠다.", "HAPPY");
    }

    public record CharacterLlmInput(String childUtterance, String analysisSummary, String mode,
                                    String characterContext, String remainingWorry) {}

    public record CharacterLlmResult(String text, String emotion) {}
}
