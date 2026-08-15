package com.mugunghwa.goodquestion.ai.speech;

import com.mugunghwa.goodquestion.ai.speech.dto.SynthesisRequest;
import com.mugunghwa.goodquestion.ai.speech.dto.SynthesisResponse;
import com.mugunghwa.goodquestion.ai.tts.SynthesizedAudio;
import com.mugunghwa.goodquestion.ai.speech.dto.TranscriptionResponse;
import com.mugunghwa.goodquestion.global.error.BusinessException;
import com.mugunghwa.goodquestion.global.error.ErrorCode;
import com.mugunghwa.goodquestion.ai.stt.SttClient;
import com.mugunghwa.goodquestion.ai.stt.SttConfidencePolicy;
import com.mugunghwa.goodquestion.ai.stt.SttResult;
import com.mugunghwa.goodquestion.ai.tts.TtsClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class SpeechService {

    private final SttClient sttClient;
    private final SttConfidencePolicy confidencePolicy;
    private final TtsClient ttsClient;

    /**
     * 원본 음성은 저장하지 않는다. 빈 결과면 422.
     *
     * <p>저신뢰 판정을 응답에 싣는다 - 아이가 텍스트를 확인하고 제출하기 전이
     * "다시 말해 볼까?" 안내가 의미 있는 유일한 시점이다.
     */
    public TranscriptionResponse transcribe(MultipartFile audio) {
        SttResult result = sttClient.transcribe(audio);
        if (result.text() == null || result.text().isBlank()) {
            throw new BusinessException(ErrorCode.STT_EMPTY_TEXT);
        }
        return new TranscriptionResponse(result.text(), result.confidence(),
                confidencePolicy.isLow(result.confidence()));
    }

    public SynthesisResponse synthesize(SynthesisRequest request) {
        SynthesizedAudio audio = ttsClient.synthesize(request.text(), request.characterName());
        return new SynthesisResponse(audio.audioUrl(), audio.expiresAt());
    }
}
