package com.mugunghwa.goodquestion.learning.reward.stardust.dto;

import java.util.List;

/**
 * 지갑 조회 응답.
 * unacknowledged가 비어 있지 않으면 섬 진입 시 별가루가 떨어지는 연출을 재생한다(보상-08).
 */
public record StardustWalletResponse(
        int balance,
        int totalEarned,
        List<StardustTransactionResponse> unacknowledged
) {
}
