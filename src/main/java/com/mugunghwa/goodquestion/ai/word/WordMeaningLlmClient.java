package com.mugunghwa.goodquestion.ai.word;

import org.springframework.stereotype.Component;

/** 단어장 — 아이 수준의 쉬운 뜻과 이야기 속 예문 생성 */
@Component
public class WordMeaningLlmClient {

    public WordMeaningResult generate(String word, String sceneContext) {
        throw new UnsupportedOperationException("TODO");
    }

    public record WordMeaningResult(String meaning, String exampleSentence) {}
}
