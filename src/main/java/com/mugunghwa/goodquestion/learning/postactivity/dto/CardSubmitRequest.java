package com.mugunghwa.goodquestion.learning.postactivity.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CardSubmitRequest(@NotEmpty List<String> submittedOrder) {
}
