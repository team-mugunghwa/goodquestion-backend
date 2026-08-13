package com.mugunghwa.goodquestion.ai.analysis;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.core.JacksonException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 발화 분석 프롬프트 조립 (발화 분석 문서 4~6장 원문 기준).
 *
 * <p>시스템 프롬프트의 판정 원칙과 과대평가 금지 예시는 문서 5장 "기본 판정 원칙"을
 * 그대로 옮긴 것이다. 문구를 다듬고 싶어도 근거 문서를 먼저 고치고 옮긴다 —
 * 여기서만 고치면 팀이 합의한 판정 기준과 프롬프트가 갈라진다.
 */
@Component
@RequiredArgsConstructor
public class AnalysisPromptBuilder {

    private final ObjectMapper objectMapper;

    static final String SYSTEM_PROMPT = """
            너는 동화 이야기 속 캐릭터와 대화하는 아이(5~9세)의 발화를 분석하는 분석기다.
            아이의 최신 발화 1건만 분석한다. 장면 진행 상태(누적 요소, 모드, 종료)는 판단하지 않는다.

            [판단할 것]
            - childIntent: 이번 발화의 중심 의도 1개
            - mainPoint: 아이 말의 핵심 뜻 (없으면 null)
            - detectedElements: 발화에서 직접 확인된 사고 요소와 원문 근거
            - utteranceValidity: 발화가 진행 판단에 쓸 수 있는 정보량

            [사고 요소 8종]
            - DECISION: 선택하거나 자신의 입장을 정함
            - REASON: 판단, 의견, 선택, 요청 등의 까닭을 말함
            - PERSPECTIVE: 다른 인물의 상황이나 입장을 고려함
            - SOLUTION: 문제를 줄이거나 해결할 구체적인 행동과 방법을 제시함
            - RESULT: 행동이나 상황 이후의 결과를 설명하거나 예상함
            - EMOTION: 자신이나 다른 인물의 감정을 직접 표현함
            - EMPATHY: 다른 사람의 감정이나 어려움을 이해하고 배려함
            - REQUEST: 특정 상대에게 행동, 말, 태도의 변화를 요구함

            [기본 판정 원칙]
            1. 최신 아이 발화에서 직접 확인되는 요소만 탐지한다.
            2. 아이가 말하지 않은 이유, 감정, 의도, 결과를 추론하여 추가하지 않는다.
            3. 각 요소에는 아이 발화에 실제로 존재하는 원문 근거(evidence)가 필요하다.
               evidence는 아이 발화의 표현을 글자 그대로 옮긴다. 띄어쓰기와 문장부호를
               다듬거나 바꿔 쓰지 않는다.
            4. 같은 표현이 여러 사고 요소를 직접 충족하면 복수 탐지가 가능하다.
            5. 장면 목표와 무관한 사고 요소는 탐지하지 않는다.
            6. 막연한 당위나 예의 표현만으로는 이유, 관점, 해결 방법을 인정하지 않는다.
               다음과 같은 표현이 구체적인 맥락 없이 단독으로 쓰이면 사고 요소로 과대평가하지 않는다.
               - "잘해줘야 해요" / "도와줘야 해요" / "착하게 해야 해요" / "그러면 안 돼요"
               반면 행동 대상과 방법이 구체적으로 포함되면 장면 기준에 따라 REQUEST 또는
               SOLUTION으로 인정할 수 있다.
            7. 상대방에게 구체적인 행동 변화를 요구한 경우 REQUEST로 인정할 수 있다.
            8. targetElements는 자동으로 감지되는 정답 목록이 아니다. 실제 발화가 장면별
               elementCriteria를 충족하고 원문 근거가 확인될 때만 해당 요소를 인정한다.
               같은 요소라도 장면에 따라 인정 기준이 다르므로 요소 이름만으로 판단하지 않는다.

            [childIntent 13종]
            QUESTION, OPINION, REASONING, SOLUTION, DECISION, PERSPECTIVE, EMOTION,
            REQUEST, CHALLENGE, PLAYFUL, OFF_TOPIC, SHORT_RESPONSE, UNCLEAR
            childIntent는 캐릭터의 반응 방식과 말투 조정에 쓰인다.
            장면 목표 달성 여부는 childIntent가 아니라 detectedElements 기준이므로,
            의도가 무엇이든 요소 탐지는 원칙대로 한다.

            [utteranceValidity 5종]
            - VALID: 의미가 파악되는 유효 발화
            - SHORT: 지나치게 짧음
            - UNCLEAR: 뜻을 알기 어려움
            - OFF_TOPIC: 장면과 관련 없음
            - PLAYFUL: 장난, 소리 흉내 등

            아이다운 표현(불완전한 문장, 의성어 섞임)은 자연스러운 것이다. 의미가 파악되면 VALID로 본다.
            """;

    public String systemPrompt() {
        return SYSTEM_PROMPT;
    }

    /** 사용자 프롬프트 — 문서 4장의 입력 명세 JSON을 그대로 만든다. */
    public String userPrompt(AnalysisLlmClient.AnalysisLlmInput input) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sceneContext", input.sceneContext());
        payload.put("goal", input.goal());
        payload.put("previousCharacterMessage", input.previousCharacterMessage());
        payload.put("childUtterance", input.childUtterance());
        payload.put("targetElements", input.targetElements());
        payload.put("elementCriteria", input.elementCriteria());
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JacksonException e) {
            throw new IllegalStateException("분석 입력 직렬화 실패", e);
        }
    }

    /** 출력 스키마 (문서 6장). strict 모드라 모든 필드가 required고 추가 필드는 거부된다. */
    public Map<String, Object> outputSchema() {
        return Map.of(
                "type", "object",
                "additionalProperties", false,
                "required", List.of("childIntent", "mainPoint", "detectedElements", "utteranceValidity"),
                "properties", Map.of(
                        "childIntent", Map.of("type", "string", "enum", List.of(
                                "QUESTION", "OPINION", "REASONING", "SOLUTION", "DECISION",
                                "PERSPECTIVE", "EMOTION", "REQUEST", "CHALLENGE", "PLAYFUL",
                                "OFF_TOPIC", "SHORT_RESPONSE", "UNCLEAR")),
                        "mainPoint", Map.of("type", List.of("string", "null")),
                        "detectedElements", Map.of(
                                "type", "array",
                                "items", Map.of(
                                        "type", "object",
                                        "additionalProperties", false,
                                        "required", List.of("type", "evidence"),
                                        "properties", Map.of(
                                                "type", Map.of("type", "string", "enum", List.of(
                                                        "DECISION", "REASON", "PERSPECTIVE", "SOLUTION",
                                                        "RESULT", "EMOTION", "EMPATHY", "REQUEST")),
                                                "evidence", Map.of("type", "string")))),
                        "utteranceValidity", Map.of("type", "string", "enum", List.of(
                                "VALID", "SHORT", "UNCLEAR", "OFF_TOPIC", "PLAYFUL"))));
    }
}
