-- ============================================================
-- 로그인 보안 (계정-05 보강)
-- 비밀번호 무차별 대입 차단: 5회 실패 시 잠금, 이후 실패마다 잠금 시간 2배(최대 24시간)
-- ============================================================

alter table parents
    add column failed_login_attempts smallint not null default 0,
    add column locked_until          timestamptz,
    -- IPv6 최대 45자. 마지막 로그인 위치만 남기고 이력은 쌓지 않는다.
    add column last_login_ip         varchar(45);