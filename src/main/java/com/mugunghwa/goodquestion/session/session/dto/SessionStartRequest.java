package com.mugunghwa.goodquestion.session.session.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SessionStartRequest(@NotNull UUID storyId) {
}
