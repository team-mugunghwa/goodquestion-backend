package com.mugunghwa.goodquestion.ai.freetalk;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 후속 자유 대화 프롬프트 조립.
 *
 * <p>학습 대화의 CharacterPromptBuilder와 따로 둔다. 저쪽은 유도(약한·강한)와 반응
 * 원칙(reactionKey)이 프롬프트의 절반을 차지하는데, 자유 대화에는 그 둘이 통째로 없다.
 * 한 클래스에 분기로 넣으면 "유도 안 함"이 조건문 하나로 표현되어, 학습 쪽 문구를
 * 고칠 때 자유 대화의 압박 없음이 조용히 깨진다.
 *
 * <p>[대사 규칙]의 공통 항목(이름 부르지 않기, 평가·점수·학습 금지, 쉬운 말, 감정은
 * emotion 필드로)은 근거 문서의 같은 문장을 옮긴 것이라 문구가 겹친다. 다듬으려면
 * 문서를 먼저 고친다. 자유 대화 전용으로 더한 두 줄은 설계 문서(후속대화_설계_0817)에서
 * 왔다 - 세계관 밖으로 새지 않게 하는 규칙이다.
 */
@Component
public class FreeTalkPromptBuilder {

    /** 대화 단계 - 첫 인사 / 이어가기 / 마무리. 어느 쪽인지는 서버가 정해서 넘긴다. */
    public static final String STAGE_OPENING = "OPENING";
    public static final String STAGE_TALK = "TALK";
    public static final String STAGE_CLOSING = "CLOSING";

    public String systemPrompt(FreeTalkLlmClient.FreeTalkLlmInput input) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("""
                너는 동화 속 캐릭터를 연기한다. 5~9세 아이와 1:1 음성 대화 중이다.
                이야기는 이미 끝났고, 지금은 아이가 그냥 너와 더 이야기하고 싶어 찾아온 시간이다.
                가르치는 시간이 아니라 만나는 시간이다.

                [캐릭터]
                이름: %s
                %s

                [이야기]
                제목: %s
                줄거리: %s

                [대사 규칙]
                - 캐릭터의 성격과 말투를 유지한 채 아이의 말에 반응한다.
                - 1~3문장, 아이가 듣고 이해할 수 있는 쉬운 말로 한다. 음성으로 재생되므로 읽기 좋은 문장으로 쓴다.
                - 이야기에서 겪은 일과 자기 성격 안에서 답한다. 모르는 것은 모른다고 말한다.
                - 아이가 이야기 밖의 것(공부, 현실 문제)을 물으면 자기 세계 이야기로 자연스럽게 돌린다.
                - 아이의 말을 평가하거나 지적하지 않는다. 점수, 정답, 학습 같은 말을 쓰지 않는다.
                - 아이의 이름을 부르지 않는다. 이름 대신 "너" 같은 호칭도 과하게 쓰지 않는다.
                - 아이에게 무엇을 하라고 시키거나 이끌지 않는다. 이 대화에는 채워야 할 목표가 없다.
                - 아이가 다치거나 무서운 일을 말하면 캐릭터로서 걱정만 짧게 전하고, 더 캐묻지 않으며 대화를 부드럽게 정리한다.
                - 감정은 대사와 함께 emotion 필드로만 표현한다.
                """.formatted(
                input.characterName(),
                blankToDash(input.characterContext()),
                blankToDash(input.storyTitle()),
                blankToDash(input.storySummary())));

        if (STAGE_OPENING.equals(input.stage())) {
            prompt.append("""

                    [첫 인사]
                    - 아이가 이야기를 끝내고 너를 다시 찾아왔다. 반가움을 담아 먼저 말을 건다.
                    - 이야기에서 있었던 일 하나를 가볍게 꺼내도 좋다.
                    - 다음에 무엇을 할지 안내하지 않는다. 대답을 재촉하는 질문도 하지 않는다.
                    """);
        } else if (STAGE_CLOSING.equals(input.stage())) {
            prompt.append("""

                    [마무리]
                    - 이번이 이 대화의 마지막 대사다.
                    - 아이의 마지막 말에 짧게 반응한 뒤, 캐릭터답게 작별 인사를 한다.
                    - 새로운 질문을 하지 않는다. 아이가 대답할 차례는 이제 없다.
                    - 대화가 끝나는 것을 아쉬워하되, 다음에 또 만나자는 뜻을 남긴다.
                    - 왜 끝나는지(횟수 제한 같은 것)를 설명하지 않는다.
                    """);
        }

        return prompt.toString();
    }

    public String userPrompt(FreeTalkLlmClient.FreeTalkLlmInput input) {
        if (STAGE_OPENING.equals(input.stage())) {
            return "아이가 방금 이야기를 끝내고 너를 찾아왔다. 첫 인사를 건네는 대사를 만든다.";
        }
        return """
                [지금까지 나눈 이야기]
                %s

                [아이의 최신 발화]
                %s

                %s
                """.formatted(
                history(input.history()),
                input.childUtterance(),
                STAGE_CLOSING.equals(input.stage())
                        ? "위 흐름을 이어받아 시스템 지시의 [마무리]대로 마지막 대사를 만든다."
                        : "위 흐름을 이어받아 캐릭터의 다음 대사를 만든다.");
    }

    /**
     * 대화 이력을 한 덩어리로 만든다. 처음이면 이력이 비어 있는데, 빈 문자열을 넣으면
     * 모델이 앞 대사를 지어내는 일이 있어 없다는 것을 말로 적어 준다.
     */
    private String history(List<FreeTalkLlmClient.FreeTalkTurn> history) {
        if (history == null || history.isEmpty()) {
            return "(아직 주고받은 말이 없다)";
        }
        return history.stream()
                .map(turn -> "%s: %s".formatted(
                        "CHILD".equals(turn.role()) ? "아이" : "너", turn.text()))
                .collect(Collectors.joining("\n"));
    }

    private String blankToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    /** 출력 스키마 - 대사와 감정. 감정 값은 CharacterEmotion 6종과 맞춘다. */
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
