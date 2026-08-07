-- ============================================================
-- 굿퀘스천 DDL v2 (PostgreSQL / Supabase)
-- 기준: DB 구조_260803_수정안 + 추가 요건 전체 포함
-- 변경: 이미지 컬럼 추가, topics 분리, reports·wordbook 추가,
--       messages.character_emotion 추가 (캐릭터 마음 변화)
-- 코드값은 서버 enum과의 일관성을 위해 대문자 스네이크케이스로 통일
-- ============================================================

create extension if not exists "pgcrypto";

-- ------------------------------------------------------------
-- 1. parents — 보호자 계정
--    id는 Supabase auth.users.id와 동일하게 사용 (자체 생성 없음)
-- ------------------------------------------------------------
create table parents (
    id          uuid primary key,
    name        varchar(50)  not null,
    created_at  timestamptz  not null default now()
);

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
    meaning           text        not null,   -- 아이 수준의 쉬운 뜻
    example_sentence  text,                   -- 단어가 나온 이야기 속 문장
    is_favorite       boolean     not null default false,
    source_scene_id   uuid        references story_scenes(id),
    created_at        timestamptz not null default now(),

    unique (child_id, word)   -- 같은 단어 중복 저장 방지
);

create index idx_wordbook_child_id on wordbook(child_id);
