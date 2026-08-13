package com.mugunghwa.goodquestion.ai.word;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/** 단어 뜻·이야기 속 예문 생성 프롬프트 조립 (단어-02) */
@Component
public class WordMeaningPromptBuilder {

    public String buildSystemPrompt() {
        return """
                너는 초등학교 1~3학년 아이에게 낱말 뜻을 풀이해 주는 도우미야.

                규칙:
                1) meaning은 5자 이상 20자 이하의 한 문장으로 쓴다. 어려운 한자어, 전문용어,
                   존댓말 종결어미(-습니다/-해요 등)는 쓰지 않는다.
                2) exampleSentence는 전달받은 이야기 장면 설명 속 상황을 그대로 반영한 한 문장으로
                   쓴다. 장면 설명이 없으면 아이가 이해하기 쉬운 일반적인 상황으로 대체한다.
                3) 반드시 아래 JSON 스키마 형식으로만 답한다. 그 외 다른 문장은 절대 덧붙이지 않는다.

                {
                  "meaning": "string",
                  "exampleSentence": "string"
                }
                """;
    }

    public String buildUserPrompt(String word, String sceneContext) {
        String scene = (sceneContext == null || sceneContext.isBlank())
                ? "(장면 설명 없음)"
                : sceneContext;
        return "단어: %s\n이야기 장면 설명: %s".formatted(word, scene);
    }

    /** 출력 스키마 — 낱말 뜻과 이야기 속 예문. */
    public Map<String, Object> outputSchema() {
        return Map.of(
                "type", "object",
                "additionalProperties", false,
                "required", List.of("meaning", "exampleSentence"),
                "properties", Map.of(
                        "meaning", Map.of("type", "string",
                                "description", "5자 이상 20자 이하의 쉬운 우리말 뜻풀이"),
                        "exampleSentence", Map.of("type", "string",
                                "description", "이야기 장면 상황을 반영한 한 문장 예문")));
    }
}
