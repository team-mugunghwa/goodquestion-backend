package com.mugunghwa.goodquestion.learning.reward.island.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record IslandRenameRequest(@NotBlank @Size(min = 1, max = 30) String name) {
}
