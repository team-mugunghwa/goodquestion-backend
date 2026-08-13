package com.mugunghwa.goodquestion.ai.llm;

import com.mugunghwa.goodquestion.ai.analysis.AnalysisLlmClient;
import com.mugunghwa.goodquestion.ai.analysis.AnalysisPromptBuilder;
import com.mugunghwa.goodquestion.ai.character.CharacterLlmClient;
import com.mugunghwa.goodquestion.ai.character.CharacterPromptBuilder;
import com.mugunghwa.goodquestion.global.config.WebClientConfig;
import io.github.cdimascio.dotenv.Dotenv;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * gpt-5-mini 실호출 스모크 테스트.
 *
 * <p>.env의 LLM_API_KEY가 있을 때만 돈다 — CI나 키 없는 로컬에서는 조용히 건너뛴다.
 * mock으로는 확인할 수 없는 것들을 본다: 스키마 준수 여부, 한국어 아동 발화에 대한
 * 실제 판정, 그리고 응답 지연(타임아웃 10초가 맞는 값인지의 근거).
 *
 * <p>호출당 과금되므로 최소 횟수만 부른다.
 */
class LlmSmokeTest {

    private static String apiKey;

    @BeforeAll
    static void loadKey() {
        apiKey = Dotenv.configure().ignoreIfMissing().load().get("LLM_API_KEY");
        assumeTrue(apiKey != null && !apiKey.isBlank(),
                ".env에 LLM_API_KEY가 없어 실호출 스모크 테스트를 건너뛴다");
    }

    private OpenAiLlmClient client() {
        return new OpenAiLlmClient(
                new WebClientConfig().webClient(3000, Duration.ofSeconds(20).toMillis()),
                new ObjectMapper(),
                "https://api.openai.com/v1", apiKey, "gpt-5-mini", "minimal");
    }

    @Test
    void 발화_분석이_스키마대로_한국어_발화를_판정한다() {
        AnalysisLlmClient analysisClient = new AnalysisLlmClient(
                client(), new AnalysisPromptBuilder(new ObjectMapper()));

        long startedAt = System.nanoTime();
        AnalysisLlmClient.AnalysisLlmResult result = analysisClient.analyze(
                new AnalysisLlmClient.AnalysisLlmInput(
                        "며느리가 방귀를 참아 몸이 힘들지만 가족들이 이상하게 볼까 봐 말하지 못하고 있다.",
                        "며느리의 상황을 이해하고 감정을 표현하며 해결 방법을 생각해 본다.",
                        "내 방귀가 너무 크다는 걸 알면 가족들이 나를 이상하게 생각하지 않을까?",
                        "참으면 배가 아프니까 가족들한테 솔직하게 말하는 게 좋을 것 같아요",
                        List.of("PERSPECTIVE", "EMOTION", "SOLUTION"),
                        Map.of("PERSPECTIVE", "며느리의 상황이나 걱정을 고려함",
                                "EMOTION", "며느리나 자신의 감정을 표현함",
                                "SOLUTION", "며느리가 할 수 있는 구체적인 행동을 제안함")));
        long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000;

        System.out.printf("분석 실호출: %dms, model=%s, intent=%s, validity=%s, elements=%s%n",
                elapsedMs, result.modelId(), result.childIntent(), result.utteranceValidity(),
                result.detectedElements().stream()
                        .map(e -> e.type() + ":" + e.evidence()).toList());

        // 스키마 준수는 구조화 출력이 보장하지만, 값이 실제 enum에 드는지는 여기서 본다.
        assertThat(result.childIntent()).isNotBlank();
        assertThat(result.utteranceValidity())
                .isIn("VALID", "SHORT", "UNCLEAR", "OFF_TOPIC", "PLAYFUL");
        // 해결 방법을 구체적으로 말한 발화다. SOLUTION이 잡혀야 정상 판정이다.
        assertThat(result.detectedElements())
                .anyMatch(element -> "SOLUTION".equals(element.type()));
    }

    @Test
    void 캐릭터_대사가_감정과_함께_생성된다() {
        CharacterLlmClient characterClient = new CharacterLlmClient(
                client(), new CharacterPromptBuilder());

        long startedAt = System.nanoTime();
        CharacterLlmClient.CharacterLlmResult result = characterClient.reply(
                new CharacterLlmClient.CharacterLlmInput(
                        "참으면 배가 아프니까 가족들한테 솔직하게 말하는 게 좋을 것 같아요",
                        "의도=SOLUTION / 핵심=솔직하게 말하자는 제안 / 확인된 요소=[SOLUTION] / 정보량=VALID",
                        "NORMAL",
                        "PROPOSAL_FROM_CHILD",
                        "방귀쟁이 며느리",
                        "남을 많이 의식해 조심스럽지만, 자신의 모습을 조금씩 받아들이는 따뜻한 인물. "
                                + "방귀를 참아 몸이 힘들지만 가족들이 이상하게 볼까 봐 말하지 못하고 있다.",
                        "그래도 가족들이 나를 이상하게 생각하면 어떡하지?",
                        true, null));
        long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000;

        System.out.printf("캐릭터 실호출: %dms, emotion=%s, text=%s%n",
                elapsedMs, result.emotion(), result.text());

        assertThat(result.text()).isNotBlank();
        assertThat(result.emotion())
                .isIn("NEUTRAL", "HAPPY", "SAD", "WORRIED", "SURPRISED", "RELIEVED");
    }
}
