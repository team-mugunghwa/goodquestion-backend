package com.mugunghwa.goodquestion.ai.stt;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * STT 저신뢰 판정 (기준값 0.5 확정, 2026-08).
 *
 * <p>신뢰도는 exp(토큰 logprob 평균) 0~1이다. 기준값 미만이면 낮음으로 보고
 * 1) /api/stt 응답에 실어 제출 전 "잘 못 알아들었을 수 있어요" 안내에 쓰고,
 * 2) messages.stt_low_confidence로 남겨 리포트 대표 발화 후보에서 제외한다.
 * 분석(LLM) 제외에는 쓰지 않는다 - 발화 품질의 분석 단계 처리는
 * utteranceValidity(SHORT/UNCLEAR)와 유도가 담당한다.
 *
 * <p>0.5는 업계 표준인 Whisper 실패 컷(환산 약 0.37)보다 위의 품질 의심 수준을,
 * 아동 음성의 낮은 신뢰도 분포(오기각 비용)를 감안해 완화한 값이다. 아동 실녹음
 * 검증(미결-01) 후 조정할 수 있도록 설정으로 뺐다.
 *
 * <p>판정 주체는 서버다 - 클라이언트마다 기준이 갈리면 리포트 필터링이 흔들린다.
 */
@Component
public class SttConfidencePolicy {

    private final BigDecimal threshold;

    public SttConfidencePolicy(
            @Value("${external.stt.low-confidence-threshold:0.5}") BigDecimal threshold) {
        this.threshold = threshold;
    }

    /** 신뢰도가 없으면(null) 낮음으로 보지 않는다 - 판단 근거가 없는 것과 낮은 것은 다르다. */
    public boolean isLow(BigDecimal confidence) {
        return confidence != null && confidence.compareTo(threshold) < 0;
    }
}
