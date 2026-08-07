package com.mugunghwa.goodquestion.learning.activity.dto;

import java.util.List;

/** 오답이면 retellingKeywords = null (재시도) */
public record CardSubmitResponse(boolean correct, List<String> retellingKeywords) {
}
