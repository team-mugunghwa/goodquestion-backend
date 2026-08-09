-- ============================================================
-- v3 -> v4 마이그레이션 (PostgreSQL)
--
-- 새로 만드는 DB는 이 파일이 아니라 db/schema.sql을 쓴다.
-- 이미 v3로 뜬 DB에만 실행한다. 여러 번 실행해도 안전하도록 작성했다.
--
-- 반영 내용
--   1. islands -> planets 개명 · 좌표계를 프론트와 같은 축좌표(q, r)로 변경
--   2. 팀원공유 db/002 — 브리프 요구인데 빠져 있던 컬럼 (STT 신뢰도·모델 ID·안전 플래그)
--   3. 팀원공유 db/001 — scene_audio (TTS 사전 생성 음성)
--   4. 팀원공유 db/004 — characters 레지스트리 + 장면 설정
--   5. 팀원공유 db/003 이식 — 별가루 멱등 인덱스 분리 · 반복 완주 횟수
--   6. 잔여 결함 — 낙관적 락 · 카드 셔플 시드 · 아이템 상태
-- ============================================================

begin;

-- ------------------------------------------------------------
-- 1. islands -> planets, 좌표계 변경
--    프론트(planet/)가 축좌표 q·r을 쓰므로 서버도 같은 이름으로 맞춘다.
--    축좌표는 원점 기준이라 음수가 유효하다 — 기존 ">= 0" check를 반드시 없앤다.
--    판 크기·모양은 클라이언트 카탈로그가 단일 소스라 grid_width/height를 버린다.
-- ------------------------------------------------------------
alter table if exists islands      rename to planets;
alter table if exists island_items rename to planet_items;

alter table planet_items rename column island_id to planet_id;
alter table planet_items rename column grid_x    to placed_q;
alter table planet_items rename column grid_y    to placed_r;

-- 컬럼을 rename해도 check 제약은 따라오므로 (placed_q >= 0)이 남는다. 음수 좌표를 막는다.
alter table planet_items drop constraint if exists island_items_grid_x_check;
alter table planet_items drop constraint if exists island_items_grid_y_check;
alter table planet_items drop constraint if exists planet_items_placed_q_check;
alter table planet_items drop constraint if exists planet_items_placed_r_check;

alter table planets drop column if exists grid_width;
alter table planets drop column if exists grid_height;

alter index if exists idx_island_items_island_id rename to idx_planet_items_planet_id;

-- ------------------------------------------------------------
-- 2. 브리프 요구인데 빠져 있던 컬럼 (팀원공유 db/002)
--    전부 나중에 소급이 안 되는 값이라 데이터가 쌓이기 전에 넣는다.
-- ------------------------------------------------------------

-- STT 신뢰도. 기준값 이하면 대표 발화 후보에서 제외한다 — 지금은 신뢰도 자체가 없어 판정 불가
alter table messages
    add column if not exists stt_confidence     numeric(4,3),
    add column if not exists stt_low_confidence boolean  not null default false,
    add column if not exists stt_retry_count    smallint not null default 0;

do $$ begin
    alter table messages add constraint messages_stt_confidence_check
        check (stt_confidence is null or (stt_confidence >= 0 and stt_confidence <= 1));
exception when duplicate_object then null; end $$;

-- 표정 키는 캐릭터마다 다르다(characters.expression_keys) — 고정 6종 check를 푼다
alter table messages drop constraint if exists messages_character_emotion_check;

-- analysis_version 하나로는 같은 프롬프트를 모델만 바꿔 돌린 경우를 구분할 수 없다
alter table utterance_analyses
    add column if not exists model_id         varchar(64),
    add column if not exists dropped_evidence jsonb not null default '[]';

-- 위험 신호를 감지하고도 남길 자리가 없으면 감지한 의미가 사라진다.
-- 범주만 남기고 아이 발화 원문은 남기지 않는다.
alter table story_sessions
    add column if not exists safety_flagged    boolean not null default false,
    add column if not exists safety_categories text[]  not null default '{}',
    add column if not exists safety_flagged_at timestamptz;

create index if not exists idx_story_sessions_safety
    on story_sessions(safety_flagged) where safety_flagged;

-- ------------------------------------------------------------
-- 3. scene_audio — TTS 사전 생성 음성 (팀원공유 db/001)
-- ------------------------------------------------------------
create table if not exists scene_audio (
    id               uuid        primary key default gen_random_uuid(),
    scene_id         uuid        not null references story_scenes(id) on delete cascade,
    slot             varchar(20) not null
        check (slot in ('NARRATION', 'OPENING', 'CLOSING')),
    child_id         uuid        references children(id) on delete cascade,
    storage_path     text        not null,
    -- 대사를 고쳤을 때 화면엔 새 문장, 스피커엔 옛 문장인 상태를 막는다
    text_hash        char(64)    not null,
    engine           varchar(64) not null,
    voice            varchar(64) not null,
    style_prompt     text,
    speaking_rate    numeric(4,2),
    duration_ms      integer     not null check (duration_ms > 0),
    sentence_timings jsonb       not null default '[]',
    created_at       timestamptz not null default now()
);

