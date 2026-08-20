package com.mugunghwa.goodquestion.user.auth;

import java.util.UUID;

/**
 * 보호자가 인증에 성공했다는 사실만 알린다. 가입·이메일 로그인·소셜 로그인이 모두 발행한다.
 *
 * <p>최초 로그인 별가루는 learning이 이 이벤트를 받아 지급한다 — user는 learning을
 * 의존할 수 없어 방향을 뒤집었다({@code ChildCreatedEvent}와 같은 방식).
 *
 * <p>토큰 재발급(refresh)은 발행하지 않는다. 재발급은 이미 로그인한 세션의 연장이고,
 * "최초 로그인"을 재발급으로 앞당길 이유가 없다.
 */
public record ParentLoggedInEvent(UUID parentId) {
}
