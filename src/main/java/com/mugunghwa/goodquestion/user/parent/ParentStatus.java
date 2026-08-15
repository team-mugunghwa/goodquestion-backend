package com.mugunghwa.goodquestion.user.parent;

/**
 * 보호자 계정 상태.
 *
 * <p>값을 바꾸는 것은 관리자 콘솔이고 서비스는 읽기만 한다. 그래도 enum을 여기 두는
 * 이유는 로그인 거부가 서비스 쪽 규칙이기 때문이다 - 관리자가 상태를 바꿔도 여기서
 * 보지 않으면 정지가 아무 일도 하지 않는다.
 */
public enum ParentStatus {
    ACTIVE,
    /** 관리자가 막은 계정. 로그인이 거부된다. 데이터는 그대로 남는다. */
    SUSPENDED
}
