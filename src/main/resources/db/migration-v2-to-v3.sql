-- ============================================================
-- 굿퀘스천 DB 마이그레이션 v2 -> v3
--
-- 이미 데이터가 들어 있는 DB용. 새로 만드는 경우에는 schema.sql만 실행하면 된다.
-- 결과는 schema.sql을 새로 실행한 것과 동일한 구조가 된다(컬럼 추가 순서만 다름).
--
-- 실행: psql "$DB_URL" -v ON_ERROR_STOP=1 -f src/main/resources/db/migration-v2-to-v3.sql
-- 되돌리기 스크립트는 제공하지 않는다. 운영 데이터가 있으면 먼저 백업할 것.
-- ============================================================

begin;

-- ------------------------------------------------------------
-- 1. stories — 이야기 상세용 컬럼 (선택-03)
-- ------------------------------------------------------------
alter table stories add column child_role varchar(50);
alter table stories add column intro      text;

-- ------------------------------------------------------------
-- 2. story_sessions — 장면 보너스 판정용 플래그 (진행-18)
-- ------------------------------------------------------------
alter table story_sessions
    add column guided_used_in_scene boolean not null default false;

-- ------------------------------------------------------------
-- 3. wordbook — is_favorite -> entry_type, meaning nullable (단어-02, 단어-04)
--    기존 즐겨찾기 데이터를 FAVORITE으로 옮긴 뒤 컬럼을 제거한다.
-- ------------------------------------------------------------
alter table wordbook add column entry_type varchar(20) not null default 'UNKNOWN';
update wordbook set entry_type = 'FAVORITE' where is_favorite;
alter table wordbook add constraint wordbook_entry_type_check
    check (entry_type in ('UNKNOWN', 'FAVORITE'));
alter table wordbook drop column is_favorite;
alter table wordbook alter column meaning drop not null;

-- ------------------------------------------------------------
-- 4. 신규 테이블 8종 — schema.sql과 동일한 정의
-- ------------------------------------------------------------

create table refresh_tokens (
    id          uuid         primary key default gen_random_uuid(),
    parent_id   uuid         not null references parents(id) on delete cascade,
    token_hash  varchar(100) not null unique,
    expires_at  timestamptz  not null,
    revoked_at  timestamptz,
    created_at  timestamptz  not null default now()
);

create index idx_refresh_tokens_parent_id on refresh_tokens(parent_id);

create table mission_results (
    id            uuid        primary key default gen_random_uuid(),
    session_id    uuid        not null references story_sessions(id) on delete cascade,
    scene_id      uuid        not null references story_scenes(id),
    mission_id    varchar(30) not null,
    mission_type  varchar(30) not null
        check (mission_type in ('PROBLEM_SOLVING', 'PERSPECTIVE_SHIFT')),
    result        jsonb       not null default '{}',
    created_at    timestamptz not null default now(),

    unique (session_id, mission_id)
);

create index idx_mission_results_session_id on mission_results(session_id);

create table items (
    id                     uuid         primary key default gen_random_uuid(),
    name                   varchar(50)  not null,
    category               varchar(20)  not null
        check (category in ('TERRAIN_PROP', 'PLANT', 'STRUCTURE', 'ANIMAL')),
    price                  integer      not null check (price > 0),
    unlock_type            varchar(30)  not null
        check (unlock_type in ('ALWAYS', 'STORY_COMPLETE', 'STARDUST_CUMULATIVE')),
    unlock_story_id        uuid         references stories(id),
    unlock_stardust_total  integer      check (unlock_stardust_total is null or unlock_stardust_total > 0),
    model_url              text,
    thumbnail_url          text,
    display_order          smallint     not null default 0,
    created_at             timestamptz  not null default now(),

    check (unlock_type <> 'STORY_COMPLETE' or unlock_story_id is not null),
    check (unlock_type <> 'STARDUST_CUMULATIVE' or unlock_stardust_total is not null)
);

create index idx_items_display_order on items(display_order);

create table stardust_wallets (
    id            uuid        primary key default gen_random_uuid(),
    child_id      uuid        not null unique references children(id) on delete cascade,
    balance       integer     not null default 0 check (balance >= 0),
    total_earned  integer     not null default 0 check (total_earned >= 0),
    created_at    timestamptz not null default now()
);

create table stardust_transactions (
    id            uuid        primary key default gen_random_uuid(),
    wallet_id     uuid        not null references stardust_wallets(id) on delete cascade,
    amount        integer     not null check (amount <> 0),
    reason        varchar(30) not null
        check (reason in ('STORY_COMPLETED', 'SCENE_BONUS', 'ITEM_PURCHASE')),
    session_id    uuid        references story_sessions(id) on delete set null,
    item_id       uuid        references items(id),
    acknowledged  boolean     not null default false,
    created_at    timestamptz not null default now()
);

create unique index idx_stardust_tx_session_reason
    on stardust_transactions(session_id, reason)
    where session_id is not null;
create index idx_stardust_tx_wallet on stardust_transactions(wallet_id, created_at desc);

create table child_items (
    id           uuid        primary key default gen_random_uuid(),
    child_id     uuid        not null references children(id) on delete cascade,
    item_id      uuid        not null references items(id),
    acquired_at  timestamptz not null default now()
);

create index idx_child_items_child_id on child_items(child_id);

create table islands (
    id                  uuid        primary key default gen_random_uuid(),
    child_id            uuid        not null unique references children(id) on delete cascade,
    name                varchar(30) not null default '내 행성',
    grid_width          smallint    not null default 8 check (grid_width > 0),
    grid_height         smallint    not null default 8 check (grid_height > 0),
    tutorial_completed  boolean     not null default false,
    created_at          timestamptz not null default now()
);

create table island_items (
    id             uuid        primary key default gen_random_uuid(),
    island_id      uuid        not null references islands(id) on delete cascade,
    child_item_id  uuid        not null unique references child_items(id) on delete cascade,
    grid_x         smallint    not null check (grid_x >= 0),
    grid_y         smallint    not null check (grid_y >= 0),
    placed_at      timestamptz not null default now(),

    unique (island_id, grid_x, grid_y)
);

create index idx_island_items_island_id on island_items(island_id);

-- ------------------------------------------------------------
-- 5. 아이 프로필마다 지갑·섬을 1개씩 보장한다 (계정-14)
--    기존 아이들에게 소급 생성. 앞으로는 아이 등록 트랜잭션에서 함께 만든다.
-- ------------------------------------------------------------
insert into stardust_wallets (child_id)
select c.id from children c
where not exists (select 1 from stardust_wallets w where w.child_id = c.id);

insert into islands (child_id)
select c.id from children c
where not exists (select 1 from islands i where i.child_id = c.id);

commit;
