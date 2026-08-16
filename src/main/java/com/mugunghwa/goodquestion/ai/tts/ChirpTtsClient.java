package com.mugunghwa.goodquestion.ai.tts;

import com.mugunghwa.goodquestion.ai.tts.GeminiTtsClient.GeminiVoiceProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClientRequest;
import tools.jackson.databind.JsonNode;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Google Cloud TTS Chirp 3: HD — 테스트 기간용 무료 벤더.
 *
 * <p>Gemini 무료 등급은 분당 호출 한도가 빠듯해 테스트 중 크레딧이 녹는다. Chirp 3: HD는
 * <b>월 100만 자 무료</b>(나레이션 한 편이 650자 남짓)라 사실상 무제한이고, 보이스 페르소나
 * 이름이 Gemini와 같아({@code ko-KR-Chirp3-HD-Leda} 등) 화자 느낌이 유지된다 —
 * 보이스 맵({@link GeminiVoiceProperties})을 그대로 재사용하고 접두사만 붙인다.
 *
 * <p>Gemini와 달리 연기 지시문(prompt)을 받지 않는다. 속도는 {@code speakingRate}로만
 * 조절한다 — 사전 렌더와 같은 0.95를 기본값으로 둔다.
 *
 * <p>키는 <b>Cloud 콘솔 키({@code GOOGLE_CLOUD_API_KEY})</b>다. AI Studio 키와 다르다 —
 * 둘 다 AIzaSy로 시작해 눈으로 구분되지 않는데, 서로 바꿔 넣으면 앱은 뜨고 합성만 403이다.
 */
@Slf4j
@Component
public class ChirpTtsClient implements TtsClient {

    /** ko-KR Chirp 3: HD 보이스 이름 접두사. 페르소나(Leda 등)는 Gemini 맵을 그대로 쓴다. */
    static final String VOICE_PREFIX = "ko-KR-Chirp3-HD-";

    private static final int CACHE_MAX_ENTRIES = 512;

    private final WebClient webClient;
    private final String baseUrl;
    private final String apiKey;
    private final double speakingRate;
    private final GeminiVoiceProperties voices;
    private final Duration timeout;
    private final ConcurrentHashMap<String, SynthesizedAudio> cache = new ConcurrentHashMap<>();

    public ChirpTtsClient(
            WebClient webClient,
            @Value("${external.tts.chirp.base-url:https://texttospeech.googleapis.com/v1}") String baseUrl,
            @Value("${external.tts.chirp.api-key:}") String apiKey,
            @Value("${external.tts.chirp.speaking-rate:0.95}") double speakingRate,
            @Value("${external.tts.timeout-ms:30000}") long timeoutMs,
            GeminiVoiceProperties voices) {
        this.webClient = webClient;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.speakingRate = speakingRate;
        this.voices = voices;
        this.timeout = Duration.ofMillis(timeoutMs);
    }

    /** 키가 없으면 라우터가 이 벤더를 고를 수 없다고 안내한다. */
    boolean isAvailable() {
        return apiKey != null && !apiKey.isBlank();
    }

    static String chirpVoiceName(String persona) {
        return VOICE_PREFIX + persona;
    }

    @Override
    public SynthesizedAudio synthesize(String text, String characterName) {
        String key = voices.voiceFor(characterName) + "" + text;
        SynthesizedAudio cached = cache.get(key);
        if (cached != null) {
            return cached;
        }
        SynthesizedAudio fresh = callVendor(text, characterName);
        if (cache.size() >= CACHE_MAX_ENTRIES) {
            cache.clear();
        }
        cache.put(key, fresh);
        return fresh;
    }

    private SynthesizedAudio callVendor(String text, String characterName) {
        JsonNode response = webClient.post()
                .uri(baseUrl + "/text:synthesize?key=" + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .httpRequest(request -> {
                    HttpClientRequest nettyRequest = request.getNativeRequest();
                    nettyRequest.responseTimeout(timeout);
                })
                .bodyValue(Map.of(
                        "input", Map.of("text", text),
                        "voice", Map.of(
                                "languageCode", "ko-KR",
                                "name", chirpVoiceName(voices.voiceFor(characterName))),
                        "audioConfig", Map.of(
                                "audioEncoding", "MP3",
                                "speakingRate", speakingRate)))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        if (response == null) {
            throw new IllegalStateException("TTS 응답이 비어 있습니다");
        }
        String encoded = response.path("audioContent").asText(null);
        if (encoded == null || encoded.isBlank()) {
            throw new IllegalStateException("TTS가 오디오를 반환하지 않았습니다");
        }
        // Cloud TTS는 완성된 mp3를 준다 - Gemini처럼 WAV 헤더를 붙일 필요가 없다.
        return new SynthesizedAudio("data:audio/mp3;base64," + encoded, null);
    }
}
