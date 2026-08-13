-- ============================================================
-- 비밀번호 재설정 토큰 (계정-06)
-- 원문은 저장하지 않고 해시만 보관한다. 소비 시 consumed_at을 남겨 재사용을 막는다.
-- refresh_tokens와 같은 패턴이다 - revoked_at 대신 consumed_at을 쓴다.
-- ============================================================

create table password_reset_tokens (
    id           uuid         primary key default gen_random_uuid(),
    parent_id    uuid         not null references parents(id) on delete cascade,
    token_hash   varchar(100) not null unique,
    expires_at   timestamptz  not null,
    consumed_at  timestamptz,
    created_at   timestamptz  not null default now()
);

create index idx_password_reset_tokens_parent_id on password_reset_tokens(parent_id);
