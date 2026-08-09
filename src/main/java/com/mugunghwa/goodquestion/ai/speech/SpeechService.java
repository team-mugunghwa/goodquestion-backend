package com.mugunghwa.goodquestion.ai.speech;

import com.mugunghwa.goodquestion.ai.speech.dto.SynthesisRequest;
import com.mugunghwa.goodquestion.ai.speech.dto.SynthesisResponse;
import com.mugunghwa.goodquestion.ai.tts.SynthesizedAudio;
import com.mugunghwa.goodquestion.ai.speech.dto.TranscriptionResponse;
import com.mugunghwa.goodquestion.global.error.BusinessException;
import com.mugunghwa.goodquestion.global.error.ErrorCode;
import com.mugunghwa.goodquestion.ai.stt.SttClient;
import com.mugunghwa.goodquestion.ai.tts.TtsClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class SpeechService {

    private final SttClient sttClient;
    private final TtsClient ttsClient;

    /** 원본 음성은 저장하지 않는다. 빈 결과면 422. */
    public TranscriptionResponse transcribe(MultipartFile audio) {
        String text = sttClient.transcribe(audio);
        if (text == null || text.isBlank()) {
            throw new BusinessException(ErrorCode.STT_EMPTY_TEXT);
        }
        return new TranscriptionResponse(text);
    }

    public SynthesisResponse synthesize(SynthesisRequest request) {
        SynthesizedAudio audio = ttsClient.synthesize(request.text(), request.characterName());
        return new SynthesisResponse(audio.audioUrl(), audio.expiresAt());
    }
}
