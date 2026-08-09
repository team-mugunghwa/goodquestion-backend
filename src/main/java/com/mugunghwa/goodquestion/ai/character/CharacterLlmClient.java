package com.mugunghwa.goodquestion.ai.character;

import org.springframework.stereotype.Component;

/**
 * 캐릭터 응답 LLM 호출 (NORMAL/GUIDED).
 * 전달: 아이 최신 발화, 분석 결과, 진행 모드, 캐릭터 감정·상황, (유도 시) remainingWorries.
 * 출력: 캐릭터 대사 + 감정(CharacterEmotion 값).
 */
@Component
public class CharacterLlmClient {

    public CharacterLlmResult reply(CharacterLlmInput input) {
        throw new UnsupportedOperationException("TODO");
    }

    public record CharacterLlmInput(String childUtterance, String analysisSummary, String mode,
                                    String characterContext, String remainingWorry) {}

    public record CharacterLlmResult(String text, String emotion) {}
}
