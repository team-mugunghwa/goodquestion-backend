package com.mugunghwa.goodquestion.story.dialogue;

import com.mugunghwa.goodquestion.global.vocab.ThinkingElement;

/** 발화에서 확인된 사고 요소와 원문 근거. jsonb 배열로 직렬화. */
public record DetectedElement(ThinkingElement type, String evidence) {
}
