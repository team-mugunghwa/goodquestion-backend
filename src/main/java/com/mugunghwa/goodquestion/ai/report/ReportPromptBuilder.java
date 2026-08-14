package com.mugunghwa.goodquestion.ai.report;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 보호자 리포트 생성 프롬프트 조립 (리포트 요건 3~8절 원문 기준).
 *
 * <p>시스템 프롬프트의 역량 정의와 작성 원칙은 요건 문서를 옮긴 것이다. 문구를 다듬고
 * 싶어도 근거 문서를 먼저 고치고 옮긴다 — 여기서만 고치면 기획이 합의한 기준과 갈라진다.
 */
@Component
@RequiredArgsConstructor
public class ReportPromptBuilder {

    private final ObjectMapper objectMapper;

    static final String SYSTEM_PROMPT = """
            너는 초등 1~2학년 아이의 말하기 활동 기록을 읽고 보호자에게 보여줄 리포트를 쓰는 작성자다.

            [전제]
            초등 1~2학년은 말하기 능력을 정량 평가하는 대상이 아니다. 대화에 즐겁게 참여하고
            상대의 말에 반응하며 자기 생각을 표현하는 경험을 쌓는 단계다. 리포트는 아이를
            평가하는 자료가 아니라 보호자가 아이와 더 이야기하도록 돕는 참고 자료다.

            [작성 원칙]
            1. 반드시 아이의 실제 발화를 근거로 쓴다. 발화에 없는 내용을 지어내지 않는다.
            2. 잘한 점을 먼저 쓰고 보완할 부분을 뒤에 쓴다.
            3. "논리력이 부족합니다" 같은 단정적인 표현을 쓰지 않는다.
            4. DECISION, REASON 같은 내부 분석 태그를 문장에 노출하지 않는다.
               보호자가 읽을 우리말 표현으로 바꿔 쓴다.
            5. 가정 연계 질문은 학습 과제가 아니라 자연스러운 대화 형태로 쓴다.
            6. 특징이 뚜렷하지 않으면 부정적으로 적지 않고, 다음에 해 볼 것을 권하는 문구로 쓴다.

            [역량 5종 - competencies에 이 순서로 담는다]
            1. 관점과 공감 - 다른 사람의 생각과 감정을 이해하는가
               (PERSPECTIVE, EMPATHY의 완성도)
            2. 감정 표현 - 감정과 그 원인을 구체적으로 말하는가
               (EMOTION의 다양성, EMOTION과 REASON의 연결 정도)
            3. 상호작용 - 상대의 말에 맞게 반응하고 구체적으로 요청하는가
               (맥락 적합성, 상대 의견에 대한 대응, REQUEST의 구체성)
            4. 생각과 이유 - 자신의 판단과 근거를 함께 말하는가
               (DECISION과 REASON의 완성도와 연결 정도)
            5. 결과와 해결 - 결과를 예상하고 해결 방법을 제안하는가
               (RESULT와 SOLUTION의 완성도와 연결 정도)

            각 역량은 name, finding(이번 활동에서 나타난 특징), evidenceUtterance(근거가 된
            실제 발화 그대로), strength(잘한 점), nextFocus(추가로 보완할 부분)를 채운다.
            근거로 쓸 발화가 없으면 evidenceUtterance는 빈 문자열로 두고, finding에는
            이번 활동에서 그 모습이 잘 드러나지 않았다는 사실만 담담히 적는다.

            [어휘 분석]
            - mainWords: 아이가 사용한 주요 어휘
            - askedWords: 아이가 뜻을 물어본 어휘 (없으면 빈 배열)
            - repeatedExpressions: 반복적으로 사용한 표현 (없으면 빈 배열)
            - feedback: 어휘 사용에 대한 한두 문장. 특징이 뚜렷하지 않으면 다양한 어휘를
              써 보도록 권하는 문구를 쓴다.

            [대표 발화 - 1건만]
            아이의 전체 발화 중 이야기의 핵심 장면과 질문 맥락에 적절하고, 아이의 생각이
            가장 자연스럽게 연결된 발화 1개를 고른다.
            발화를 의미 단위로 나눈 뒤 각 부분에 나타난 사고 요소와 그 연결 구조를 비교한다.
            단순히 길거나 사고 요소가 많은 발화보다, 이야기의 핵심 내용과 아이의 말하기
            강점이 잘 드러나는 발화를 우선한다.
            text는 아이 발화를 글자 그대로 옮기고, reason에는 왜 그 발화를 골랐는지
            한 문장으로 쓴다.

            [가정 연계 대화 가이드]
            이번 활동에서 나타난 강점과 보완점에 따라 질문 유형을 다르게 고른다.
            - 표현이 강하고 논리가 부족하면: 이유, 결과, 해결을 묻는 질문
            - 논리가 강하고 표현이 부족하면: 감정, 관점, 공감을 묻는 질문
            - 답변이 짧았으면: 이유, 사례, 다음 결과로 넓히는 질문

            storyQuestions는 이번 이야기의 상황과 등장인물을 바탕으로, 아이가 인물이 되거나
            새 장면을 상상하며 답할 수 있는 질문 2~3개를 쓴다.
            dailyLifeQuestions는 이야기의 주제를 아이의 실제 경험과 잇는 질문 2~3개를 쓴다.

            [summary / strengths / nextFocus]
            summary는 이번 활동 전체를 보호자에게 전하는 두세 문장이다.
            strengths와 nextFocus의 element에는 사고 요소 8종 중 하나를 그대로 쓰고,
            comment에는 보호자가 읽을 문장을 쓴다. 화면에는 comment만 보인다.

            [사고 요소 8종]
            DECISION 선택하거나 입장을 정함 / REASON 까닭을 말함 /
            PERSPECTIVE 다른 인물의 상황이나 입장을 고려함 / SOLUTION 구체적 해결 방법 제시 /
            RESULT 이후의 결과를 설명하거나 예상함 / EMOTION 감정을 직접 표현함 /
            EMPATHY 다른 사람의 감정을 이해하고 배려함 / REQUEST 상대에게 변화를 요구함
            """;

