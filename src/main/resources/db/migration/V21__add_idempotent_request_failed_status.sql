-- ============================================================
-- V21: idempotent_requests에 FAILED 상태 추가
--
-- 실패한 요청의 키를 지우면 같은 키의 재시도가 "처음 보는 요청"이 되어 작업 전체를
-- 다시 실행한다. 작업이 원자적이면 옳다 - 아무것도 커밋되지 않았으니 다시 하면 된다.
--
-- 그런데 발화 처리는 일부러 트랜잭션 셋으로 쪼개져 있다(커넥션 점유를 줄이려고
-- LLM 호출을 트랜잭션 밖에 둔다). 아이 발화가 커밋된 뒤 캐릭터 대사 생성이 실패하면
-- 이미 반영된 것이 있는데 키만 사라진다. 그 상태로 재시도하면 같은 발화가 두 번
-- 저장되고 턴 수가 두 번 오르고 분석 LLM이 두 번 과금된다 - 멱등키가 막겠다고 한
-- 바로 그 피해다.
--
-- 그래서 실패를 지우지 않고 남길 수 있게 한다. 원자적이지 않은 엔드포인트는 FAILED로
-- 기록하고, 같은 키의 재시도에는 재실행 대신 "이전 시도가 실패했으니 상태를 확인하라"고
-- 답한다. 새 작업을 원하면 클라이언트가 새 키를 만든다.
--
-- 기존 행은 손대지 않는다. IN_PROGRESS와 COMPLETED의 의미는 그대로다.
-- ============================================================

-- 인라인 check 제약은 이름이 자동 생성된다(테이블명_컬럼명_check).
alter table idempotent_requests drop constraint if exists idempotent_requests_status_check;

alter table idempotent_requests
    add constraint idempotent_requests_status_check
    check (status in ('IN_PROGRESS', 'COMPLETED', 'FAILED'));

-- 실패 사유. 재시도한 클라이언트에게 그대로 내리지는 않고(벤더 메시지가 샐 수 있다)
-- 운영자가 로그와 대조할 때 쓴다.
alter table idempotent_requests add column if not exists failure_reason varchar(200);
