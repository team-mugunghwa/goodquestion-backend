package com.mugunghwa.goodquestion.ai.speech.dto;

import java.math.BigDecimal;

/**
 * STT 변환 응답.
 *
 * @param text          변환 텍스트 - 이야기 어휘 근접 오인식 교정(VocabularyCorrector)이
 *                      끝난 상태. 화면 표시와 판정 모두 이 값을 쓴다
 * @param rawText       벤더가 돌려준 원문. 교정이 틀렸을 때 무엇이 실제로 인식됐는지
 *                      추적하는 유일한 근거다. 클라이언트는 발화 제출의 sttRawText에
 *                      <b>이 값을</b> 되올린다 - text를 되올리면 원문이 유실된다
 * @param confidence    신뢰도 0~1 (exp(토큰 logprob 평균)). 벤더가 못 주면 null.
 *                      클라이언트는 이 값을 발화 제출(sttConfidence)에 그대로 되올린다
 * @param lowConfidence 기준값(0.5) 미만 여부 - 판정은 서버가 한다. true면 제출 전에
 *                      "잘 못 알아들었을 수 있어요" 다시 말하기 안내를 띄운다 (비차단)
 */
public record TranscriptionResponse(String text, String rawText, BigDecimal confidence,
                                    boolean lowConfidence) {
}
