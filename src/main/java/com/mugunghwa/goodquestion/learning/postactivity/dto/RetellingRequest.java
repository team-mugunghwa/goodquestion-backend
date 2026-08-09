package com.mugunghwa.goodquestion.learning.postactivity.dto;

import jakarta.validation.constraints.NotBlank;

public record RetellingRequest(@NotBlank String text, String sttRawText) {
}
