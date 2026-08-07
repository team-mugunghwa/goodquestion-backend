package com.mugunghwa.goodquestion.learning.activity.dto;

import java.util.List;

public record ActivityStartResponse(List<Card> cards, short attemptCount) {
    public record Card(String id, String text) {}
}
