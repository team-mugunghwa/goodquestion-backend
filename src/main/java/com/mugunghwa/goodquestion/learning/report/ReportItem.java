package com.mugunghwa.goodquestion.learning.report;

import com.mugunghwa.goodquestion.global.vocab.ThinkingElement;

/** 리포트 강점/다음 연습 항목. jsonb 배열로 직렬화. */
public record ReportItem(ThinkingElement element, String comment) {
}
