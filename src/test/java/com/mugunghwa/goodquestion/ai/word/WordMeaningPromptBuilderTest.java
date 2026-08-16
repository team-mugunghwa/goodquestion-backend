package com.mugunghwa.goodquestion.ai.word;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WordMeaningPromptBuilderTest {

    private final WordMeaningPromptBuilder promptBuilder = new WordMeaningPromptBuilder();

    @Test
    void 장면_설명이_있으면_단어와_장면_설명을_그대로_포함한다() {
        String prompt = promptBuilder.buildUserPrompt("도토리", "다람쥐가 도토리를 모으고 있다");

        assertThat(prompt).isEqualTo("단어: 도토리\n이야기 장면 설명: 다람쥐가 도토리를 모으고 있다");
    }

    @Test
    void 장면_설명이_null이면_장면_설명_없음으로_대체한다() {
        String prompt = promptBuilder.buildUserPrompt("도토리", null);

        assertThat(prompt).isEqualTo("단어: 도토리\n이야기 장면 설명: (장면 설명 없음)");
    }

    @Test
    void 장면_설명이_공백뿐이면_장면_설명_없음으로_대체한다() {
        String prompt = promptBuilder.buildUserPrompt("도토리", "   ");

        assertThat(prompt).isEqualTo("단어: 도토리\n이야기 장면 설명: (장면 설명 없음)");
    }

    @Test
    void 출력_스키마는_meaning과_예문을_필수로_요구하고_다른_필드를_허용하지_않는다() {
        Map<String, Object> schema = promptBuilder.outputSchema();

        @SuppressWarnings("unchecked")
        List<String> required = (List<String>) schema.get("required");
        assertThat(required).containsExactlyInAnyOrder("meaning", "exampleStory", "exampleDaily", "exampleAdvanced", "isRealWord");
        assertThat(schema.get("additionalProperties")).isEqualTo(false);
    }
}
