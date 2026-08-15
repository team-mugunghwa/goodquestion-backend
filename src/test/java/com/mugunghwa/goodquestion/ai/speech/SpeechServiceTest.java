package com.mugunghwa.goodquestion.ai.speech;

import com.mugunghwa.goodquestion.ai.speech.dto.TranscriptionResponse;
import com.mugunghwa.goodquestion.ai.stt.SttClient;
import com.mugunghwa.goodquestion.ai.stt.SttConfidencePolicy;
import com.mugunghwa.goodquestion.ai.stt.SttResult;
import com.mugunghwa.goodquestion.ai.stt.VocabularyCorrector;
import com.mugunghwa.goodquestion.ai.tts.SynthesizedAudio;
import com.mugunghwa.goodquestion.ai.tts.TtsClient;
import com.mugunghwa.goodquestion.global.error.BusinessException;
import com.mugunghwa.goodquestion.global.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * STT 응답 조립 — 교정·원문 보존·환각 필터.
 *
 * <p>실제 벤더를 부르는 스모크와 달리 여기서는 벤더를 손수 짠 가짜로 두고
 * 서비스 계층의 규칙만 본다(API 키 불필요).
 */
class SpeechServiceTest {

    private static final MultipartFile AUDIO =
            new MockMultipartFile("audio", "a.wav", "audio/wav", new byte[] {1});

    private SpeechService serviceReturning(String vendorText) {
        SttClient stt = audio -> new SttResult(vendorText, new BigDecimal("0.9"));
        TtsClient tts = (text, characterName) -> new SynthesizedAudio("data:audio/mp3;base64,", null);
        return new SpeechService(stt,
                new VocabularyCorrector("며느리, 시아버지, 방귀, 친정, 갓, 이장, 배나무, 장대, 기왓장"),
                new SttConfidencePolicy(new BigDecimal("0.5")), tts);
    }

    @Test
    void 교정본을_text로_원문을_rawText로_내려준다() {
        TranscriptionResponse response = serviceReturning("방비를 뀌었어요").transcribe(AUDIO);

        assertThat(response.text()).isEqualTo("방귀를 뀌었어요");
        assertThat(response.rawText()).isEqualTo("방비를 뀌었어요");
    }

    @Test
    void 교정할_것이_없으면_text와_rawText가_같다() {
        TranscriptionResponse response = serviceReturning("솔직하게 말해 보세요").transcribe(AUDIO);

        assertThat(response.text()).isEqualTo("솔직하게 말해 보세요");
        assertThat(response.rawText()).isEqualTo("솔직하게 말해 보세요");
    }

    /** 무음에서 모델이 학습 데이터의 영어 상투구를 뱉는 환각 — 에코 판정도 저신뢰 컷도 못 잡는다. */
    @Test
    void 한글이_전혀_없는_결과는_인식_실패로_본다() {
        assertThatThrownBy(() -> serviceReturning("Thank you for watching!").transcribe(AUDIO))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STT_EMPTY_TEXT);
    }

    @Test
    void 한글이_섞여_있으면_통과한다() {
        assertThat(serviceReturning("OK 알겠어요").transcribe(AUDIO).text())
                .isEqualTo("OK 알겠어요");
    }

    @Test
    void 빈_결과는_지금처럼_422다() {
        assertThatThrownBy(() -> serviceReturning("  ").transcribe(AUDIO))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STT_EMPTY_TEXT);
    }
}
