package com.mugunghwa.goodquestion.story.dialogue;

import com.mugunghwa.goodquestion.story.session.dto.CharacterMessageResponse;
import com.mugunghwa.goodquestion.story.session.dto.SceneTransitionResponse;

/**
 * 캐릭터 대사를 반영한 트랜잭션이 남긴 것.
 *
 * @param closingReaction 최대 턴 종료 턴의 짧은 반응. 마무리 대사(message) 앞에 재생된다.
 *                        그 외에는 null
 * @param transition      장면이 끝난 턴에만 값이 있다
 */
record TurnClosure(CharacterMessageResponse message, CharacterMessageResponse closingReaction,
                   SceneTransitionResponse transition) {
}
