package com.mugunghwa.goodquestion.user.auth;

/**
 * 로그인 실패 횟수 저장소. 현재 구현은 메모리 기반이며, DB 컬럼이 준비되면
 * 이 계약을 구현하는 영속 저장소로 교체한다.
 */
public interface LoginAttemptStore {

    void assertLoginAllowed(String email);

    void recordFailure(String email);

    void reset(String email);
}
