package com.mugunghwa.goodquestion.learning.reward.island.dto;

import java.util.UUID;

/** 명세 3-19 배치 아이템. */
public record PlacementResponse(
        UUID placementId,
        UUID childItemId,
        UUID itemId,
        String modelUrl,
        int gridX,
        int gridY
) {
}
