package com.mugunghwa.goodquestion.story.content;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 시드 걱정 문안(remaining_worries)의 금지선 정적 검증.
 *
 * <p>걱정 문안은 문서에 확정 문구가 없어 개발이 작성한 제안값이다(이야기_전개_가이드).
 * 사람 검수만으로는 나중 수정에서 금지선 이탈을 못 잡으므로, 연동 기준 13장(직접적인
 * 학습 질문 금지)·14장(정답·모범 답안 선점 금지)을 여기서 상시 감시한다.
 * DB 없이 시드 SQL 텍스트만 읽는다.
 */
class SeedWorryGuardTest {

    /** DIALOGUE 장면 하나: required_elements 배열 + element_criteria + remaining_worries. */
    private record DialogueSeed(Set<String> requiredElements, JsonNode criteria, JsonNode worries) {}

    private static final List<DialogueSeed> SCENES = new ArrayList<>();

    @BeforeAll
    static void loadSeed() throws IOException {
        String sql;
        try (InputStream in = SeedWorryGuardTest.class
                .getResourceAsStream("/db/migration/R__1_seed_content.sql")) {
            sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        // DIALOGUE 장면의 values 나열 순서: array[요소들], '{판정 기준}'::jsonb, '{걱정}'::jsonb.
        // jsonb 앞의 SQL 주석 줄은 건너뛴다.
        Pattern scene = Pattern.compile(
                "array\\[([^\\]]+)\\],\\s*(?:--[^\\n]*\\n\\s*)*'(\\{.*?\\})'::jsonb,\\s*(?:--[^\\n]*\\n\\s*)*'(\\{.*?\\})'::jsonb",
                Pattern.DOTALL);
        ObjectMapper mapper = new ObjectMapper();
        Matcher m = scene.matcher(sql);
        while (m.find()) {
            Set<String> elements = new HashSet<>();
            for (String raw : m.group(1).split(",")) {
                elements.add(raw.trim().replace("'", ""));
            }
            SCENES.add(new DialogueSeed(elements,
                    mapper.readTree(m.group(2)), mapper.readTree(m.group(3))));
        }
    }

    @Test
    void 대화_장면_4개의_걱정_블록이_전부_잡힌다() {
        assertThat(SCENES).hasSize(4);
    }

    @Test
    void 걱정_키셋은_필수_요소와_정확히_일치한다() {
        for (DialogueSeed seed : SCENES) {
            Set<String> worryKeys = new HashSet<>();
            seed.worries().properties().forEach(entry -> worryKeys.add(entry.getKey()));
            assertThat(worryKeys).isEqualTo(seed.requiredElements());
        }
    }

    @Test
    void 직접적인_학습_질문_표현을_쓰지_않는다() {
        for (DialogueSeed seed : SCENES) {
            seed.worries().properties().forEach(entry -> {
                String text = entry.getValue().asString();
                assertThat(text).doesNotContain("말해 봐", "말해봐", "무엇이 있을까");
            });
        }
    }

    @Test
    void 장면7_SOLUTION은_모범답안의_메커니즘을_선점하지_않는다() {
        // 충족 예가 "방귀로 배나무를 흔들어요"다. 걱정이 '방귀'나 '흔들-'을 먼저 말하면
        // 캐릭터가 정답 절반을 불러 주는 셈이 된다(연동 기준 14장).
        // REQUEST+SOLUTION 조합은 장면7뿐이다(장면9는 SOLUTION은 있지만 REQUEST가 없다).
        DialogueSeed scene7 = SCENES.stream()
                .filter(s -> s.requiredElements().contains("REQUEST")
                        && s.requiredElements().contains("SOLUTION"))
                .findFirst().orElseThrow();
        String solution = scene7.worries().get("SOLUTION").asString();
        assertThat(solution).doesNotContain("방귀", "흔들");
    }

    @Test
    void 문안은_음성으로_듣기_좋게_2문장을_넘지_않는다() {
        for (DialogueSeed seed : SCENES) {
            seed.worries().properties().forEach(entry -> {
                String text = entry.getValue().asString();
                long sentences = text.chars().filter(c -> c == '.' || c == '?' || c == '!').count();
                assertThat(sentences)
                        .as("걱정 문안이 3문장 이상: %s", text)
                        .isLessThanOrEqualTo(2);
            });
        }
    }
}
