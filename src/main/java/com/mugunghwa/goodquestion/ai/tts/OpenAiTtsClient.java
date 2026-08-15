package com.mugunghwa.goodquestion.ai.tts;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClientRequest;

import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OpenAI 음성 합성 (gpt-4o-mini-tts).
 *
 * <p>벤더 비교를 위한 실측 구현이다(미결-01). 캐릭터별 목소리는 설정으로 매핑한다 -
 * 캐릭터 메타(tts_voice)는 story 도메인에 있는데 ai 패키지는 도메인을 참조할 수 없어,
 * 벤더 확정 전까지는 설정 맵으로 잇는다.
 *
 * <p>오디오는 스토리지 없이 data URL로 돌려준다. 명세의 "바이트를 직접 내리지 않는다"는
 * 계약을 유지하면서(클라이언트는 URL을 그대로 재생·보관할 수 있다) 스토리지 선정을
 * 기다리지 않기 위한 것이다. 스토리지가 정해지면 이 구현체만 바꾼다. 만료 시각은 없다 -
 * data URL은 응답 자체가 오디오라 만료 개념이 없다.
 */
@Component
// 벤더가 둘이 되면서 스위치가 필요해졌다. 미설정이면 지금까지처럼 OpenAI를 쓴다 —
// 설정 파일을 안 고친 환경(로컬·CI)에서 앱이 안 뜨는 일이 없어야 한다.
@ConditionalOnProperty(name = "external.tts.vendor", havingValue = "openai", matchIfMissing = true)
public class OpenAiTtsClient implements TtsClient {

    private final WebClient webClient;
    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final VoiceProperties voices;
    private final Duration timeout;

    public OpenAiTtsClient(WebClient webClient,
                           @Value("${external.llm.base-url:https://api.openai.com/v1}") String baseUrl,
                           @Value("${external.tts.api-key}") String apiKey,
                           @Value("${external.tts.model:gpt-4o-mini-tts}") String model,
                           @Value("${external.tts.timeout-ms:30000}") long timeoutMs,
                           VoiceProperties voices) {
        this.webClient = webClient;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.model = model;
        this.voices = voices;
        this.timeout = Duration.ofMillis(timeoutMs);
    }

    /**
     * 같은 (보이스, 문장) 합성 결과 캐시. 이야기 고정 대사가 20개뿐인데 재생마다
     * 벤더 과금 + 왕복 지연이 나가던 것을 막는다(08-15 감사). scene_audio 사전 렌더가
     * 연결되면 이 캐시는 자연히 한산해진다 — 그 전까지의 최소 방어층이다.
     * 상한을 두는 이유: 아이 이름이 들어간 문장 등 비고정 입력이 무한히 쌓이면 안 된다.
     */
    private static final int CACHE_MAX_ENTRIES = 512;
    private final ConcurrentHashMap<String, SynthesizedAudio> cache = new ConcurrentHashMap<>();

    @Override
    public SynthesizedAudio synthesize(String text, String characterName) {
        String key = voices.voiceFor(characterName) + " " + text;
        SynthesizedAudio cached = cache.get(key);
        if (cached != null) {
            return cached;
        }
        SynthesizedAudio fresh = callVendor(text, characterName);
        if (cache.size() >= CACHE_MAX_ENTRIES) {
            cache.clear(); // 단순 정책. LRU가 필요할 규모면 scene_audio를 연결할 때다
        }
        cache.put(key, fresh);
        return fresh;
    }

    private SynthesizedAudio callVendor(String text, String characterName) {
        byte[] audio = webClient.post()
                .uri(baseUrl + "/audio/speech")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                // 긴 내레이션 합성은 공용 10초를 넘길 수 있다. 대화 턴은 아이가 마이크
                // 앞에서 기다리는 시간이라 공용 값은 짧게 두고 여기만 늘린다.
                .httpRequest(request -> {
                    HttpClientRequest nettyRequest = request.getNativeRequest();
                    nettyRequest.responseTimeout(timeout);
                })
                .bodyValue(Map.of(
                        "model", model,
                        "voice", voices.voiceFor(characterName),
                        "input", text,
                        "instructions", voices.instructionsFor(characterName),
                        "response_format", "mp3"))
                .retrieve()
                .bodyToMono(byte[].class)
                .block();

        if (audio == null || audio.length == 0) {
            throw new IllegalStateException("TTS 응답이 비어 있습니다");
        }
        return new SynthesizedAudio(
                "data:audio/mp3;base64," + Base64.getEncoder().encodeToString(audio), null);
    }

    /**
     * 캐릭터명 -> 보이스·말투 매핑.
     *
     * @param defaultVoice        매핑에 없는 캐릭터와 내레이션(characterName=null)의 보이스
     * @param defaultInstructions 공통 말투 지시. 캐릭터별 지시가 있으면 그것을 쓴다
     */
    @ConfigurationProperties(prefix = "external.tts")
    public record VoiceProperties(String defaultVoice, String defaultInstructions,
                                  Map<String, String> voices, Map<String, String> instructions) {

        public VoiceProperties {
            if (defaultVoice == null || defaultVoice.isBlank()) defaultVoice = "nova";
            if (defaultInstructions == null || defaultInstructions.isBlank()) {
                defaultInstructions = "한국 전래동화를 5~9세 아이에게 들려주듯 따뜻하고 또렷하게 말한다.";
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
