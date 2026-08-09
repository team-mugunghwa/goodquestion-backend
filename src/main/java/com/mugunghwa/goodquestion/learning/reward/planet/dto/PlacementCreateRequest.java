package com.mugunghwa.goodquestion.learning.reward.planet.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * 보관함의 아이템을 빈 칸에 놓는다.
 * 좌표는 원점 기준 축좌표(q, r)라 음수가 유효하다 — 범위 검증은 하지 않는다.
 */
public record PlacementCreateRequest(
        @NotNull UUID childItemId,
        @NotNull Integer placedQ,
        @NotNull Integer placedR
) {
}
