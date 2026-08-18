package com.mugunghwa.goodquestion.story.freetalk.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** 자유 대화 시작. 어느 이야기의 누구와 이야기할지만 받는다. */
public record FreeTalkStartRequest(@NotNull UUID storyId, @NotNull UUID characterId) {
}
