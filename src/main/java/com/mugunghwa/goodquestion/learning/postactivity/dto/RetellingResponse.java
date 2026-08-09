package com.mugunghwa.goodquestion.learning.postactivity.dto;

import com.mugunghwa.goodquestion.learning.reward.stardust.dto.StardustTransactionResponse;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 재구성 발화 제출 = 세션 완료 + 별가루 지급(활동-09~10, 보상-04).
 *
 * <p>지급 결과를 이 응답에 담아야 하므로 세션 완료 이벤트는 같은 트랜잭션에서 동기로 처리한다.
 */
public record RetellingResponse(
        String sessionStatus,
        OffsetDateTime completedAt,
        Stardust stardust,
        List<UnlockedItem> unlockedItems
) {
    public record Stardust(int earned, List<StardustTransactionResponse> breakdown, int balance) {}

    /** 이번 완주로 해금된 아이템 — 완료 화면 안내용. */
    public record UnlockedItem(UUID itemId, String name, String thumbnailUrl) {}
}
