-- ============================================================
-- 굿퀘스천 DDL v3 (PostgreSQL) — 21개 테이블
-- v3 변경
--   · refresh_tokens 신설 (리프레시 회전·로그아웃 무효화)
--   · mission_results 신설 (미션 결과)
--   · 보상(섬 꾸미기) 6종 신설:
--     items / child_items / stardust_wallets / stardust_transactions / islands / island_items
--   · stories에 child_role·intro 추가 (이야기 상세)
--   · wordbook.is_favorite -> entry_type, meaning을 nullable로 (미입력 시 LLM 생성)
--   · story_sessions에 guided_used_in_scene 추가 (장면 보너스 판정)
-- 코드값은 서버 enum과의 일관성을 위해 대문자 스네이크케이스로 통일
--
-- 주의: ddl-auto=validate이므로 컬럼을 바꾸면 엔티티도 함께 고쳐야 앱이 뜬다.
-- 기존 DB가 있으면 이 파일 대신 db/migration-v2-to-v3.sql을 실행한다.
-- ============================================================

create extension if not exists "pgcrypto";

-- ------------------------------------------------------------
-- 1. parents — 보호자 계정
--    Supabase Auth를 사용하지 않고 서버가 직접 발급·관리한다.
--    provider=LOCAL은 email+password_hash로, provider=KAKAO는 provider_id로 식별한다.
-- ------------------------------------------------------------
create table parents (
    id             uuid primary key default gen_random_uuid(),
    email          varchar(255),
    password_hash  varchar(100),
    provider       varchar(20)  not null
        check (provider in ('LOCAL', 'KAKAO')),
    provider_id    varchar(100),
    name           varchar(50)  not null,
    created_at     timestamptz  not null default now()
);

create unique index idx_parents_email on parents(email) where email is not null;
create unique index idx_parents_provider_id on parents(provider, provider_id) where provider_id is not null;

-- ------------------------------------------------------------
-- 2. children — 아이 정보
-- ------------------------------------------------------------
create table children (
    id          uuid primary key default gen_random_uuid(),
    parent_id   uuid         not null references parents(id) on delete cascade,
    name        varchar(50)  not null,
    birth_year  smallint     not null check (birth_year between 2000 and 2100),
    created_at  timestamptz  not null default now()
);

create index idx_children_parent_id on children(parent_id);

-- ------------------------------------------------------------
-- 3. child_consents — 아동 개인정보 처리 동의
-- ------------------------------------------------------------
create table child_consents (
    id                   uuid primary key default gen_random_uuid(),
    child_id             uuid         not null references children(id) on delete cascade,
    consent_version      varchar(30)  not null,
    verification_method  varchar(30)  not null
        check (verification_method in ('AUTHENTICATED_PARENT', 'INSTITUTION_PAPER', 'MOBILE_VERIFICATION')),
    consented_at         timestamptz  not null default now(),
    withdrawn_at         timestamptz
);

create index idx_child_consents_child_id on child_consents(child_id);

