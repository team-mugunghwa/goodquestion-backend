package com.mugunghwa.goodquestion.ai.tts;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClientRequest;
import tools.jackson.databind.JsonNode;

import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gemini 음성 합성 (gemini-2.5-flash-preview-tts).
 *
 * <p><b>사전 렌더 자산과 화자를 맞추려고 만들었다.</b> 장면 음성 13개와 선택지 음성 52개는
 * Gemini로 렌더돼 있는데(며느리 Leda · 시아버지 Puck · 이장 Charon) 런타임 합성만 OpenAI
 * (nova/onyx/echo)라, {@code scene_audio}를 연결하는 순간 한 장면 안에서 오프닝은 Gemini,
 * 중간 대사는 OpenAI로 갈린다. 중간 대사는 LLM이 그때그때 만들어 사전 렌더가 원리상
 * 불가능하므로, <b>두 경로가 같은 엔진·같은 보이스여야만</b> 같은 캐릭터로 들린다.
 *
 * <p>보이스 이름만으로는 성별이 정해지지 않는다 — 같은 Puck이 지시문에 따라 244Hz(여성)로도
 * 128Hz(남성)로도 나온다(2026-08-08 F0 실측). 그래서 {@code instructions}에 <b>성별과 연령을
 * 반드시 넣는다.</b> 이 값은 사전 렌더에 쓴 것과 같은 문구를 설정으로 준다.
 *
 * <p>보이스 맵은 {@code external.tts.gemini} 아래에 따로 둔다. OpenAI와 이름 체계가 달라
 * (nova/onyx/echo vs Leda/Puck/Charon) 한 맵을 공유할 수 없다 — 공유하면 벤더만 바꿨을 때
 * Gemini가 "nova"를 보이스 이름으로 받는다. 벤더 전환은 {@code external.tts.vendor}로만 한다.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "external.tts.vendor", havingValue = "gemini")
public class GeminiTtsClient implements TtsClient {

    /** 응답은 헤더 없는 PCM이라 WAV 헤더를 직접 붙인다. Gemini가 이 규격으로 준다. */
    private static final int SAMPLE_RATE = 24_000;
    private static final int CHANNELS = 1;
    private static final int BITS_PER_SAMPLE = 16;

    /** 같은 (보이스, 문장) 재합성을 막는다. 고정 대사가 20개뿐이라 적중률이 높다. */
    private static final int CACHE_MAX_ENTRIES = 512;

    private final WebClient webClient;
    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final GeminiVoiceProperties voices;
    private final Duration timeout;
    private final ConcurrentHashMap<String, SynthesizedAudio> cache = new ConcurrentHashMap<>();

    public GeminiTtsClient(
            WebClient webClient,
            @Value("${external.tts.gemini.base-url:https://generativelanguage.googleapis.com/v1beta}") String baseUrl,
            @Value("${external.tts.gemini.api-key}") String apiKey,
            @Value("${external.tts.gemini.model:gemini-2.5-flash-preview-tts}") String model,
            @Value("${external.tts.timeout-ms:30000}") long timeoutMs,
            GeminiVoiceProperties voices) {
        this.webClient = webClient;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.model = model;
        this.voices = voices;
        this.timeout = Duration.ofMillis(timeoutMs);
    }

    @Override
    public SynthesizedAudio synthesize(String text, String characterName) {
        String key = voices.voiceFor(characterName) + "" + text;
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
        // 연기 지시를 본문 앞에 붙인다 — Gemini TTS는 별도 파라미터가 아니라 프롬프트로 받는다.
        String prompt = voices.instructionsFor(characterName) + "\n\n" + text;

        JsonNode response = webClient.post()
                // 키는 쿼리스트링이 아니라 헤더로 보낸다. URL은 요청 로깅 필터(WebClientConfig
                // logRequest)와 WebClient 예외 메시지에 그대로 실리므로, 쿼리에 실으면 벤더
                // 장애가 나는 순간 키가 ERROR 로그로 샌다.
                .uri(baseUrl + "/models/" + model + ":generateContent")
                .header("x-goog-api-key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                // 긴 내레이션은 공용 10초를 넘길 수 있다. 대화 턴은 짧아야 하므로 여기만 늘린다.
                .httpRequest(request -> {
                    HttpClientRequest nettyRequest = request.getNativeRequest();
                    nettyRequest.responseTimeout(timeout);
                })
                .bodyValue(Map.of(
                        "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                        "generationConfig", Map.of(
                                "responseModalities", List.of("AUDIO"),
                                "speechConfig", Map.of("voiceConfig", Map.of(
                                        "prebuiltVoiceConfig", Map.of(
                                                "voiceName", voices.voiceFor(characterName)))))))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        if (response == null) {
            throw new IllegalStateException("TTS 응답이 비어 있습니다");
        }
        String encoded = response.path("candidates").path(0).path("content")
                .path("parts").path(0).path("inlineData").path("data").asText(null);
        if (encoded == null || encoded.isBlank()) {
            // 안전 거부면 오디오 대신 finishReason이 온다. 아이 발화가 섞일 수 있어 원문은 안 남긴다.
            throw new IllegalStateException("TTS가 오디오를 반환하지 않았습니다: "
                    + response.path("candidates").path(0).path("finishReason").asText("원인 미상"));
        }

        byte[] wav = toWav(Base64.getDecoder().decode(encoded));
        return new SynthesizedAudio(
                "data:audio/wav;base64," + Base64.getEncoder().encodeToString(wav), null);
    }

