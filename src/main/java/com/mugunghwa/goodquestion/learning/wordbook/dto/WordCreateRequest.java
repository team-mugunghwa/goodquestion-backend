package com.mugunghwa.goodquestion.learning.wordbook.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record WordCreateRequest(
        @NotBlank @Size(max = 50) String word,
        UUID sourceSceneId
) {
}
