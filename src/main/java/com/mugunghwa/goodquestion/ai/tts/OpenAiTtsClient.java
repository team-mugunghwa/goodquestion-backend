package com.mugunghwa.goodquestion.ai.tts;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Base64;
import java.util.Map;

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
public class OpenAiTtsClient implements TtsClient {

    private final WebClient webClient;
    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final VoiceProperties voices;

    public OpenAiTtsClient(WebClient webClient,
                           @Value("${external.llm.base-url:https://api.openai.com/v1}") String baseUrl,
                           @Value("${external.tts.api-key}") String apiKey,
                           @Value("${external.tts.model:gpt-4o-mini-tts}") String model,
                           VoiceProperties voices) {
        this.webClient = webClient;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.model = model;
        this.voices = voices;
    }

    @Override
    public SynthesizedAudio synthesize(String text, String characterName) {
        byte[] audio = webClient.post()
                .uri(baseUrl + "/audio/speech")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
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
