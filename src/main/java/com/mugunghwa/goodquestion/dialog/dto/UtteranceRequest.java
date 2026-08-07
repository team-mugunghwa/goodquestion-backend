package com.mugunghwa.goodquestion.dialog.dto;

import jakarta.validation.constraints.NotBlank;

public record UtteranceRequest(
        @NotBlank String text,   // 확정 발화 텍스트
        String sttRawText,       // STT 최초 변환 텍스트 (선택)
        String missionId         // 이 발화가 미션 수행 결과일 때 mission_config.mission_id (선택)
) {
}
