package com.mugunghwa.goodquestion.ai.speech;

import com.mugunghwa.goodquestion.ai.speech.dto.SynthesisRequest;
import com.mugunghwa.goodquestion.ai.speech.dto.SynthesisResponse;
import jakarta.validation.Valid;
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

    /** 텍스트 → 음성. 다시 듣기·단어 듣기가 공용으로 쓴다(장면-04, 단어-03). */
    @PostMapping("/api/tts")
    public SynthesisResponse synthesize(@Valid @RequestBody SynthesisRequest request) {
        return speechService.synthesize(request);
    }
}