    /**
     * 헤더 없는 16bit LE mono PCM에 44바이트 WAV 헤더를 붙인다.
     *
     * <p>OpenAI 구현은 mp3를 그대로 받아 data URL로 내보내지만 Gemini는 raw PCM으로 준다.
     * 헤더 없이 내보내면 브라우저가 재생하지 못한다 — 서버에서 붙여야 클라이언트가
     * 벤더 차이를 몰라도 된다. mp3로 줄이려면 ffmpeg 의존이 생기므로 여기서는 WAV로 둔다
     * (문장 단위라 수백 KB 수준이고, 응답 디코드 상한 16MB 안에 충분히 들어간다).
     */
    private static byte[] toWav(byte[] pcm) {
        int byteRate = SAMPLE_RATE * CHANNELS * BITS_PER_SAMPLE / 8;
        int blockAlign = CHANNELS * BITS_PER_SAMPLE / 8;
        ByteArrayOutputStream out = new ByteArrayOutputStream(44 + pcm.length);
        writeAscii(out, "RIFF");
        writeIntLe(out, 36 + pcm.length);
        writeAscii(out, "WAVE");
        writeAscii(out, "fmt ");
        writeIntLe(out, 16);              // fmt 청크 크기
        writeShortLe(out, 1);             // PCM
        writeShortLe(out, CHANNELS);
        writeIntLe(out, SAMPLE_RATE);
        writeIntLe(out, byteRate);
        writeShortLe(out, blockAlign);
        writeShortLe(out, BITS_PER_SAMPLE);
        writeAscii(out, "data");
        writeIntLe(out, pcm.length);
        out.writeBytes(pcm);
        return out.toByteArray();
    }

    private static void writeAscii(ByteArrayOutputStream out, String text) {
        for (int i = 0; i < text.length(); i++) {
            out.write(text.charAt(i));
        }
    }

    private static void writeIntLe(ByteArrayOutputStream out, int value) {
        out.write(value & 0xFF);
        out.write((value >> 8) & 0xFF);
        out.write((value >> 16) & 0xFF);
        out.write((value >> 24) & 0xFF);
    }

    private static void writeShortLe(ByteArrayOutputStream out, int value) {
        out.write(value & 0xFF);
        out.write((value >> 8) & 0xFF);
    }

    /**
     * Gemini 전용 캐릭터명 -> 보이스·연기 지시 매핑.
     *
     * <p>api-key·model은 여기 두지 않는다. 이 레코드는 벤더와 무관하게 바인딩되는데,
     * 기본값 없는 {@code ${GEMINI_API_KEY}}를 컴포넌트로 넣으면 OpenAI로 돌리는 환경에서도
     * 플레이스홀더를 풀지 못해 앱이 뜨지 않는다. 키는 조건부 빈의 {@code @Value}로 받는다.
     *
     * @param defaultVoice        매핑에 없는 캐릭터와 내레이션의 보이스
     * @param defaultInstructions 공통 연기 지시. 캐릭터별 지시가 있으면 그것을 쓴다
     */
    @ConfigurationProperties(prefix = "external.tts.gemini")
    public record GeminiVoiceProperties(String defaultVoice, String defaultInstructions,
                                        Map<String, String> voices,
                                        Map<String, String> instructions) {

        public GeminiVoiceProperties {
            // 사전 렌더 내레이션에 쓴 보이스다. 바꾸면 내레이션과 실시간 합성이 갈린다.
            if (defaultVoice == null || defaultVoice.isBlank()) defaultVoice = "Kore";
            if (defaultInstructions == null || defaultInstructions.isBlank()) {
                defaultInstructions = "한국 전래동화를 5~9세 아이에게 들려주듯 따뜻하고 또렷하게 말해줘:";
            }
            if (voices == null) voices = Map.of();
            if (instructions == null) instructions = Map.of();
        }

        String voiceFor(String characterName) {
            return characterName != null ? voices.getOrDefault(characterName, defaultVoice) : defaultVoice;
        }

        String instructionsFor(String characterName) {
            return characterName != null
                    ? instructions.getOrDefault(characterName, defaultInstructions) : defaultInstructions;
        }
    }
}
