package com.mugunghwa.goodquestion.ai.speech;

import com.mugunghwa.goodquestion.ai.speech.dto.SynthesisRequest;
import com.mugunghwa.goodquestion.ai.speech.dto.TranscriptionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class SpeechController {

    private final SpeechService speechService;

    /** 음성 → 텍스트. 원본 음성은 저장하지 않고 즉시 폐기한다(음성-07). */
    @PostMapping(value = "/api/stt", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TranscriptionResponse transcribe(@RequestParam("audio") MultipartFile audio) {
        return speechService.transcribe(audio);
    }

    /**
     * 텍스트 → 음성.
     * NOTE: 명세는 {@code {audioUrl, expiresAt}} JSON을 규정하지만 현재 구현은 audio/mpeg 바이트를
     * 그대로 반환한다. 어느 쪽으로 갈지는 팀 확인 필요 — 경로만 명세에 맞춰 두었다.
     */
    @PostMapping(value = "/api/tts", produces = "audio/mpeg")
    public byte[] synthesize(@RequestBody SynthesisRequest request) {
        return speechService.synthesize(request);
    }
}
