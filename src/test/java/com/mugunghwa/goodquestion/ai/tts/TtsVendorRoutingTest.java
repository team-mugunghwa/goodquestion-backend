package com.mugunghwa.goodquestion.ai.tts;

import com.mugunghwa.goodquestion.global.error.BusinessException;
import com.mugunghwa.goodquestion.global.error.ErrorCode;
import com.mugunghwa.goodquestion.global.settings.AppSettingRepository;
import com.mugunghwa.goodquestion.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TTS 벤더 라우팅 — app_settings(tts.vendor)로 재배포 없이 전환한다.
 *
 * <p>전환 실수(키 없는 벤더 선택)는 폴백하지 않고 503으로 또렷하게 실패해야 한다.
 * 조용히 다른 벤더가 나가면 "전환했는데 목소리가 그대로"가 디버깅 미궁이 된다.
 */
@IntegrationTest
@Transactional
@TestPropertySource(properties = {
        // Gemini만 키를 주고 Chirp는 비워 둔다 - 가용/불가용 두 경로를 다 본다
        "external.tts.gemini.api-key=test-key-not-used",
        "external.tts.chirp.api-key=",
})
class TtsVendorRoutingTest {

    @Autowired
    private RoutingTtsClient router;

    @Autowired
    private AppSettingRepository settings;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    private void setVendor(String value) {
        jdbc.update("""
                insert into app_settings(key, value) values ('tts.vendor', ?)
                on conflict (key) do update set value = excluded.value
                """, value);
        // 테스트는 한 트랜잭션이라 JPA 1차 캐시가 JDBC 변경을 못 본다. 운영에서는
        // 합성마다 새 트랜잭션이라 문제가 없지만, 여기서는 캐시를 비워 준다.
        entityManager.clear();
    }

    @Test
    void 설정_행이_없으면_기본_벤더다() {
        assertThat(router.currentVendor()).isEqualTo(TtsVendor.OPENAI);
        assertThat(router.delegate(router.currentVendor())).isInstanceOf(OpenAiTtsClient.class);
    }

    @Test
    void 설정을_바꾸면_재기동_없이_그_벤더로_간다() {
        setVendor("GEMINI");
        assertThat(router.currentVendor()).isEqualTo(TtsVendor.GEMINI);
        assertThat(router.delegate(router.currentVendor())).isInstanceOf(GeminiTtsClient.class);

        setVendor("OPENAI");
        assertThat(router.currentVendor()).isEqualTo(TtsVendor.OPENAI);
    }

    @Test
    void 소문자_값도_받아준다() {
        setVendor("chirp3");
        assertThat(router.currentVendor()).isEqualTo(TtsVendor.CHIRP3);
    }

    @Test
    void 키_없는_벤더로_전환하면_폴백하지_않고_503이다() {
        setVendor("CHIRP3");
        assertThatThrownBy(() -> router.delegate(router.currentVendor()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AI_UNAVAILABLE);
    }

    @Test
    void 이상한_값이면_기본_벤더로_물러난다() {
        setVendor("KAKAO_TTS");
        assertThat(router.currentVendor()).isEqualTo(TtsVendor.OPENAI);
    }

    @Test
    void Chirp_보이스는_Gemini_페르소나에_접두사만_붙인다() {
        assertThat(ChirpTtsClient.chirpVoiceName("Leda")).isEqualTo("ko-KR-Chirp3-HD-Leda");
        assertThat(ChirpTtsClient.chirpVoiceName("Kore")).isEqualTo("ko-KR-Chirp3-HD-Kore");
    }
}
