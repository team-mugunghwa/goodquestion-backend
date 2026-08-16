package com.mugunghwa.goodquestion.ai.word;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class WordMeaningPromptBuilderTest {

    private final WordMeaningPromptBuilder promptBuilder = new WordMeaningPromptBuilder();

    /// 길이 규칙은 글로만 적어 두면 모델이 글자를 못 세서 안 지켜진다
    /// (실서버 측정 11건 중 7건 초과, 최대 36자). 짧은 뜻 예시를 함께 준다.
    @Test
    void 시스템_프롬프트는_어절_기준_길이와_뜻_예시를_준다() {
        String prompt = promptBuilder.buildSystemPrompt();

        // 글자 수는 모델이 셀 수 없어 안 지켜졌다. 어절은 띄어쓰기 단위라 셀 수 있다.
        assertThat(prompt).contains("5~7덩어리(어절)");
        assertThat(prompt).contains("정확함을 해치면서까지 줄이지 않는다");
        assertThat(prompt).contains("마침표는 찍지 않는다");
        assertThat(prompt).contains("지붕을 덮는 납작한 흙 조각이에요");
    }

    /// 예시에 적힌 덩어리 수가 실제와 다르면 모델에게 잘못 세는 법을 가르친다.
    /// 예시를 손볼 때 표기도 함께 고치도록 고정한다.
    @Test
    void 뜻_예시에_적힌_덩어리_수가_실제_어절_수와_같다() {
        Matcher matcher = Pattern
                .compile("-> (\\S.*?)\\s+\\((\\d)덩어리\\)")
                .matcher(promptBuilder.buildSystemPrompt());

        int found = 0;
        while (matcher.find()) {
            String meaning = matcher.group(1).trim();
            int declared = Integer.parseInt(matcher.group(2));
            assertThat(meaning.split("\\s+")).as("예시 \"%s\"", meaning).hasSize(declared);
            assertThat(declared).isBetween(5, 7);
            found++;
        }
        assertThat(found).as("뜻 예시 개수").isGreaterThanOrEqualTo(3);
    }

    /// 유효성 관문이 실재하는 낱말을 막으면 아이가 궁금해한 말을 담지 못한다.
    /// 실서버에서 "아궁이"가 오거절된 사례가 있어 판정을 보수적으로 못 박는다.
    @Test
    void 시스템_프롬프트는_애매하면_실재_낱말로_보라고_지시한다() {
        String prompt = promptBuilder.buildSystemPrompt();

        assertThat(prompt).contains("확실히 존재하지 않는 말일 때만");
        assertThat(prompt).contains("판단이 애매하면 true");
        assertThat(prompt).contains("옛말");
    }

    /// 프롬프트 안의 JSON 예시가 실제 출력 스키마와 어긋나면 모델이 옛 필드명으로
    /// 답할 수 있다. 둘이 같은 필드를 말하는지 고정한다.
    @Test
    void 시스템_프롬프트의_JSON_예시가_출력_스키마와_같은_필드를_쓴다() {
        String prompt = promptBuilder.buildSystemPrompt();
        @SuppressWarnings("unchecked")
        List<String> required = (List<String>) promptBuilder.outputSchema().get("required");

        assertThat(required).allSatisfy(field -> assertThat(prompt).contains("\"" + field + "\""));
        assertThat(prompt).doesNotContain("\"exampleSentence\"");
    }

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
