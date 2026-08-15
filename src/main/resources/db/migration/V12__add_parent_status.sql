-- ============================================================
-- 보호자 계정 상태
--
-- 관리자가 계정을 정지시킬 수 있어야 한다. 지금은 parents에 상태 컬럼이 없어서
-- "이 계정을 막아 달라"는 요청이 들어와도 손쓸 방법이 행 삭제뿐인데, 그건 아이의
-- 학습 기록과 리포트까지 함께 지우는 일이라 되돌릴 수 없다.
--
-- 이 컬럼을 보는 것은 서비스 쪽 로그인이다. 정지된 계정은 로그인이 거부된다.
-- admin-goodquestion-backend의 V2__add_parent_status.sql에 같은 내용이 들어 있다.
-- ============================================================

alter table parents add column if not exists status varchar(20) not null default 'ACTIVE';

-- 제약은 컬럼과 따로 건다. add column if not exists는 이미 컬럼이 있으면 통째로
-- 건너뛰므로, 인라인 check로 적으면 다른 쪽이 먼저 만든 경우에 제약만 빠진다.
alter table parents drop constraint if exists ck_parents_status;
alter table parents
    add constraint ck_parents_status check (status in ('ACTIVE', 'SUSPENDED'));

-- 정지 사유와 시각. 사유가 없으면 나중에 왜 막았는지 아무도 모른다.
alter table parents add column if not exists suspended_at     timestamptz;
alter table parents add column if not exists suspended_reason text;
