package com.mugunghwa.goodquestion.ai.speech;

import com.mugunghwa.goodquestion.ai.stt.OpenAiSttClient;
import com.mugunghwa.goodquestion.ai.stt.SttResult;
import com.mugunghwa.goodquestion.ai.tts.OpenAiTtsClient;
import com.mugunghwa.goodquestion.ai.tts.SynthesizedAudio;
import com.mugunghwa.goodquestion.global.config.WebClientConfig;
import io.github.cdimascio.dotenv.Dotenv;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * OpenAI STT/TTS 실호출 스모크 테스트. .env의 LLM_API_KEY가 있을 때만 돈다.
 *
 * <p>왕복 구조다 - TTS로 합성한 음성을 STT로 되읽어 원문과 비교한다. 오디오 픽스처 파일
 * 없이 두 방향을 한 번에 검증하고, 인식이 왕복을 못 버티면 실사용(아이 발화)은 더 어렵다는
 * 하한 신호로 읽는다.
 *
 * <p>주의: 이것은 성인 톤의 합성 음성이다. 미결-01의 "아동 한국어 인식률 검증"은 실제 아동
 * 녹음으로 따로 해야 하며, 이 테스트는 연동이 동작하는지와 지연만 본다.
 */
class SpeechSmokeTest {

    private static final String SENTENCE = "며느리가 방귀를 뀌어서 배가 떨어졌어요";

    private static String apiKey;

    @BeforeAll
    static void loadKey() {
        apiKey = Dotenv.configure().ignoreIfMissing().load().get("LLM_API_KEY");
        assumeTrue(apiKey != null && !apiKey.isBlank(),
                ".env에 LLM_API_KEY가 없어 실호출 스모크 테스트를 건너뛴다");
    }

    private WebClient webClient() {
        return new WebClientConfig().webClient(3000, Duration.ofSeconds(30).toMillis());
    }

    @Test
    void 합성한_음성을_되읽으면_원문이_나온다() {
        OpenAiTtsClient tts = new OpenAiTtsClient(webClient(), "https://api.openai.com/v1",
                apiKey, "gpt-4o-mini-tts",
                new OpenAiTtsClient.VoiceProperties(null, null, Map.of(), Map.of()));
        OpenAiSttClient stt = new OpenAiSttClient(webClient(), "https://api.openai.com/v1",
                apiKey, "gpt-4o-mini-transcribe",
                "며느리, 시아버지, 방귀, 친정, 갓, 이장, 배나무, 장대, 기왓장");

        long ttsStart = System.nanoTime();
        SynthesizedAudio audio = tts.synthesize(SENTENCE, null);
        long ttsMs = (System.nanoTime() - ttsStart) / 1_000_000;

        assertThat(audio.audioUrl()).startsWith("data:audio/mp3;base64,");
        byte[] mp3 = Base64.getDecoder()
                .decode(audio.audioUrl().substring("data:audio/mp3;base64,".length()));

        long sttStart = System.nanoTime();
        SttResult result = stt.transcribe(
                new MockMultipartFile("audio", "roundtrip.mp3", "audio/mpeg", mp3));
        long sttMs = (System.nanoTime() - sttStart) / 1_000_000;
        String text = result.text();

        String compact = text.replaceAll("\\s", "");
        long hit = java.util.stream.Stream.of("며느리", "방귀", "배가").filter(compact::contains).count();
        System.out.printf("TTS %dms (%dKB mp3) -> STT %dms, 핵심 단어 %d/3, 신뢰도 %s, 인식 결과: %s%n",
                ttsMs, mp3.length / 1024, sttMs, hit, result.confidence(), text);

        // include[]=logprobs를 요청했으므로 신뢰도가 계산돼야 한다. 벤더가 응답 형식을
        // 바꾸면 여기서 잡힌다 - 판정(0.5 컷)의 입력이 사라지는 회귀다.
        assertThat(result.confidence()).isNotNull();
        assertThat(result.confidence()).isBetween(java.math.BigDecimal.ZERO, java.math.BigDecimal.ONE);

        // 특정 단어 하나에 하드 검증을 걸지 않는다. TTS 발음이 매번 달라 왕복 실측에서
        // "방귀"가 "방비"로, "며느리"가 "와느리"로 각각 다른 실행에서 오인식됐다 - 어느
        // 단어든 흔들릴 수 있다. 핵심 단어 3개 중 2개 이상이면 연동이 동작하는 것으로 보고,
        // 실제 인식률 판정은 미결-01대로 아동 실녹음으로 한다.
        assertThat(text).isNotBlank();
        assertThat(hit).as("핵심 단어(며느리/방귀/배가) 인식 수. 결과: %s", text)
                .isGreaterThanOrEqualTo(2);
    }
}
