package com.mugunghwa.goodquestion.learning.reward.island.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record PlacementMoveRequest(
        @NotNull @PositiveOrZero Integer gridX,
        @NotNull @PositiveOrZero Integer gridY
) {
}
