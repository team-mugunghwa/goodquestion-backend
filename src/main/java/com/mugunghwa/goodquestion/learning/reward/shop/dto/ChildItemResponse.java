package com.mugunghwa.goodquestion.learning.reward.shop.dto;

import com.mugunghwa.goodquestion.learning.reward.shop.ItemCategory;

import java.time.OffsetDateTime;
import java.util.UUID;

/** 명세 3-18 보유 아이템. placed=false면 보관함에 있다(보상-20). */
public record ChildItemResponse(
        UUID childItemId,
        UUID itemId,
        String name,
        ItemCategory category,
        String thumbnailUrl,
        String modelUrl,
        OffsetDateTime acquiredAt,
        boolean placed
) {
}
