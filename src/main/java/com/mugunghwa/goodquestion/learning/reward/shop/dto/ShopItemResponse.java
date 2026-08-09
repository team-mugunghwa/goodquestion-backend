package com.mugunghwa.goodquestion.learning.reward.shop.dto;

import com.mugunghwa.goodquestion.learning.reward.shop.ItemCategory;

import java.util.UUID;

/**
 * 명세 3-17 상점 아이템.
 * 해금·구매 가능·부족 수량은 전부 서버가 계산해 내려준다 — 프론트 판정 금지(보상-12).
 */
public record ShopItemResponse(
        UUID itemId,
        String name,
        ItemCategory category,
        int price,
        String modelUrl,
        String thumbnailUrl,
        boolean unlocked,
        boolean silhouette,
        UnlockGuide unlockGuide,
        boolean purchasable,
        int shortfall
) {
    /** 이야기 완주로 해금되는 아이템에만 값이 있다. */
    public record UnlockGuide(String storyTitle, String storyImageUrl) {}
}
