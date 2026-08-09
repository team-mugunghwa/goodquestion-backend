package com.mugunghwa.goodquestion.learning.reward.planet.dto;

import java.util.UUID;

/** 명세 3-19 배치 아이템. 좌표는 프론트와 같은 축좌표(q, r). */
public record PlacementResponse(
        UUID placementId,
        UUID childItemId,
        UUID itemId,
        String modelUrl,
        int placedQ,
        int placedR
) {
}
