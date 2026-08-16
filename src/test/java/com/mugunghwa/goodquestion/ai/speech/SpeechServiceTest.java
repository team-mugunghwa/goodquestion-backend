package com.mugunghwa.goodquestion.ai.speech;

import com.mugunghwa.goodquestion.ai.speech.dto.TranscriptionResponse;
import com.mugunghwa.goodquestion.ai.stt.SttClient;
import com.mugunghwa.goodquestion.ai.stt.SttConfidencePolicy;
import com.mugunghwa.goodquestion.ai.stt.SttResult;
import com.mugunghwa.goodquestion.ai.stt.VocabularyCorrector;
import com.mugunghwa.goodquestion.ai.stt.VocabularyEchoDetector;
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
 *
 * <p>어휘 교정은 저신뢰 턴에만 걸어야 한다 - 또렷한 발화까지 걸면 자모 거리 안에
 * 일상어가 잡히는 오교정이 난다("방금"->"방귀", "바뀌었어요"->"방귀었어요" 실측).
 * 그 경계가 여기의 핵심 회귀 케이스다.
 */
class SpeechServiceTest {

    private static final String VOCAB = "며느리, 시아버지, 방귀, 친정, 갓, 이장, 배나무, 장대, 기왓장";
    private static final BigDecimal LOW = new BigDecimal("0.3");
    private static final BigDecimal HIGH = new BigDecimal("0.9");

    private static final MultipartFile AUDIO =
            new MockMultipartFile("audio", "a.wav", "audio/wav", new byte[] {1});

    private SpeechService serviceReturning(String vendorText, BigDecimal confidence) {
        SttClient stt = audio -> new SttResult(vendorText, confidence);
        TtsClient tts = (text, characterName) -> new SynthesizedAudio("data:audio/mp3;base64,", null);
        return new SpeechService(stt,
                new VocabularyCorrector(VOCAB),
                new VocabularyEchoDetector(VOCAB),
                new SttConfidencePolicy(new BigDecimal("0.5")), tts);
    }

    @Test
    void 저신뢰_턴은_교정본을_text로_원문을_rawText로_내려준다() {
        TranscriptionResponse response =
                serviceReturning("방비를 뀌었어요", LOW).transcribe(AUDIO);

        assertThat(response.text()).isEqualTo("방귀를 뀌었어요");
        assertThat(response.rawText()).isEqualTo("방비를 뀌었어요");
        assertThat(response.lowConfidence()).isTrue();
    }

    /** 실측 오교정 회귀 - 또렷한 발화의 일상어가 자모 거리 안에 잡혀도 건드리면 안 된다. */
    @Test
    void 또렷한_발화는_교정하지_않는다() {
        for (String utterance : new String[] {
                "방금 학교에서 왔어요", "생각이 바뀌었어요", "마음이 바뀌면 말해줘"}) {
            TranscriptionResponse response =
                    serviceReturning(utterance, HIGH).transcribe(AUDIO);

            assertThat(response.text()).isEqualTo(utterance);
            assertThat(response.rawText()).isEqualTo(utterance);
        }
    }

    /** 신뢰도를 모르는 턴도 교정하지 않는다 - 근거 없이 바꾸지 않는다. */
    @Test
    void 신뢰도가_없으면_교정하지_않는다() {
        TranscriptionResponse response =
                serviceReturning("방비를 뀌었어요", null).transcribe(AUDIO);

        assertThat(response.text()).isEqualTo("방비를 뀌었어요");
        assertThat(response.lowConfidence()).isFalse();
    }

    @Test
    void 교정할_것이_없으면_text와_rawText가_같다() {
        TranscriptionResponse response =
                serviceReturning("솔직하게 말해 보세요", LOW).transcribe(AUDIO);

        assertThat(response.text()).isEqualTo("솔직하게 말해 보세요");
        assertThat(response.rawText()).isEqualTo("솔직하게 말해 보세요");
    }

    /**
     * 뭉개진 에코는 원문 기준 에코 판정(벤더 클라이언트)을 빠져나간 뒤 교정으로
     * 정확한 힌트 단어가 될 수 있다 - 교정 후 텍스트로 한 번 더 판정해 막는다.
     */
    @Test
    void 교정_후_에코가_되는_결과는_인식_실패로_본다() {
        // 원문은 정확한 힌트 단어가 4개뿐이라 재조합 판정(9개 중 6개)을 통과하지만,
        // 교정이 방기->방귀, 시아버시->시아버지, 며느니->며느리, 배나부->배나무를
        // 복원하면 8개가 돼 에코가 된다. 이때 놓치면 안 된다.
        String garbledEcho = "방기 시아버시 며느니 친정 이장 배나부 장대 기왓장";

        assertThatThrownBy(() -> serviceReturning(garbledEcho, LOW).transcribe(AUDIO))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STT_EMPTY_TEXT);
    }

    /** 위 케이스의 전제 확인 - 뭉개진 원문은 원문 기준 에코 판정을 통과한다. */
    @Test
    void 뭉개진_에코_원문은_원문_기준_판정을_빠져나간다() {
        assertThat(new VocabularyEchoDetector(VOCAB)
                .isEcho("방기 시아버시 며느니 친정 이장 배나부 장대 기왓장")).isFalse();
    }

    /** 무음에서 모델이 학습 데이터의 영어 상투구를 뱉는 환각 — 에코 판정도 저신뢰 컷도 못 잡는다. */
    @Test
    void 한글이_전혀_없는_결과는_인식_실패로_본다() {
        assertThatThrownBy(() -> serviceReturning("Thank you for watching!", HIGH).transcribe(AUDIO))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STT_EMPTY_TEXT);
    }

    @Test
    void 한글이_섞여_있으면_통과한다() {
        assertThat(serviceReturning("OK 알겠어요", HIGH).transcribe(AUDIO).text())
                .isEqualTo("OK 알겠어요");
    }

    @Test
    void 빈_결과는_지금처럼_422다() {
        assertThatThrownBy(() -> serviceReturning("  ", HIGH).transcribe(AUDIO))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STT_EMPTY_TEXT);
    }
}
