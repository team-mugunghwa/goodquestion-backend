package com.mugunghwa.goodquestion.ai.tts;

import com.mugunghwa.goodquestion.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TTS 벤더 전환 검증.
 *
 * <p>구현체가 둘이고 {@code @ConditionalOnProperty}로 갈린다. 조건이 어긋나면 둘 다 뜨거나
 * 둘 다 안 떠서 주입이 깨지는데, <b>기동 실패로 드러나므로</b> 배포에서 발견하면 늦다.
 *
 * <p>기본값(openai)은 전체 테스트가 그 설정으로 도니까 따로 보지 않는다. 여기서는 켰을 때
 * 실제로 갈리는지와, 보이스 맵이 Gemini 것으로 바뀌는지를 본다 — 맵을 공유하던 때는
 * 벤더만 바꾸면 Gemini가 "nova"를 보이스 이름으로 받았다.
 */
@IntegrationTest
@TestPropertySource(properties = {
        "external.tts.vendor=gemini",
        "external.tts.gemini.api-key=test-key-not-used"
})
class TtsVendorSwitchTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private TtsClient ttsClient;

    @Test
    void 벤더를_gemini로_두면_Gemini_구현만_뜬다() {
        assertThat(ttsClient).isInstanceOf(GeminiTtsClient.class);
        assertThat(context.getBeansOfType(TtsClient.class)).hasSize(1);
    }

    @Test
    void 캐릭터별_보이스가_사전_렌더와_같은_값이다() {
        GeminiTtsClient.GeminiVoiceProperties voices =
                context.getBean(GeminiTtsClient.GeminiVoiceProperties.class);

        assertThat(voices.voiceFor("방귀쟁이 며느리")).isEqualTo("Leda");
        assertThat(voices.voiceFor("시아버지")).isEqualTo("Puck");
        assertThat(voices.voiceFor("마을 이장")).isEqualTo("Charon");
        // 내레이션(캐릭터 없음)은 기본 보이스 - 사전 렌더 내레이션도 Kore 로 만들었다
        assertThat(voices.voiceFor(null)).isEqualTo("Kore");
    }

    /** 보이스 이름은 성별을 보장하지 않는다. 지시문에 성별·연령이 없으면 같은 보이스가 다른 사람이 된다. */
    @Test
    void 연기_지시에_성별이_적혀_있다() {
        GeminiTtsClient.GeminiVoiceProperties voices =
                context.getBean(GeminiTtsClient.GeminiVoiceProperties.class);

        assertThat(voices.instructionsFor("방귀쟁이 며느리")).contains("여자");
        assertThat(voices.instructionsFor("시아버지")).contains("남자");
        assertThat(voices.instructionsFor("마을 이장")).contains("남자");
    }
}
