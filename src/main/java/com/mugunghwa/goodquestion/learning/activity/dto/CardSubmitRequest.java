package com.mugunghwa.goodquestion.learning.activity.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CardSubmitRequest(@NotEmpty List<String> submittedOrder) {
}
