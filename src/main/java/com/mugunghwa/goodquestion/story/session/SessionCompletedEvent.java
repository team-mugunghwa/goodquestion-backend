package com.mugunghwa.goodquestion.story.session;

import java.util.UUID;

/**
 * 후속 활동 없이 세션이 완료됐다는 사실을 알린다 (2026-08 확정).
 *
 * <p>post_activity_config가 없는 이야기는 마지막 장면이 끝나면 후속 활동을 건너뛰고
 * 즉시 완료된다 - 건너뛰지 않으면 카드 시작이 404를 던져 세션이 POST_ACTIVITY에
 * 갇힌다. 완주 별가루는 learning 소속이라 story가 직접 지급할 수 없어
 * {@link SceneClosedEvent}처럼 방향을 이벤트로 뒤집는다. 동기 리스너라 완료 처리와
 * 같은 트랜잭션에서 지급된다.
 *
 * <p>후속 활동이 있는 이야기의 완주 지급은 이 이벤트가 아니라 retelling 완료
 * (PostActivityService)에서 직접 일어난다 - 두 경로 모두 지급은 멱등이다.
 */
public record SessionCompletedEvent(UUID sessionId) {
}
