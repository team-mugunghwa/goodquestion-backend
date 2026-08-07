package com.mugunghwa.goodquestion.dialog.analysis;

import com.mugunghwa.goodquestion.common.ThinkingElement;

/** 발화에서 확인된 사고 요소와 원문 근거. jsonb 배열로 직렬화. */
public record DetectedElement(ThinkingElement type, String evidence) {
}
