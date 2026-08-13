package com.mugunghwa.goodquestion.story.dialogue;

import com.mugunghwa.goodquestion.story.session.dto.CharacterMessageResponse;
import com.mugunghwa.goodquestion.story.session.dto.SceneTransitionResponse;

/**
 * 캐릭터 대사를 반영한 트랜잭션이 남긴 것.
 *
 * @param transition 장면이 끝난 턴에만 값이 있다
 */
record TurnClosure(CharacterMessageResponse message, SceneTransitionResponse transition) {
}
