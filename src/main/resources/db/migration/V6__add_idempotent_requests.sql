-- 멱등 요청 기록 (멱등키 규약, 2026-08 확정)
--
-- 타임아웃 후 재시도(순차 중복)는 락으로 막을 수 없다 - 첫 요청의 트랜잭션이 이미
-- 끝나 조정할 대상이 없고, 재전송인지 새 작업인지는 클라이언트만 안다. 클라이언트가
-- Idempotency-Key 헤더로 그 구분을 실어 보내면 서버가 여기 기록해 중복 실행을 막는다.
--
-- 적용 대상: 발화 제출(UTTERANCE), 아이템 구매(ITEM_PURCHASE) - 중복 실행의 피해가
-- 돈(LLM 요금 2배 / 별가루 이중 차감)인 두 곳. 다른 엔드포인트는 필요 시 추가한다.
--
-- 동작: 첫 요청은 IN_PROGRESS로 선점 후 처리하고 완료 응답을 저장한다(COMPLETED).
-- 같은 키가 다시 오면 COMPLETED는 저장 응답 재생, IN_PROGRESS는 409. 작업이 실패하면
-- 행을 지워 정직한 재시도가 다시 실행되게 한다. 행은 24시간 뒤 청소 대상이다.

create table idempotent_requests (
    id              uuid primary key default gen_random_uuid(),
    endpoint        varchar(40) not null,
    -- 키의 유효 범위. 발화는 session_id, 구매는 child_id
    scope_id        uuid not null,
    -- 재생 시 소유권 검증용. 기록한 보호자와 다르면 재생하지 않는다
    parent_id       uuid not null,
    idempotency_key varchar(64) not null,
    status          varchar(20) not null check (status in ('IN_PROGRESS', 'COMPLETED')),
    -- 완료 응답 본문. 재생 시 이 값을 그대로 돌려준다
    response        jsonb,
    created_at      timestamptz not null default now(),
    completed_at    timestamptz,
    constraint uq_idempotent_requests unique (endpoint, scope_id, idempotency_key)
);

-- 24시간 경과분 청소용
create index idx_idempotent_requests_created_at on idempotent_requests(created_at);
