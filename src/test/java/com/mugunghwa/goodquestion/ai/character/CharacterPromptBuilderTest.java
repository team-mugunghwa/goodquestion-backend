package com.mugunghwa.goodquestion.ai.character;

import com.mugunghwa.goodquestion.ai.character.CharacterLlmClient.CharacterLlmInput;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 프롬프트 조립 회귀 테스트 (LLM 무호출, 순수 문자열).
 *
 * <p>물음-종결 지시는 강한 유도에만 붙어야 한다. 약한 유도·CLOSING·무유도 경로에
 * 새면 문서 14장의 수위 구분(핵심 반응 vs 부가 유도)이 무너지고, 끝내는 턴에서
 * 마무리 대사와 물음이 겹친다.
 */
class CharacterPromptBuilderTest {

    private final CharacterPromptBuilder builder = new CharacterPromptBuilder();

    private CharacterLlmInput input(String mode, String worry, boolean softCue, String guidanceStyle) {
        return new CharacterLlmInput("응 알겠어", "아이가 공감을 표현함", mode,
                "DIRECT_RESPONSE", "방귀쟁이 며느리", "방귀를 참는 것이 힘든 며느리",
                worry, softCue, guidanceStyle);
    }

    @Test
    void 강한_유도는_물음_하나로_끝내라는_지시와_주어_혼용_금지를_담는다() {
        String prompt = builder.systemPrompt(
                input("NORMAL", "어떻게 하면 좋을지 모르겠어.", false, null));

        assertThat(prompt).contains("[강한 유도]");
        assertThat(prompt).contains("물음 하나로 끝낸다");
        // 주어를 겹쳐 세워 문장이 깨지던 것이 실제 사고였다(2026-08-18). 금지선과 깨진 예를 함께 박는다.
        assertThat(prompt).contains("한 물음 안에서 두 시점을 겹치지 않는다");
        assertThat(prompt).contains("너는 내가 그렇게 생각하는 모습 보면 어떨까?");
        // 걱정 문안 자체가 자문 형태라 물음이 둘로 쌓이던 것도 함께 막는다.
        assertThat(prompt).contains("새 물음을 만들어 뒤에 덧붙이지 않는다");
        assertThat(prompt).contains("한 대사에 물음표는 하나만 쓴다");
        assertThat(prompt).contains("안 되는 물음: \"해결 방법을 말해 봐.\"");
        assertThat(prompt).doesNotContain("[약한 유도]");
    }

    @Test
    void 약한_유도는_기존_문구_그대로이고_물음_종결_지시가_없다() {
        String prompt = builder.systemPrompt(
                input("NORMAL", "어떻게 하면 좋을지 모르겠어.", true, null));

        assertThat(prompt).contains("[약한 유도]");
        assertThat(prompt).contains("가볍게 흘리듯");
        assertThat(prompt).doesNotContain("[강한 유도]");
        assertThat(prompt).doesNotContain("물음 하나로 끝낸다");
    }

    @Test
    void 걱정이_없으면_유도_블록_자체가_없다() {
        String prompt = builder.systemPrompt(input("NORMAL", null, false, null));

        assertThat(prompt).doesNotContain("[강한 유도]");
        assertThat(prompt).doesNotContain("[약한 유도]");
        assertThat(prompt).doesNotContain("물음 하나로 끝낸다");
    }

    @Test
    void 끝내는_턴은_종료_전용_대사_규칙을_쓰고_유도_블록이_없다() {
        String prompt = builder.systemPrompt(input("CLOSING", null, false, null));

        assertThat(prompt).contains("[대사 규칙 - 장면이 끝나는 턴]");
        assertThat(prompt).doesNotContain("[강한 유도]");
        assertThat(prompt).doesNotContain("[약한 유도]");
    }

    @Test
    void userPrompt는_강한_유도_턴에만_유도_참조_문장으로_끝난다() {
        String strong = builder.userPrompt(
                input("NORMAL", "어떻게 하면 좋을지 모르겠어.", false, null));
        String soft = builder.userPrompt(
                input("NORMAL", "어떻게 하면 좋을지 모르겠어.", true, null));
        String closing = builder.userPrompt(
                input("CLOSING", "어떻게 하면 좋을지 모르겠어.", false, null));
        String noWorry = builder.userPrompt(input("NORMAL", null, false, null));

        assertThat(strong).contains("이번 대사의 핵심은 시스템 지시의 [강한 유도]다");
        for (String unchanged : new String[]{soft, closing, noWorry}) {
            assertThat(unchanged).contains("위 반응 방식에 따라 캐릭터의 다음 대사를 만든다.");
            assertThat(unchanged).doesNotContain("[강한 유도]");
        }
    }

    @Test
    void 유도_스타일이_있으면_드러내는_방식_줄이_유지된다() {
        String prompt = builder.systemPrompt(
                input("NORMAL", "어떻게 하면 좋을지 모르겠어.", false, "한숨을 쉬며 혼잣말처럼"));

        assertThat(prompt).contains("걱정을 드러내는 방식: 한숨을 쉬며 혼잣말처럼");
    }
}
