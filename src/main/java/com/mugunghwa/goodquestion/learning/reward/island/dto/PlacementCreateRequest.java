package com.mugunghwa.goodquestion.learning.reward.island.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.UUID;

/** 보관함의 아이템을 빈 칸에 놓는다. 범위 검증은 섬의 격자 크기 기준으로 서버가 수행한다. */
public record PlacementCreateRequest(
        @NotNull UUID childItemId,
        @NotNull @PositiveOrZero Integer gridX,
        @NotNull @PositiveOrZero Integer gridY
) {
}
