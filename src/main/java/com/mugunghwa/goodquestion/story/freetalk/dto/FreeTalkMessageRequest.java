package com.mugunghwa.goodquestion.story.freetalk.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 아이의 말 한 줄. 음성 인식은 클라이언트가 /api/stt로 하고 여기는 텍스트만 받는다 -
 * 아이 음성 원본은 서버에 저장하지 않는다.
 */
public record FreeTalkMessageRequest(@NotBlank String text) {
}
