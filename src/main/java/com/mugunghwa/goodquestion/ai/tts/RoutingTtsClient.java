package com.mugunghwa.goodquestion.ai.tts;

import com.mugunghwa.goodquestion.global.error.BusinessException;
import com.mugunghwa.goodquestion.global.error.ErrorCode;
import com.mugunghwa.goodquestion.global.settings.AppSettingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * 요청 시점에 벤더를 고르는 TTS 라우터.
 *
 * <p>원래 벤더는 {@code @ConditionalOnProperty}로 기동 시 하나만 떴다 — 바꾸려면 재배포였다.
 * Gemini 무료 등급 한도 때문에 테스트 기간에는 Chirp로 내렸다가 시연 때 올려야 하는데,
 * 그 전환을 관리자 콘솔에서 하려면 <b>매 합성마다</b> 설정을 읽어 갈라야 한다.
 *
 * <p>설정은 app_settings(tts.vendor)에서 읽는다. 관리자 콘솔이 그 행을 쓰고, 여기는
 * 읽기만 한다. 행이 없으면 환경변수 기본값({@code TTS_VENDOR}, 없으면 openai) — 관리자
 * 콘솔이 없는 로컬·CI에서도 지금과 똑같이 동작한다. 합성은 대화 턴당 한 번 수준이라
 * PK 조회 한 번은 캐시 없이도 값싸고, 캐시를 두면 전환이 "언제 먹은 건지"가 흐려진다.
 *
 * <p>알 수 없는 값·키 없는 벤더는 503(AI_UNAVAILABLE)으로 또렷하게 실패시킨다 —
 * 조용히 다른 벤더로 폴백하면 "전환했는데 목소리가 그대로"가 디버깅 미궁이 된다.
 */
@Slf4j
@Component
@Primary
public class RoutingTtsClient implements TtsClient {

    static final String SETTING_KEY = "tts.vendor";

    private final AppSettingRepository settings;
    private final OpenAiTtsClient openAi;
    private final GeminiTtsClient gemini;
    private final ChirpTtsClient chirp;
    private final TtsVendor defaultVendor;

    public RoutingTtsClient(AppSettingRepository settings, OpenAiTtsClient openAi,
                            GeminiTtsClient gemini, ChirpTtsClient chirp,
                            @Value("${external.tts.vendor:openai}") String defaultVendor) {
        this.settings = settings;
        this.openAi = openAi;
        this.gemini = gemini;
        this.chirp = chirp;
        this.defaultVendor = TtsVendor.valueOf(defaultVendor.trim().toUpperCase());
    }

    @Override
    public SynthesizedAudio synthesize(String text, String characterName) {
        return delegate(currentVendor()).synthesize(text, characterName);
    }

    /** 지금 설정된 벤더. 관리자 콘솔이 쓴 값이 이상하면 기본값으로 물러난다(합성이 멎으면 안 된다). */
    TtsVendor currentVendor() {
        return settings.findById(SETTING_KEY)
                .map(setting -> parse(setting.getValue()))
                .orElse(defaultVendor);
    }

    private TtsVendor parse(String value) {
        try {
            return TtsVendor.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("app_settings의 tts.vendor 값을 해석할 수 없어 기본값을 씁니다: {}", value);
            return defaultVendor;
        }
    }

    TtsClient delegate(TtsVendor vendor) {
        return switch (vendor) {
            case OPENAI -> openAi;
            case GEMINI -> requireAvailable(gemini.isAvailable(), vendor, gemini);
            case CHIRP3 -> requireAvailable(chirp.isAvailable(), vendor, chirp);
        };
    }

    private TtsClient requireAvailable(boolean available, TtsVendor vendor, TtsClient client) {
        if (!available) {
            // 키 없는 벤더로의 전환은 관리자 실수다. 다른 벤더로 조용히 폴백하지 않고
            // 무엇이 빠졌는지 그대로 말한다.
            throw new BusinessException(ErrorCode.AI_UNAVAILABLE,
                    "TTS 벤더 %s의 API 키가 설정되지 않았습니다".formatted(vendor));
        }
        return client;
    }
}
