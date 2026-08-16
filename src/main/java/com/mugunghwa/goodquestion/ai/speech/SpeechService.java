package com.mugunghwa.goodquestion.ai.speech;

import com.mugunghwa.goodquestion.ai.speech.dto.SynthesisRequest;
import com.mugunghwa.goodquestion.ai.speech.dto.SynthesisResponse;
import com.mugunghwa.goodquestion.ai.tts.SynthesizedAudio;
import com.mugunghwa.goodquestion.ai.speech.dto.TranscriptionResponse;
import com.mugunghwa.goodquestion.global.error.BusinessException;
import com.mugunghwa.goodquestion.global.error.ErrorCode;
import com.mugunghwa.goodquestion.ai.stt.SttClient;
import com.mugunghwa.goodquestion.ai.stt.VocabularyCorrector;
import com.mugunghwa.goodquestion.ai.stt.VocabularyEchoDetector;
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
    private final VocabularyCorrector vocabularyCorrector;
    private final VocabularyEchoDetector echoDetector;
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
        // 무음·뭉개진 입력에서 모델이 학습 데이터의 영어 상투구("Thank you for
        // watching")를 뱉는 환각은 어휘 에코 판정도 저신뢰 컷도 빠져나간다 -
        // 한국어 아동 발화 서비스라 한글이 전혀 없는 결과는 인식 실패로 본다.
        if (!containsHangul(result.text())) {
            throw new BusinessException(ErrorCode.STT_EMPTY_TEXT);
        }
        // 이야기 어휘 근접 오인식 교정("방비"->"방귀"). 원문은 rawText로 보존한다.
        //
        // 저신뢰 턴에만 건다 - 또렷한 발화까지 걸면 자모 거리 2 안에 일상어가 잡힌다
        // ("방금"->"방귀", "바뀌었어요"->"방귀었어요" 오교정 실측). 실측 사고였던
        // "방비"는 뭉개진 발화라 저신뢰로 들어오고, 또렷한 발화는 교정 없이 나간다.
        // 신뢰도를 모르는(null) 턴도 걸지 않는다 - 근거 없이 바꾸지 않는다.
        boolean lowConfidence = confidencePolicy.isLow(result.confidence());
        String corrected = result.text();
        if (lowConfidence) {
            corrected = vocabularyCorrector.correct(result.text());
            // 뭉개진 에코("방기, 시아버지, ...")는 벤더 원문 기준 에코 판정을 통과한 뒤
            // 교정으로 정확한 힌트 단어가 될 수 있다 - 교정 후 텍스트로 한 번 더 판정한다.
            if (!corrected.equals(result.text()) && echoDetector.isEcho(corrected)) {
                throw new BusinessException(ErrorCode.STT_EMPTY_TEXT);
            }
        }
        return new TranscriptionResponse(corrected, result.text(), result.confidence(),
                lowConfidence);
    }

    private static boolean containsHangul(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= 0xAC00 && c <= 0xD7A3) {
                return true;
            }
        }
        return false;
    }

    public SynthesisResponse synthesize(SynthesisRequest request) {
        SynthesizedAudio audio = ttsClient.synthesize(request.text(), request.characterName());
        return new SynthesisResponse(audio.audioUrl(), audio.expiresAt());
    }
}