    public String systemPrompt() {
        return SYSTEM_PROMPT;
    }

    /**
     * 사용자 프롬프트 — 세션 한 건의 대화와 확인된 사고 요소를 JSON으로 넘긴다.
     *
     * <p>인식이 미덥지 않았던 발화는 호출부에서 미리 걸러 넘긴다 — 저장된 원문이 아이가
     * 실제로 한 말과 다를 수 있는데, 리포트는 "아이가 이렇게 말했다"고 보여주는 자리다.
     */
    public String userPrompt(ReportLlmClient.ReportLlmInput input) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("storyTitle", input.storyTitle());
        payload.put("childName", input.childName());
        payload.put("childAge", input.childAge());
        payload.put("turns", input.turns());
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JacksonException e) {
            throw new IllegalStateException("리포트 입력 직렬화 실패", e);
        }
    }

    /** 출력 스키마. strict 모드라 모든 필드가 required고 추가 필드는 거부된다. */
    public Map<String, Object> outputSchema() {
        return Map.of(
                "type", "object",
                "additionalProperties", false,
                "required", List.of("summary", "strengths", "nextFocus", "vocabulary",
                        "competencies", "representativeUtterance", "homeGuide"),
                "properties", Map.of(
                        "summary", Map.of("type", "string"),
                        "strengths", reportItemArray(),
                        "nextFocus", reportItemArray(),
                        "vocabulary", Map.of(
                                "type", "object",
                                "additionalProperties", false,
                                "required", List.of("mainWords", "askedWords",
                                        "repeatedExpressions", "feedback"),
                                "properties", Map.of(
                                        "mainWords", stringArray(),
                                        "askedWords", stringArray(),
                                        "repeatedExpressions", stringArray(),
                                        "feedback", Map.of("type", "string"))),
                        "competencies", Map.of(
                                "type", "array",
                                "items", Map.of(
                                        "type", "object",
                                        "additionalProperties", false,
                                        "required", List.of("name", "finding", "evidenceUtterance",
                                                "strength", "nextFocus"),
                                        "properties", Map.of(
                                                "name", Map.of("type", "string"),
                                                "finding", Map.of("type", "string"),
                                                "evidenceUtterance", Map.of("type", "string"),
                                                "strength", Map.of("type", "string"),
                                                "nextFocus", Map.of("type", "string")))),
                        "representativeUtterance", Map.of(
                                "type", "object",
                                "additionalProperties", false,
                                "required", List.of("text", "reason"),
                                "properties", Map.of(
                                        "text", Map.of("type", "string"),
                                        "reason", Map.of("type", "string"))),
                        "homeGuide", Map.of(
                                "type", "object",
                                "additionalProperties", false,
                                "required", List.of("storyQuestions", "dailyLifeQuestions"),
                                "properties", Map.of(
                                        "storyQuestions", stringArray(),
                                        "dailyLifeQuestions", stringArray()))));
    }

    /** strengths·nextFocus가 같은 형태라 한 곳에서 만든다. */
    private Map<String, Object> reportItemArray() {
        return Map.of(
                "type", "array",
                "items", Map.of(
                        "type", "object",
                        "additionalProperties", false,
                        "required", List.of("element", "comment"),
                        "properties", Map.of(
                                "element", Map.of("type", "string", "enum", List.of(
                                        "DECISION", "REASON", "PERSPECTIVE", "SOLUTION",
                                        "RESULT", "EMOTION", "EMPATHY", "REQUEST")),
                                "comment", Map.of("type", "string"))));
    }

    private Map<String, Object> stringArray() {
        return Map.of("type", "array", "items", Map.of("type", "string"));
    }
}