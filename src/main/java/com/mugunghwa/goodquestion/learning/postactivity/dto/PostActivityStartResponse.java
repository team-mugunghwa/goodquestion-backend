package com.mugunghwa.goodquestion.learning.postactivity.dto;

import java.util.List;

public record PostActivityStartResponse(List<Card> cards, short attemptCount) {
    public record Card(String id, String text) {}
}
