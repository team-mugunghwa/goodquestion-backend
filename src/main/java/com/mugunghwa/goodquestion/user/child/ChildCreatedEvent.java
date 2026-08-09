package com.mugunghwa.goodquestion.user.child;

import java.util.UUID;

/**
 * 아이 프로필이 만들어졌다는 사실만 알린다.
 *
 * <p>아이에게는 행성과 별가루 지갑이 각각 1개씩 딸려야 하지만(계정-14),
 * user는 learning을 의존할 수 없어 직접 만들 수 없다 — 방향을 이벤트로 뒤집는다.
 * 동기 리스너라 아이 생성과 같은 트랜잭션에서 처리된다.
 */
public record ChildCreatedEvent(UUID childId) {
}
