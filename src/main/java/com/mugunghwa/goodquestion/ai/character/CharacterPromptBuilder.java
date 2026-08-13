package com.mugunghwa.goodquestion.ai.character;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 캐릭터 응답 프롬프트 조립 (대화 작동 규칙 3.1~3.2, 통합 명세서 6장).
 *
 * <p>반응 원칙(reactionKey)과 유도 방식(모드, 걱정 정보)은 서버가 이미 정해서 들어온다.
 * 프롬프트는 그 결정을 연기 지시로 옮길 뿐, LLM에게 판단을 다시 묻지 않는다(정책-02).
 *
 * <p>유도 원칙과 금지 사항은 근거 문서의 문구를 그대로 옮긴 것이다. 다듬으려면 문서를
 * 먼저 고친다.
 */
@Component
public class CharacterPromptBuilder {

    /** reactionKey별 연기 지시 (대화 작동 규칙 3.1 표의 "동작" 열). */
    private static final Map<String, String> REACTION_INSTRUCTIONS = Map.of(
            "PLAYFUL_UTTERANCE", "아이가 장난스럽게 말했다. 장난을 실제 사건으로 단정하지 말고 가볍게 받아친 뒤 이야기로 돌아온다.",
            "QUESTION_FROM_CHILD", "아이가 질문했다. 질문에 먼저 답한다.",
            "PROPOSAL_FROM_CHILD", "아이가 해결 방법을 제안했다. 제안의 도움이 되는 점을 인정한다. 걱정을 얹으라는 지시가 있으면 하나만 얹는다.",
            "UNCLEAR_UTTERANCE", "아이 말이 짧거나 뜻을 알기 어렵다. 필요할 때만 짧게 되묻는다.",
            "EMPATHY_FROM_CHILD", "아이가 감정을 이해하고 배려해 주었다. 공감으로 반응한다.",
            "DISAGREEMENT", "아이가 의견이나 반박, 결정을 말했다. 무조건 부정하지 말고, 일리를 인정하되 걱정 하나를 드러낼 수 있다.",
            "DIRECT_RESPONSE", "아이의 최신 말에 직접 반응한다.");

    public String systemPrompt(CharacterLlmClient.CharacterLlmInput input) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("""
                너는 동화 속 캐릭터를 연기한다. 5~9세 아이와 1:1 음성 대화 중이다.

                [캐릭터]
                이름: %s
                %s
                """.formatted(input.characterName(), input.characterContext()));

        prompt.append("""

                [대사 규칙]
                - 캐릭터의 성격과 입장을 유지한 채 아이의 말에 반응한다.
                - 1~3문장, 아이가 듣고 이해할 수 있는 쉬운 말로 한다. 음성으로 재생되므로 읽기 좋은 문장으로 쓴다.
                - 아이의 이름을 부르지 않는다. 이름 대신 "너" 같은 호칭도 과하게 쓰지 않는다.
                - 아이의 답을 평가하거나 지적하지 않는다. 점수, 정답, 학습 같은 말을 쓰지 않는다.
                - "해결 방법을 말해 봐", "다른 방법은 무엇이 있을까?" 같은 직접적인 학습 질문을 하지 않는다.
                - 장면을 마무리하는 듯한 작별·정리 톤을 쓰지 않는다. 이야기는 아직 진행 중이다.
                - 감정은 대사와 함께 emotion 필드로만 표현한다.
                """);

        if (input.remainingWorry() != null && !input.remainingWorry().isBlank()) {
            if (input.softCue()) {
                prompt.append("""

                        [약한 유도]
                        아이의 발화에 대한 반응이 중심이다. 반응을 마친 뒤, 아래의 남은 걱정을
                        가볍게 흘리듯 드러낼 수 있다. 유도 정보는 부가적으로만 쓰고 직접적인 답
                        요구는 하지 않는다.
                        남은 걱정: %s
                        """.formatted(input.remainingWorry()));
            } else {
                prompt.append("""

                        [강한 유도]
                        아래의 걱정을 이번 대사의 핵심 반응으로 드러낸다. 한 번에 이 걱정 하나만
                        유도한다. 캐릭터의 상황과 감정 안에서 걱정으로 표현하고, 정답이나 모범
                        답안을 캐릭터가 먼저 말하지 않는다.
                        걱정: %s
                        """.formatted(input.remainingWorry()));
            }
            if (input.guidanceStyle() != null && !input.guidanceStyle().isBlank()) {
                prompt.append("걱정을 드러내는 방식: ").append(input.guidanceStyle()).append('\n');
            }
        }

        return prompt.toString();
    }

    public String userPrompt(CharacterLlmClient.CharacterLlmInput input) {
        return """
                [이번 반응 방식]
                %s

                [아이의 최신 발화]
                %s

                [발화 분석 요약]
                %s

                위 반응 방식에 따라 캐릭터의 다음 대사를 만든다.
                """.formatted(
                REACTION_INSTRUCTIONS.getOrDefault(input.reactionKey(),
                        REACTION_INSTRUCTIONS.get("DIRECT_RESPONSE")),
                input.childUtterance(),
                input.analysisSummary());
    }

    /** 출력 스키마 — 대사와 감정. 감정 값은 CharacterEmotion 6종과 맞춘다. */
    public Map<String, Object> outputSchema() {
        return Map.of(
                "type", "object",
                "additionalProperties", false,
                "required", List.of("text", "emotion"),
                "properties", Map.of(
                        "text", Map.of("type", "string",
                                "description", "캐릭터의 다음 대사. 1~3문장의 한국어"),
                        "emotion", Map.of("type", "string", "enum", List.of(
                                "NEUTRAL", "HAPPY", "SAD", "WORRIED", "SURPRISED", "RELIEVED"))));
    }
}
