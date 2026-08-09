package com.mugunghwa.goodquestion.story.session.dto;

import com.mugunghwa.goodquestion.story.content.dto.SceneContentResponse;
import com.mugunghwa.goodquestion.story.session.PlayPhase;

/**
 * STORY 장면 재생 완료 -> 다음 장면 이동 결과(장면-05~10).
 *
 * <p>다음 장면이 DIALOGUE면 고정 첫 대사를 함께 저장·반환한다(캐릭터-14).
 * 마지막 장면이 STORY로 끝났다면 phase=POST_ACTIVITY이고 currentScene은 null이다.
 */
public record SceneAdvanceResponse(
        PlayPhase phase,
        SceneContentResponse currentScene,
        CharacterMessageResponse openingMessage
) {
}
