package com.mugunghwa.goodquestion.learning.reward.island.dto;

import java.util.List;
import java.util.UUID;

/** 내 섬 조회 — 격자·배치 상태·진행 표시(보상-15~16, 21~23). */
public record IslandResponse(
        UUID islandId,
        String name,
        int gridWidth,
        int gridHeight,
        boolean tutorialCompleted,
        List<PlacementResponse> placedItems,
        Progress progress
) {
    /** nextUnlock은 모두 해금된 경우 null. */
    public record Progress(int placedCount, NextUnlock nextUnlock) {}

    public record NextUnlock(String itemName, String thumbnailUrl, String conditionText) {}
}
