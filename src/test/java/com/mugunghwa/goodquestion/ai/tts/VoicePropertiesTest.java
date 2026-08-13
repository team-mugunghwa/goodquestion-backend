package com.mugunghwa.goodquestion.ai.tts;

import com.mugunghwa.goodquestion.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 캐릭터 보이스 매핑 바인딩 검증.
 *
 * <p>맵 키에 공백이 든 한글(캐릭터명)이 있어 yml에서 대괄호 표기를 쓴다. 표기가 틀리면
 * 조용히 기본 보이스로 떨어지는 게 아니라 바인딩이 실패해 앱이 뜨지 않는데, 실제로 그
 * 사고가 났었다. 여기서는 대괄호가 벗겨진 원형 키로 조회되는지를 본다 - 캐릭터명은
 * story_scenes.character_name 값 그대로 들어온다.
 */
@IntegrationTest
class VoicePropertiesTest {

    @Autowired
    private OpenAiTtsClient.VoiceProperties voices;

    @Test
    void 캐릭터명_원형으로_보이스가_조회된다() {
        assertThat(voices.voiceFor("방귀쟁이 며느리")).isEqualTo("nova");
        assertThat(voices.voiceFor("시아버지")).isEqualTo("onyx");
        assertThat(voices.voiceFor("마을 이장")).isEqualTo("echo");
    }

    @Test
    void 매핑에_없는_캐릭터와_내레이션은_기본_보이스다() {
        assertThat(voices.voiceFor("모르는 캐릭터")).isEqualTo("nova");
        assertThat(voices.voiceFor(null)).isEqualTo("nova");
    }

    @Test
    void 캐릭터별_말투_지시가_조회된다() {
        assertThat(voices.instructionsFor("시아버지")).contains("호들갑");
        assertThat(voices.instructionsFor(null)).isNotBlank();
    }
}