-- ------------------------------------------------------------
-- 4. stories — 이야기 (정적 콘텐츠)
--    image_url: 목록·상세 화면의 대표 이미지 (Supabase Storage 경로)
-- ------------------------------------------------------------
create table stories (
    id                    uuid primary key default gen_random_uuid(),
    title                 varchar(100) not null,
    summary               text         not null,
    -- 상세 화면: 아이가 맡는 역할과 도입 소개 (선택-03)
    child_role            varchar(50),
    intro                 text,
    image_url             text,
    difficulty            varchar(20)  not null,
    estimated_minutes     smallint,
    post_activity_config  jsonb,
    status                varchar(20)  not null default 'DRAFT'
        check (status in ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    created_at            timestamptz  not null default now()
);

-- 목록 화면은 공개된 이야기만 조회
create index idx_stories_status on stories(status);

-- ------------------------------------------------------------
-- 4-1. topics — 토픽 마스터
-- ------------------------------------------------------------
create table topics (
    id             uuid primary key default gen_random_uuid(),
    name           varchar(30)  not null unique,
    display_order  smallint     not null default 0,
    created_at     timestamptz  not null default now()
);

-- ------------------------------------------------------------
-- 4-2. story_topics — 이야기·토픽 매핑 (M:N)
-- ------------------------------------------------------------
create table story_topics (
    story_id  uuid not null references stories(id) on delete cascade,
    topic_id  uuid not null references topics(id)  on delete cascade,

    primary key (story_id, topic_id)
);

create index idx_story_topics_topic_id on story_topics(topic_id);

-- ------------------------------------------------------------
-- 5. story_scenes — 장면 (정적 콘텐츠)
--    image_url: 장면 진행 화면의 배경/장면 이미지
-- ------------------------------------------------------------
create table story_scenes (
    id                 uuid primary key default gen_random_uuid(),
    story_id           uuid        not null references stories(id) on delete cascade,
    scene_order        smallint    not null,
    -- STORY: 내레이션 장면(도입·전개, 아이 발화 없음) / DIALOGUE: 캐릭터 대화 장면
    -- 콘텐츠 문서('방귀 뀌는 며느리')의 화면 흐름이 두 유형을 구분함에 따라 추가
    scene_type         varchar(20) not null check (scene_type in ('STORY', 'DIALOGUE')),
    scene_description  text        not null,
    conflict           text,
    image_url          text,
    character_name     varchar(50),
    -- 캐릭터 성격·상태 설명. 캐릭터 LLM 입력 — 장면별로 두어 이야기 진행에 따른 변화 반영
    character_persona  text,
    character_opening  text,
    -- 콘텐츠 문서 '공통 대화 장면 처리 규칙'이 고정 마지막 대사 출력 조건을 정의
    -- → 기존 상충은 "고정 마지막 대사 사용"으로 확정 (최대 턴 시: LLM 짧은 반응 후 재생)
    character_closing  text,
    scene_goal         text,
    -- 허용 값: DECISION, REASON, PERSPECTIVE, SOLUTION, RESULT, EMOTION, EMPATHY, REQUEST
    required_elements  text[],
    -- 장면별 요소 인정 기준 (발화 분석 문서 8장). 분석 LLM 입력.
    element_criteria   jsonb       not null default '{}',
    -- 요소별 캐릭터의 남은 걱정 (발화 분석 문서 8장). 유도 시 캐릭터 LLM에 전달.
    remaining_worries  jsonb       not null default '{}',
    -- 이야기 내 미션 설정 (목적·노출 조건·확인 요소). 미션 없는 장면은 null
    mission_config     jsonb,
    preferred_turns    smallint,
    max_turns          smallint,

    unique (story_id, scene_order),
    -- 대화 장면은 대화 관련 필드 필수
    check (scene_type = 'STORY' or (
        character_name is not null and character_opening is not null
        and scene_goal is not null and required_elements is not null
        and preferred_turns is not null and max_turns is not null
    )),
    check (preferred_turns is null or max_turns is null or preferred_turns <= max_turns)
);

create index idx_story_scenes_story_id on story_scenes(story_id);

-- ------------------------------------------------------------
-- 6. story_sessions — 이야기 진행 기록 (런타임 상태)
-- ------------------------------------------------------------
create table story_sessions (
    id                                 uuid primary key default gen_random_uuid(),
    child_id                           uuid        not null references children(id) on delete cascade,
    story_id                           uuid        not null references stories(id),
    current_scene_id                   uuid        references story_scenes(id),
    current_child_turn_count           smallint    not null default 0,
    accumulated_elements               text[]      not null default '{}',
    last_detected_elements             text[]      not null default '{}',
    last_response_mode                 varchar(20)
        check (last_response_mode in ('NORMAL', 'GUIDED', 'CLOSING')),
    last_guidance_target               varchar(20),
    turns_without_new_element          smallint    not null default 0,
    consecutive_low_information_turns  smallint    not null default 0,
    scene_goal_met                     boolean     not null default false,
    scene_end_reason                   varchar(20)
        check (scene_end_reason in ('GOAL_MET', 'MAX_TURNS')),
    -- 현재 장면에서 유도 모드가 한 번이라도 발생했는지 (장면 이동 시 초기화).
    -- 유도 없이 목표를 통과하면 별가루 장면 보너스 대상이 된다 (진행-18 -> 보상-04)
    guided_used_in_scene               boolean     not null default false,
    -- 현재 장면의 미션 상태 (장면 이동 시 초기화). 종료 조건·재노출 방지에 사용
    mission_exposed                    boolean     not null default false,
    mission_completed                  boolean     not null default false,
    status                             varchar(20) not null default 'IN_PROGRESS'
        check (status in ('IN_PROGRESS', 'POST_ACTIVITY', 'COMPLETED', 'STOPPED')),
    started_at                         timestamptz not null default now(),
    completed_at                       timestamptz,
    last_activity_at                   timestamptz not null default now()
);

-- 메인 화면 "이어하기" 조회: 아이별 최근 활동 순
create index idx_story_sessions_child_recent
    on story_sessions(child_id, last_activity_at desc);
create index idx_story_sessions_story_id on story_sessions(story_id);

-- ------------------------------------------------------------
-- 7. messages — 대화 기록
--    character_emotion: 캐릭터 마음 변화(표정·태도) 표시용.
--    캐릭터 발화에만 저장, 값 목록은 서버 enum(CharacterEmotion)으로 관리
-- ------------------------------------------------------------
create table messages (
    id                 uuid primary key default gen_random_uuid(),
    session_id         uuid        not null references story_sessions(id) on delete cascade,
    scene_id           uuid        not null references story_scenes(id),
    speaker_type       varchar(20) not null
        check (speaker_type in ('CHILD', 'CHARACTER', 'SYSTEM')),
    turn_order         integer     not null,
    text               text        not null,
    stt_raw_text       text,   -- 아이 발화에만 저장, 원본 음성은 저장하지 않음
    character_emotion  varchar(20)
        check (character_emotion in ('NEUTRAL', 'HAPPY', 'SAD', 'WORRIED', 'SURPRISED', 'RELIEVED')),
    created_at         timestamptz not null default now(),

    unique (session_id, turn_order)
);

create index idx_messages_session_turn on messages(session_id, turn_order);

-- ------------------------------------------------------------
-- 8. utterance_analyses — 발화 분석 결과 (아이 메시지 1건당 1건)
-- ------------------------------------------------------------
create table utterance_analyses (
    id                  uuid primary key default gen_random_uuid(),
    message_id          uuid        not null unique references messages(id) on delete cascade,
    child_intent        varchar(20) not null,
    main_point          text,
    -- 예: [{"type": "REASON", "evidence": "억울하니까"}]
    detected_elements   jsonb       not null default '[]',
    utterance_validity  varchar(20) not null
        check (utterance_validity in ('VALID', 'SHORT', 'UNCLEAR', 'OFF_TOPIC', 'PLAYFUL')),
    -- analysis_versions 테이블 대신 MVP에서는 버전 문자열만 기록 (DB 문서 방침 유지)
    analysis_version    varchar(30) not null default 'mvp_v1',
    created_at          timestamptz not null default now()
);

-- ------------------------------------------------------------
-- 9. post_activity_results — 말하기 후 활동 결과 (세션당 1건)
-- ------------------------------------------------------------
create table post_activity_results (
    id                uuid primary key default gen_random_uuid(),
    session_id        uuid     not null unique references story_sessions(id) on delete cascade,
    submitted_order   text[],
    is_order_correct  boolean,
    attempt_count     smallint not null default 0,
    retelling_text    text,
    completed_at      timestamptz
);

-- ------------------------------------------------------------
-- 10. reports — 보호자 리포트 (세션당 1건)
--     세션의 대화·발화 분석 결과를 종합해 보호자에게 보여줄 내용
-- ------------------------------------------------------------
create table reports (
    id          uuid primary key default gen_random_uuid(),
    session_id  uuid  not null unique references story_sessions(id) on delete cascade,
    summary     text  not null,   -- 아이의 전체 활동 요약
    -- 아이가 잘 보여준 말하기 요소. 예: [{"element": "REASON", "comment": "..."}]
    strengths   jsonb not null default '[]',
    -- 다음에 연습하면 좋은 말하기 요소. 형식은 strengths와 동일
    next_focus  jsonb not null default '[]',
    created_at  timestamptz not null default now()
);

-- ------------------------------------------------------------
-- 11. wordbook — 단어장
--     대화 중 만난 단어 저장. 쉬운 뜻·이야기 속 문장 제공, 좋아하는 단어 표시
-- ------------------------------------------------------------
create table wordbook (
    id                uuid primary key default gen_random_uuid(),
    child_id          uuid        not null references children(id) on delete cascade,
    word              varchar(50) not null,
    -- 아이 수준의 쉬운 뜻. 저장 요청에 없으면 서버가 LLM으로 생성하므로 nullable (단어-02)
    meaning           text,
    example_sentence  text,                   -- 단어가 나온 이야기 속 문장
    -- UNKNOWN: 모르는 단어 / FAVORITE: 좋아하는 단어 (단어-01, 단어-04)
    entry_type        varchar(20) not null default 'UNKNOWN'
        check (entry_type in ('UNKNOWN', 'FAVORITE')),
    source_scene_id   uuid        references story_scenes(id),
    created_at        timestamptz not null default now(),

    unique (child_id, word)   -- 같은 단어 중복 저장 방지
);

create index idx_wordbook_child_id on wordbook(child_id);

-- ------------------------------------------------------------
-- 12. refresh_tokens — 리프레시 토큰 회전·무효화 (계정-05)
--     원문은 저장하지 않고 해시만 보관한다. 회전 시 이전 토큰을 revoked 처리한다.
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

-- ------------------------------------------------------------
-- 13. mission_results — 미션 수행 결과 (세션·미션당 1건, 미션-11)
--     result: 미션1은 {"tool": "...", "safety": "..."}, 미션2는 {"cards": [...]}
-- ------------------------------------------------------------
create table mission_results (
    id            uuid        primary key default gen_random_uuid(),
    session_id    uuid        not null references story_sessions(id) on delete cascade,
    scene_id      uuid        not null references story_scenes(id),
    mission_id    varchar(30) not null,
    mission_type  varchar(30) not null
        check (mission_type in ('PROBLEM_SOLVING', 'PERSPECTIVE_SHIFT')),
    result        jsonb       not null default '{}',
    created_at    timestamptz not null default now(),

    unique (session_id, mission_id)   -- 중복 제출 방지 (409 MISSION_ALREADY_SUBMITTED)
);

create index idx_mission_results_session_id on mission_results(session_id);

-- ------------------------------------------------------------
-- 14. items — 아이템 마스터 (보상-09~11, MVP 16종)
--     해금 3종: 항상 열림 / 이야기 완주 / 별가루 누적 획득
-- ------------------------------------------------------------
create table items (
    id                     uuid         primary key default gen_random_uuid(),
    name                   varchar(50)  not null,
    category               varchar(20)  not null
        check (category in ('TERRAIN_PROP', 'PLANT', 'STRUCTURE', 'ANIMAL')),
    price                  integer      not null check (price > 0),
    unlock_type            varchar(30)  not null
        check (unlock_type in ('ALWAYS', 'STORY_COMPLETE', 'STARDUST_CUMULATIVE')),
    -- unlock_type=STORY_COMPLETE일 때 해금 대상 이야기 (동물↔이야기 1:1)
    unlock_story_id        uuid         references stories(id),
    -- unlock_type=STARDUST_CUMULATIVE일 때 필요한 누적 획득량
    unlock_stardust_total  integer      check (unlock_stardust_total is null or unlock_stardust_total > 0),
    model_url              text,
    thumbnail_url          text,
    display_order          smallint     not null default 0,
    created_at             timestamptz  not null default now(),

    -- 해금 조건별 필수값을 DB가 보장한다
    check (unlock_type <> 'STORY_COMPLETE' or unlock_story_id is not null),
    check (unlock_type <> 'STARDUST_CUMULATIVE' or unlock_stardust_total is not null)
);

create index idx_items_display_order on items(display_order);

-- ------------------------------------------------------------
-- 15. stardust_wallets — 별가루 지갑 (아이당 1개, 보상-07)
--     total_earned는 누적 해금 판정 기준이라 사용해도 줄지 않는다.
-- ------------------------------------------------------------
create table stardust_wallets (
    id            uuid        primary key default gen_random_uuid(),
    child_id      uuid        not null unique references children(id) on delete cascade,
    balance       integer     not null default 0 check (balance >= 0),
    total_earned  integer     not null default 0 check (total_earned >= 0),
    created_at    timestamptz not null default now()
);

-- ------------------------------------------------------------
-- 16. stardust_transactions — 별가루 지급·사용 이력 (보상-07~08)
--     amount: 지급 +, 사용 −. acknowledged=false면 섬 진입 시 떨어지는 연출 대상.
-- ------------------------------------------------------------
create table stardust_transactions (
    id            uuid        primary key default gen_random_uuid(),
    wallet_id     uuid        not null references stardust_wallets(id) on delete cascade,
    amount        integer     not null check (amount <> 0),
    reason        varchar(30) not null
        check (reason in ('STORY_COMPLETED', 'SCENE_BONUS', 'ITEM_PURCHASE')),
    -- 지급 근거 세션 (멱등 판정용). 구매는 null
    session_id    uuid        references story_sessions(id) on delete set null,
    -- 사용(구매) 대상 아이템. 지급은 null
    item_id       uuid        references items(id),
    acknowledged  boolean     not null default false,
    created_at    timestamptz not null default now()
);

-- 지급 멱등: 같은 세션·같은 사유로는 1건만 (데이터-06)
create unique index idx_stardust_tx_session_reason
    on stardust_transactions(session_id, reason)
    where session_id is not null;
create index idx_stardust_tx_wallet on stardust_transactions(wallet_id, created_at desc);

-- ------------------------------------------------------------
-- 17. child_items — 보유 아이템 (보상-14, 보상-20)
--     같은 아이템 중복 구매를 허용하므로 (child_id, item_id) 유니크를 두지 않는다.
--     구매 취소·삭제 경로가 없어 행은 지워지지 않는다 — 보관함의 원천.
-- ------------------------------------------------------------
create table child_items (
    id           uuid        primary key default gen_random_uuid(),
    child_id     uuid        not null references children(id) on delete cascade,
    item_id      uuid        not null references items(id),
    acquired_at  timestamptz not null default now()
);

create index idx_child_items_child_id on child_items(child_id);

-- ------------------------------------------------------------
-- 18. islands — 아이의 섬 (아이당 1개, 보상-15~16, 보상-22, 보상-26)
-- ------------------------------------------------------------
create table islands (
    id                  uuid        primary key default gen_random_uuid(),
    child_id            uuid        not null unique references children(id) on delete cascade,
    name                varchar(30) not null default '내 행성',
    -- 격자 크기는 확장 대비로 컬럼에 둔다 (MVP 8x8)
    grid_width          smallint    not null default 8 check (grid_width > 0),
    grid_height         smallint    not null default 8 check (grid_height > 0),
    tutorial_completed  boolean     not null default false,
    created_at          timestamptz not null default now()
);

-- ------------------------------------------------------------
-- 19. island_items — 격자 배치 (보상-16~17)
--     겹침 불가와 "보유 아이템 하나는 한 곳에만"을 DB 유니크로 보장한다 (보상-02).
--     치우기는 이 행을 삭제하는 것이고 child_items는 남는다 = 보관함 복귀.
-- ------------------------------------------------------------
create table island_items (
    id             uuid        primary key default gen_random_uuid(),
    island_id      uuid        not null references islands(id) on delete cascade,
    child_item_id  uuid        not null unique references child_items(id) on delete cascade,
    grid_x         smallint    not null check (grid_x >= 0),
    grid_y         smallint    not null check (grid_y >= 0),
    placed_at      timestamptz not null default now(),

    unique (island_id, grid_x, grid_y)   -- 한 칸에 하나 (409 CELL_OCCUPIED)
);

create index idx_island_items_island_id on island_items(island_id);
