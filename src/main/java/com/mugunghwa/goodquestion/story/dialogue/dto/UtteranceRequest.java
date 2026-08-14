package com.mugunghwa.goodquestion.story.dialogue.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/**
 * 발화 제출.
 *
 * <p>STT 신뢰도는 /api/stt 응답의 confidence를 클라이언트가 그대로 되올린다 —
 * 서버가 messages에 보관하며, 기준값(0.5) 미만 판정(sttLowConfidence)은 저장 시
 * 서버가 한다. 클라이언트마다 기준이 갈리면 리포트 필터링이 흔들리기 때문이다.
 */
public record UtteranceRequest(
        @NotBlank String text,   // 확정 발화 텍스트
        String sttRawText,       // STT 최초 변환 텍스트 (선택)

        /** STT 신뢰도 0~1 (선택). 낮으면 리포트 대표 발화 후보에서 제외된다 */
        @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal sttConfidence,

        /** 아이가 다시 말한 횟수 (선택, 기본 0) */
        @PositiveOrZero Short sttRetryCount,

        String missionId         // 이 발화가 미션 수행 결과일 때 mission_config.mission_id (선택)
) {

    /** 선택 필드라 null이면 0으로 본다. */
    public short retryCountOrZero() {
        return sttRetryCount != null ? sttRetryCount : 0;
    }
}