create unique index if not exists idx_scene_audio_shared
    on scene_audio(scene_id, slot) where child_id is null;
create unique index if not exists idx_scene_audio_per_child
    on scene_audio(scene_id, slot, child_id) where child_id is not null;
create index if not exists idx_scene_audio_scene_id on scene_audio(scene_id);

-- ------------------------------------------------------------
-- 4. characters 레지스트리 + 장면 설정 (팀원공유 db/004)
--    story_scenes.character_name varchar 하나로는 TTS 화자 고정이 불가능하다.
-- ------------------------------------------------------------
create table if not exists characters (
    id              uuid        primary key default gen_random_uuid(),
    story_id        uuid        not null references stories(id) on delete cascade,
    character_key   varchar(64) not null,
    name            varchar(50) not null,
    personality     text        not null,
    guidance_style  text,
    tts_voice       varchar(64),
    tts_style       text,
    tts_gender      varchar(10)
        check (tts_gender is null or tts_gender in ('MALE', 'FEMALE')),
    expression_keys text[]      not null default '{}',
    created_at      timestamptz not null default now(),

    unique (story_id, character_key)
);

alter table story_scenes
    add column if not exists character_id uuid references characters(id) on delete set null,
    add column if not exists scene_stance text,
    add column if not exists proper_nouns text[] not null default '{}';

create index if not exists idx_story_scenes_character_id on story_scenes(character_id);

-- ------------------------------------------------------------
-- 5. 별가루 — 멱등 인덱스 분리 · 반복 완주 횟수 (팀원공유 db/003 이식)
--
--    기존 (session_id, reason) 단일 유니크는 장면 보너스 2건째를 막았다.
--    "장면당 +1, 최대 2 -> 세션 합계 3~5" 규칙이 성립하지 않던 원인이다.
-- ------------------------------------------------------------
alter table stardust_transactions
    add column if not exists scene_id uuid references story_scenes(id) on delete set null;

alter table stardust_transactions drop constraint if exists stardust_transactions_reason_check;
alter table stardust_transactions add constraint stardust_transactions_reason_check
    check (reason in ('STORY_COMPLETED', 'SCENE_BONUS', 'ITEM_PURCHASE', 'ADMIN_ADJUST'));

drop index if exists idx_stardust_tx_session_reason;
create unique index if not exists idx_stardust_tx_session_reason
    on stardust_transactions(session_id, reason)
    where session_id is not null and scene_id is null;
create unique index if not exists idx_stardust_tx_scene_reason
    on stardust_transactions(session_id, scene_id, reason)
    where session_id is not null and scene_id is not null;

-- 완주 횟수를 COMPLETED 세션 count로 구하면 조회와 지급 사이가 원자적이지 않아
-- 중복 지급이 난다. upsert 한 문장으로 올리고 그 반환값으로 지급액을 정한다.
create table if not exists child_story_play_counts (
    child_id    uuid        not null references children(id) on delete cascade,
    story_id    uuid        not null references stories(id)  on delete cascade,
    play_count  smallint    not null default 0 check (play_count >= 0),
    updated_at  timestamptz not null default now(),

    primary key (child_id, story_id)
);

-- 이미 완주한 세션이 있으면 초기값을 채운다
insert into child_story_play_counts (child_id, story_id, play_count)
select child_id, story_id, count(*)
from story_sessions
where status = 'COMPLETED'
group by child_id, story_id
on conflict (child_id, story_id) do nothing;

-- ------------------------------------------------------------
-- 6. 잔여 결함
-- ------------------------------------------------------------

-- 턴 처리는 STT·분석·대사 생성으로 수 초가 걸린다. 연타가 들어오면
-- 턴 카운터·누적 요소가 덮어써지고 max_turns 종료 판정까지 어긋난다.
alter table story_sessions add column if not exists version bigint not null default 0;

create index if not exists idx_story_sessions_child_status
    on story_sessions(child_id, status);

-- 카드 셔플 고정용 시드. 기존 행은 임의 시드로 채운다
alter table post_activity_results add column if not exists card_order_seed varchar(64);
update post_activity_results set card_order_seed = gen_random_uuid()::text
    where card_order_seed is null;
alter table post_activity_results alter column card_order_seed set not null;

-- child_items가 FK로 물고 있어 행 삭제가 불가능하므로 상태로 감춘다
alter table items add column if not exists status varchar(10) not null default 'ACTIVE';
do $$ begin
    alter table items add constraint items_status_check check (status in ('ACTIVE', 'HIDDEN'));
exception when duplicate_object then null; end $$;

commit;
