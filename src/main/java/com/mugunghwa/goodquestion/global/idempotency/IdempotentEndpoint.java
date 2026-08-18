package com.mugunghwa.goodquestion.global.idempotency;

/**
 * 멱등키가 적용되는 엔드포인트. 중복 실행의 피해가 돈인 곳부터 적용한다.
 *
 * <p>{@code reexecutable}은 "실패한 요청을 같은 키로 다시 실행해도 되는가"다.
 * 작업이 한 트랜잭션 안에서 끝나면 실패 시 아무것도 남지 않으므로 다시 하면 되고,
 * 여러 트랜잭션에 걸쳐 있으면 앞부분이 이미 커밋됐을 수 있어 다시 하면 중복이 된다.
 */
public enum IdempotentEndpoint {

    /**
     * 발화 제출 - 중복 처리 시 중복 턴 + LLM 요금 2배. scope는 세션.
     *
     * <p>재실행하지 않는다. 턴 처리는 트랜잭션 셋으로 쪼개져 있어(TurnTransactions)
     * 아이 발화가 커밋된 뒤 캐릭터 대사 생성이 실패할 수 있다. 그 상태로 다시 실행하면
     * 같은 발화가 두 번 저장된다. 실패는 기록으로 남기고 클라이언트는 turn-state로
     * 실제 상태를 확인한 뒤, 새 턴이 필요하면 새 키로 보낸다.
     */
    UTTERANCE(false),

    /**
     * 아이템 구매 - 중복 처리 시 별가루 이중 차감. scope는 아이.
     *
     * <p>재실행해도 된다. 해금 검증, 차감, 입고가 한 트랜잭션이라 실패하면 전부
     * 롤백된다. 정직한 재시도가 다시 실행될 수 있어야 한다.
     */
    ITEM_PURCHASE(true);

    private final boolean reexecutable;

    IdempotentEndpoint(boolean reexecutable) {
        this.reexecutable = reexecutable;
    }

    /** 실패한 요청을 같은 키로 다시 실행해도 되는가. */
    public boolean isReexecutable() {
        return reexecutable;
    }
}
