package com.mugunghwa.goodquestion.ai.speech;

import com.mugunghwa.goodquestion.ai.speech.dto.SynthesisRequest;
import com.mugunghwa.goodquestion.ai.speech.dto.TranscriptionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/speech")
@RequiredArgsConstructor
public class SpeechController {

    private final SpeechService speechService;

    @PostMapping(value = "/transcriptions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TranscriptionResponse transcribe(@RequestParam("audio") MultipartFile audio) {
        return speechService.transcribe(audio);
    }

    @PostMapping(value = "/syntheses", produces = "audio/mpeg")
    public byte[] synthesize(@RequestBody SynthesisRequest request) {
        return speechService.synthesize(request);
    }
}
