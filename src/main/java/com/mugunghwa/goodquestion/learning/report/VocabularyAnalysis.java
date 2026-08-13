package com.mugunghwa.goodquestion.learning.report;

import java.util.List;

/**
 * 어휘 분석 (리포트 요건 3-1).
 *
 * <p>특징이 뚜렷하지 않아도 부정적으로 적지 않는다. 그때는 feedback에 다양한 어휘를
 * 권하는 문구가 들어간다 — 초1~2는 평가 대상이 아니라 말하기 경험을 쌓는 단계다.
 */
public record VocabularyAnalysis(List<String> mainWords,
                                 List<String> askedWords,
                                 List<String> repeatedExpressions,
                                 String feedback) {
}