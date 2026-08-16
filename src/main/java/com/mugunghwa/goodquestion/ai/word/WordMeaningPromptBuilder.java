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
                1) meaning은 5자 이상 20자 이하의 한 문장으로, "-이에요/-예요/-해요"처럼
                   아이에게 말하듯 부드러운 존댓말로 끝맺는다. 어려운 한자어와 전문용어는
                   쓰지 않는다. 단어 자체를 주어로 반복하지 않는다("OO는 ~이에요" 금지).
                2) 예문은 세 개를 만든다.
                   - exampleStory: 전달받은 이야기 장면 설명 속 상황을 그대로 반영한 한 문장.
                     장면 설명이 없으면 아이가 이해하기 쉬운 일반적인 상황으로 대체한다.
                   - exampleDaily: 이야기와 관계없이 일상 생활에서 이 단어를 어떻게 쓰는지
                     보여 주는 한 문장.
                   - exampleAdvanced: exampleDaily보다 한 단계 어려운 문장. 문장을 조금 더
                     길게 쓰되 초등 3학년이 이해할 수 있는 수준을 넘지 않는다.
                3) isRealWord는 전달받은 단어가 실제로 쓰이는 우리말 낱말이면 true, 오타나
                   음성 인식 오류로 보이는 존재하지 않는 말이면 false로 답한다. false일 때는
                   meaning과 예문 세 개를 모두 빈 문자열로 둔다.
                4) 반드시 아래 JSON 스키마 형식으로만 답한다. 그 외 다른 문장은 절대 덧붙이지 않는다.

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
                "required", List.of("meaning", "exampleStory", "exampleDaily",
                        "exampleAdvanced", "isRealWord"),
                "properties", Map.of(
                        "meaning", Map.of("type", "string",
                                "description", "5자 이상 20자 이하, -이에요/-해요체로 끝나는 쉬운 우리말 뜻풀이"),
                        "exampleStory", Map.of("type", "string",
                                "description", "이야기 장면 상황을 반영한 한 문장 예문"),
                        "exampleDaily", Map.of("type", "string",
                                "description", "일상 생활에서의 쓰임을 보여 주는 한 문장 예문"),
                        "exampleAdvanced", Map.of("type", "string",
                                "description", "일상 예문보다 한 단계 어려운 한 문장 예문"),
                        "isRealWord", Map.of("type", "boolean",
                                "description", "실제로 쓰이는 우리말 낱말이면 true, 오타/오인식으로 보이면 false")));
    }
}
