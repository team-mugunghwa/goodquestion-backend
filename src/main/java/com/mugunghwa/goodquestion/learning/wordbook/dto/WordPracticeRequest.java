package com.mugunghwa.goodquestion.learning.wordbook.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** @param spokenText /api/stt가 돌려준 인식 텍스트. 음성 자체는 보내지도 저장하지도 않는다. */
public record WordPracticeRequest(@NotBlank @Size(max = 500) String spokenText) {
}
