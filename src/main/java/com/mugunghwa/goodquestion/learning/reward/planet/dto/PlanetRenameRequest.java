package com.mugunghwa.goodquestion.learning.reward.planet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PlanetRenameRequest(@NotBlank @Size(min = 1, max = 30) String name) {
}
