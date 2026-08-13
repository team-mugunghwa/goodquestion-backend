package com.mugunghwa.goodquestion.ai.stt;

import java.math.BigDecimal;

/**
 * STT 변환 결과.
 *
 * @param text       변환된 텍스트 (실패나 무음이면 null 또는 빈 문자열)
 * @param confidence 발화 신뢰도 0~1 (exp(토큰 logprob 평균)). 벤더가 logprob을
 *                   안 주면 null - 판정(SttConfidencePolicy)은 null을 낮음으로 보지 않는다
 */
public record SttResult(String text, BigDecimal confidence) {
}
