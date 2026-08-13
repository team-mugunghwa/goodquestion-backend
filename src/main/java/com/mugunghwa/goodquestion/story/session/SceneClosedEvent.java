package com.mugunghwa.goodquestion.story.session;

import java.util.UUID;

/**
 * 대화 장면이 끝났다는 사실만 알린다.
 *
 * <p>유도 없이 목표를 통과한 장면에는 별가루 보너스가 붙지만(보상-04), story는 learning을
 * 의존할 수 없어 직접 지급할 수 없다 - 방향을 이벤트로 뒤집는다. 동기 리스너라 턴 처리와
 * 같은 트랜잭션에서 지급된다.
 *
 * <p>bonusEligible을 이벤트에 담아 보내는 이유: 자격은 장면 단위 누적 상태에서 나오는데
 * 다음 장면으로 이동하는 순간 그 상태가 초기화된다. 리스너가 나중에 세션을 다시 읽으면
 * 이미 지워진 값을 보게 된다.
 */
public record SceneClosedEvent(UUID sessionId, UUID sceneId, boolean bonusEligible) {
}
