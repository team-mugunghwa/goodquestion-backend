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
                1) meaning은 **띄어쓰기 기준 5~7덩어리(어절)**로 된 한 문장이다. 쓰고 나서
                   덩어리를 세어 보고, 8덩어리 이상이면 줄여 다시 쓴다.
                     기왓장 -> 지붕을 덮는 납작한 흙 조각이에요       (5덩어리)
                     이장   -> 마을의 일을 맡아서 돌보는 사람이에요    (5덩어리)
                     절구   -> 곡식을 넣고 빻을 때 쓰는 그릇이에요     (6덩어리)
                     부뚜막 -> 솥을 얹고 불을 때는 부엌의 흙 자리예요  (7덩어리)
                   **정확함을 해치면서까지 줄이지 않는다.** 낱말의 핵심(무엇이고 무엇에
                   쓰는지)이 빠질 것 같으면 7덩어리까지 쓴다. 다만 한 낱말에 재료 설명,
                   시대 설명, 용도 나열을 다 넣지는 않는다 - 가장 중요한 하나만 고른다.
                2) meaning은 "-이에요/-예요/-해요"처럼 아이에게 말하듯 부드러운 존댓말로
                   끝맺고, **마침표는 찍지 않는다.** 어려운 한자어와 전문용어는 쓰지 않는다.
                   단어 자체를 주어로 반복하지 않는다("OO는 ~이에요" 금지).
                3) 예문은 세 개를 만든다. 예문에는 마침표를 찍는다.
                   - exampleStory: 전달받은 이야기 장면 설명 속 상황을 그대로 반영한 한 문장.
                     장면 설명이 없으면 아이가 이해하기 쉬운 일반적인 상황으로 대체한다.
                   - exampleDaily: 이야기와 관계없이 일상 생활에서 이 단어를 어떻게 쓰는지
                     보여 주는 한 문장.
                   - exampleAdvanced: exampleDaily보다 한 단계 어려운 문장. 문장을 조금 더
                     길게 쓰되 초등 3학년이 이해할 수 있는 수준을 넘지 않는다.
                4) isRealWord는 **확실히 존재하지 않는 말일 때만** false다. 아이가 옛이야기를
                   듣다 만난 낱말이라 낯설거나, 옛말이거나, 사투리이거나, 잘 안 쓰는 말이어도
                   실제로 쓰이는 낱말이면 true다. 판단이 애매하면 true로 답한다 - 진짜 낱말을
                   막으면 아이가 궁금해한 말을 담지 못한다.
                   false는 자음/모음이 뭉개진 소리 조각처럼 어느 낱말로도 읽히지 않을 때만
                   쓴다. false일 때는 meaning과 예문 세 개를 모두 빈 문자열로 둔다.
                5) 반드시 아래 JSON 스키마 형식으로만 답한다. 그 외 다른 문장은 절대 덧붙이지 않는다.

                {
                  "meaning": "string",
                  "exampleStory": "string",
                  "exampleDaily": "string",
                  "exampleAdvanced": "string",
                  "isRealWord": true
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
                                "description", "띄어쓰기 기준 5~7어절, -이에요/-해요체로 끝나고 마침표가 없는 쉬운 우리말 뜻풀이"),
                        "exampleStory", Map.of("type", "string",
                                "description", "이야기 장면 상황을 반영한 한 문장 예문"),
                        "exampleDaily", Map.of("type", "string",
                                "description", "일상 생활에서의 쓰임을 보여 주는 한 문장 예문"),
                        "exampleAdvanced", Map.of("type", "string",
                                "description", "일상 예문보다 한 단계 어려운 한 문장 예문"),
                        "isRealWord", Map.of("type", "boolean",
                                "description", "실제 낱말이면 true. 옛말/사투리/낯선 말도 true. 어느 낱말로도 읽히지 않을 때만 false")));
    }
}
