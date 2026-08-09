package com.mugunghwa.goodquestion.learning.reward.planet.dto;

import jakarta.validation.constraints.NotNull;

public record PlacementMoveRequest(
        @NotNull Integer placedQ,
        @NotNull Integer placedR
) {
}
