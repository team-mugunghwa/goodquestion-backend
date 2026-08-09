package com.mugunghwa.goodquestion.learning.reward.shop.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ItemPurchaseRequest(@NotNull UUID itemId) {
}
