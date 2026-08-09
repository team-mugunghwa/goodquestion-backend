package com.mugunghwa.goodquestion.learning.reward.planet.dto;

import java.util.List;
import java.util.UUID;

/**
 * 내 행성 조회 — 배치 상태·진행 표시(보상-15~16, 21~23).
 * 판 크기·모양은 클라이언트 카탈로그가 단일 소스라 응답에 담지 않는다.
 */
public record PlanetResponse(
        UUID planetId,
        String name,
        boolean tutorialCompleted,
        List<PlacementResponse> placedItems,
        Progress progress
) {
    /** nextUnlock은 모두 해금된 경우 null. */
    public record Progress(int placedCount, NextUnlock nextUnlock) {}

    public record NextUnlock(String itemName, String thumbnailUrl, String conditionText) {}
}
