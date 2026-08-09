package com.mugunghwa.goodquestion.learning.reward.stardust.dto;

import com.mugunghwa.goodquestion.learning.reward.stardust.StardustReason;

import java.time.OffsetDateTime;
import java.util.UUID;

/** 명세 3-16 별가루 거래. amount는 지급 +, 사용 −. */
public record StardustTransactionResponse(
        UUID transactionId,
        int amount,
        StardustReason reason,
        OffsetDateTime createdAt
) {
}
