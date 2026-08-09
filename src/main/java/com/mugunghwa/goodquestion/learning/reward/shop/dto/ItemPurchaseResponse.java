package com.mugunghwa.goodquestion.learning.reward.shop.dto;

/** 구매 결과 — 보관함에 입고된 아이템과 차감 후 잔액. */
public record ItemPurchaseResponse(ChildItemResponse item, int balance) {
}
