package com.mugunghwa.goodquestion.story.session.dto;

/** 장면 첫 대사 재생(멱등) — 재호출이면 alreadyOpened=true. */
public record SceneOpeningResponse(CharacterMessageResponse message, boolean alreadyOpened) {
}
