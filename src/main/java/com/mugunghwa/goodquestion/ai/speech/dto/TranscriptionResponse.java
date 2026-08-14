package com.mugunghwa.goodquestion.ai.speech.dto;

import java.math.BigDecimal;

/**
 * STT 변환 응답.
 *
 * @param text          변환 텍스트
 * @param confidence    신뢰도 0~1 (exp(토큰 logprob 평균)). 벤더가 못 주면 null.
 *                      클라이언트는 이 값을 발화 제출(sttConfidence)에 그대로 되올린다
 * @param lowConfidence 기준값(0.5) 미만 여부 - 판정은 서버가 한다. true면 제출 전에
 *                      "잘 못 알아들었을 수 있어요" 다시 말하기 안내를 띄운다 (비차단)
 */
public record TranscriptionResponse(String text, BigDecimal confidence, boolean lowConfidence) {
}
