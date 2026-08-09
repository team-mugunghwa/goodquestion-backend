-- ============================================================
-- 굿퀘스천 DDL v4 (PostgreSQL) — 24개 테이블
-- v4 변경 (팀원공유 db/001~004 반영 + 행성 명칭 통일)
--   · islands -> planets, island_items -> planet_items 로 개명 (도메인 용어를 "행성"으로 통일)
--   · 배치 좌표를 프론트(planet/)와 같은 축좌표로 통일: grid_x/grid_y -> placed_q/placed_r
--     축좌표는 원점 기준이라 음수가 유효하므로 하한 check를 두지 않는다.
--     판 크기·모양은 클라이언트 카탈로그가 단일 소스라 planets.grid_width/height를 제거했다.
--   · scene_audio 신설 (TTS 사전 생성 음성 — 팀원공유 001)
--   · characters 신설 + story_scenes 확장 (캐릭터 레지스트리 — 팀원공유 004)
--   · child_story_play_counts 신설 (반복 완주 상한 — 팀원공유 003)
--   · messages에 STT 신뢰도 3종, utterance_analyses에 model_id,
--     story_sessions에 safety_* 3종 추가 (브리프 요구 — 팀원공유 002)
--   · story_sessions.version 추가 (턴 처리 낙관적 락)
--   · stardust_transactions.scene_id 추가 + 지급 멱등 인덱스를 세션/장면으로 분리
--     (기존 단일 인덱스는 장면 보너스 2건째를 유니크 위반으로 막았다)
--   · post_activity_results.card_order_seed, items.status 추가
-- 코드값은 서버 enum과의 일관성을 위해 대문자 스네이크케이스로 통일
--
-- ⚠ 이 파일은 "빈 DB"에만 실행한다.
--   테이블이 이미 있는 DB에 실행하면 기존 테이블은 전부 already exists로 실패하고
--   신규 테이블만 만들어져 반쯤 적용된 상태가 된다 — 겉보기엔 다 생성된 것처럼 보인다.
--   아래 안전장치가 그 경우를 막는다.
--
--     빈 DB에 새로 만들기   psql -d <db> -f db/schema.sql
--     v3 DB를 올리기        psql -d <db> -f db/migration-to-v4.sql
--     내용 버리고 새로 만들기 psql -d <db> -c 'drop schema public cascade; create schema public;'
--                            뒤에 schema.sql 실행
--
-- 주의: ddl-auto=validate이므로 컬럼을 바꾸면 엔티티도 함께 고쳐야 앱이 뜬다.
-- ============================================================

-- 전체를 한 트랜잭션으로 묶는다. 중간에 실패하면 아무것도 남기지 않는다 —
-- 반쯤 적용된 스키마가 제일 위험하다(엔티티 검증은 통과하는데 컬럼이 없는 상태).
begin;

-- 안전장치: 이미 테이블이 있으면 여기서 멈춘다.
do $$
begin
    if exists (select 1 from information_schema.tables
               where table_schema = 'public' and table_type = 'BASE TABLE') then
        raise exception 'schema.sql은 빈 DB 전용이다 — public 스키마에 이미 테이블이 있다.'
            using hint = 'v3 DB라면 db/migration-to-v4.sql을 실행한다. '
                         '내용을 버려도 된다면 drop schema public cascade; create schema public; 후 다시 실행한다.';
    end if;
end $$;

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
-- 4-3. characters — 캐릭터 레지스트리
--    story_scenes.character_name varchar 하나로는 성격·말투·보이스·표정키를 담을 수 없다.
--    특히 TTS 화자 고정이 여기 걸린다 — 장면마다 페르소나가 따로 있으면 같은 캐릭터가
--    장면별로 다른 목소리로 합성되는 것을 막을 방법이 없다.
-- ------------------------------------------------------------
create table characters (
    id              uuid        primary key default gen_random_uuid(),
    story_id        uuid        not null references stories(id) on delete cascade,
    -- 표정 이미지 파일명의 키: {character_key}_{expression}.png. 바꾸면 이미지 조회가 깨진다.
    character_key   varchar(64) not null,
    name            varchar(50) not null,     -- 화면 표시 이름
    personality     text        not null,     -- 성격·말투 (캐릭터 LLM 페르소나)
    -- 유도를 "어떻게 드러낼지". GUIDED 모드 대사 생성 입력
    guidance_style  text,
    tts_voice       varchar(64),
    -- Gemini 계열 연기 지시문. 보이스 이름이 성별을 보장하지 않으므로 성별·연령을 반드시 포함한다
    tts_style       text,
    tts_gender      varchar(10)
        check (tts_gender is null or tts_gender in ('MALE', 'FEMALE')),
    -- 이 캐릭터가 실제로 가진 표정. 없는 표정을 요구하면 fallback으로 내린다
    expression_keys text[]      not null default '{}',
    created_at      timestamptz not null default now(),

    unique (story_id, character_key)
);

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
    -- 캐릭터 참조. character_name은 화면 표시용으로 남기고 페르소나·보이스·표정은 이 FK로 찾는다
    character_id       uuid        references characters(id) on delete set null,
    character_name     varchar(50),
    -- 같은 캐릭터라도 장면마다 입장이 다르다 (예: 시아버지가 대화2에서는 내치려 하고 전개4에서는 후회한다)
    scene_stance       text,
    -- STT 디코딩 힌트. 아동 발화는 고유명사 오인식이 가장 많다. 예: {자라, 별주부, 용왕}
    proper_nouns       text[]      not null default '{}',
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
create index idx_story_scenes_character_id on story_scenes(character_id);

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
    -- 위험 신호(자해·가정 폭력·학대 정황)로 캐릭터 대사 생성을 중단한 적이 있는 세션.
    -- 감지하고도 남길 자리가 없으면 감지한 의미가 사라진다.
    -- safety_categories에는 범주만 남기고 아이 발화 원문은 남기지 않는다.
    safety_flagged                     boolean     not null default false,
    safety_categories                  text[]      not null default '{}',
    safety_flagged_at                  timestamptz,
    status                             varchar(20) not null default 'IN_PROGRESS'
        check (status in ('IN_PROGRESS', 'POST_ACTIVITY', 'COMPLETED', 'STOPPED')),
    -- 낙관적 락(@Version). 턴 처리는 STT·분석·대사 생성으로 수 초가 걸려
    -- 연타가 들어오면 턴 카운터·누적 요소가 덮어써진다 — 덮어쓰기를 409로 바꾼다.
    version                            bigint      not null default 0,
    started_at                         timestamptz not null default now(),
    completed_at                       timestamptz,
    last_activity_at                   timestamptz not null default now()
);

-- 메인 화면 "이어하기" 조회: 아이별 최근 활동 순
create index idx_story_sessions_child_recent
    on story_sessions(child_id, last_activity_at desc);
create index idx_story_sessions_story_id on story_sessions(story_id);
-- 진행 중 세션 조회 · 반복 완주 횟수 확인
create index idx_story_sessions_child_status on story_sessions(child_id, status);
-- 확인이 필요한 세션만 빠르게 뽑는다
create index idx_story_sessions_safety on story_sessions(safety_flagged) where safety_flagged;

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
    -- STT 신뢰도(0~1). 기준값 이하면 낮은 신뢰 표시를 남기고 대표 발화 후보에서 제외한다.
    -- 기준값 자체는 아직 미정이라 판정은 애플리케이션이 한다.
    stt_confidence     numeric(4,3)
        check (stt_confidence is null or (stt_confidence >= 0 and stt_confidence <= 1)),
    stt_low_confidence boolean     not null default false,
    stt_retry_count    smallint    not null default 0,   -- 아이가 다시 말한 횟수
    -- 캐릭터 표정 키. 값 목록은 characters.expression_keys로 캐릭터마다 다르므로 check를 두지 않는다
    character_emotion  varchar(20),
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
    -- 분석에 사용한 LLM 식별자. analysis_version만으로는 같은 프롬프트를 모델만 바꿔
    -- 돌린 경우를 구분할 수 없다. 소급이 안 되는 값이라 처음부터 남긴다.
    model_id            varchar(64),
    -- 후처리에서 폐기된 근거. 분석 LLM이 없는 요소를 만들어내는 빈도 추적용(검수·프롬프트 개선)
    dropped_evidence    jsonb       not null default '[]',
    created_at          timestamptz not null default now()
);

-- ------------------------------------------------------------
-- 9. post_activity_results — 말하기 후 활동 결과 (세션당 1건)
-- ------------------------------------------------------------
create table post_activity_results (
    id                uuid primary key default gen_random_uuid(),
    session_id        uuid     not null unique references story_sessions(id) on delete cascade,
    -- 카드 셔플 고정용 시드. 없으면 재진입·재시도마다 순서가 바뀌어 채점 재현이 안 된다
    card_order_seed   varchar(64) not null,
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
    -- 운영 중 내리기. child_items가 FK로 물고 있어 행 삭제는 불가능하므로 상태로 감춘다
    status                 varchar(10)  not null default 'ACTIVE'
        check (status in ('ACTIVE', 'HIDDEN')),
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
        check (reason in ('STORY_COMPLETED', 'SCENE_BONUS', 'ITEM_PURCHASE', 'ADMIN_ADJUST')),
    -- 지급 근거 세션 (멱등 판정용). 구매는 null
    session_id    uuid        references story_sessions(id) on delete set null,
    -- 장면 보너스는 장면마다 최대 1회라 장면까지 구분해야 한다. 완주 보상·구매는 null
    scene_id      uuid        references story_scenes(id) on delete set null,
    -- 사용(구매) 대상 아이템. 지급은 null
    item_id       uuid        references items(id),
    acknowledged  boolean     not null default false,
    created_at    timestamptz not null default now()
);

-- 지급 멱등 (데이터-06). 세션 단위와 장면 단위를 나눠 건다 —
-- (session_id, reason) 하나로 묶으면 장면 보너스 2건째가 유니크 위반으로 막힌다.
create unique index idx_stardust_tx_session_reason
    on stardust_transactions(session_id, reason)
    where session_id is not null and scene_id is null;
create unique index idx_stardust_tx_scene_reason
    on stardust_transactions(session_id, scene_id, reason)
    where session_id is not null and scene_id is not null;
create index idx_stardust_tx_wallet on stardust_transactions(wallet_id, created_at desc);

-- ------------------------------------------------------------
-- 16-1. child_story_play_counts — 이야기별 완주 횟수
--     "2회차는 완주 보상 절반, 3회차부터 지급 없음"의 판정 근거.
--     COMPLETED 세션을 count하면 조회와 지급 사이가 원자적이지 않아 중복 지급이 난다.
--     upsert 한 문장으로 증가시키고 그 반환값으로 지급액을 정한다.
-- ------------------------------------------------------------
create table child_story_play_counts (
    child_id    uuid        not null references children(id) on delete cascade,
    story_id    uuid        not null references stories(id)  on delete cascade,
    play_count  smallint    not null default 0 check (play_count >= 0),
    updated_at  timestamptz not null default now(),

    primary key (child_id, story_id)
);

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
-- 18. planets — 아이의 행성 (아이당 1개, 보상-15~16, 보상-22, 보상-26)
--     판 크기·모양은 클라이언트 카탈로그가 단일 소스라 서버에 두지 않는다.
-- ------------------------------------------------------------
create table planets (
    id                  uuid        primary key default gen_random_uuid(),
    child_id            uuid        not null unique references children(id) on delete cascade,
    name                varchar(30) not null default '내 행성',
    tutorial_completed  boolean     not null default false,
    created_at          timestamptz not null default now()
);

-- ------------------------------------------------------------
-- 19. planet_items — 격자 배치 (보상-16~17)
--     좌표는 프론트(planet/)와 같은 축좌표(q, r). 원점 기준이라 음수가 유효해
--     하한 check를 두지 않는다.
--     겹침 불가와 "보유 아이템 하나는 한 곳에만"을 DB 유니크로 보장한다 (보상-02).
--     치우기는 이 행을 삭제하는 것이고 child_items는 남는다 = 보관함 복귀.
--
--     한계: 발판이 2x2인 아이템은 앵커 칸만 저장하므로 나머지 칸의 겹침은 이 유니크가
--     막지 못한다. 카탈로그 발판 정의로 애플리케이션이 점유 칸을 계산해 검증해야 한다.
-- ------------------------------------------------------------
create table planet_items (
    id             uuid        primary key default gen_random_uuid(),
    planet_id      uuid        not null references planets(id) on delete cascade,
    child_item_id  uuid        not null unique references child_items(id) on delete cascade,
    placed_q       smallint    not null,
    placed_r       smallint    not null,
    placed_at      timestamptz not null default now(),

    unique (planet_id, placed_q, placed_r)   -- 한 칸에 하나 (409 CELL_OCCUPIED)
);

create index idx_planet_items_planet_id on planet_items(planet_id);

-- ------------------------------------------------------------
-- 20. scene_audio — TTS 사전 생성 음성
--     내레이션과 장면 첫·마지막 대사만 담는다. 중간 반응 대사는 실시간 합성이라 여기 없다.
--     오디오 바이너리는 오브젝트 스토리지에 두고 경로와 메타데이터만 남긴다.
--
--     주의: messages·child_consents의 "원본 음성 미저장"은 아이가 말한 녹음을 남기지
--     않는다는 개인정보 원칙이고, TTS 산출물 저장과는 다른 문제다.
-- ------------------------------------------------------------
create table scene_audio (
    id               uuid        primary key default gen_random_uuid(),
    scene_id         uuid        not null references story_scenes(id) on delete cascade,
    -- NARRATION = scene_description 낭독
    slot             varchar(20) not null
        check (slot in ('NARRATION', 'OPENING', 'CLOSING')),
    -- 아이 이름이 들어가는 대사("ㅇㅇ아, ...")만 아이별로 렌더한다. null이면 공용 음성
    child_id         uuid        references children(id) on delete cascade,
    storage_path     text        not null,
    -- 렌더 원본 텍스트의 SHA-256. 없으면 대사를 고쳤을 때 화면엔 새 문장,
    -- 스피커엔 옛 문장인 상태가 되고 아무도 눈치채지 못한다. 재생 전에 대조해 잡는다.
    text_hash        char(64)    not null,
    -- 보이스를 바꿔 재렌더할 때 어떤 게 옛 엔진 산출물인지 모르면 전수 재생성밖에 없다
    engine           varchar(64) not null,
    voice            varchar(64) not null,
    style_prompt     text,                       -- Gemini 계열 연기 지시문
    speaking_rate    numeric(4,2),               -- Cloud TTS 계열 속도
    duration_ms      integer     not null check (duration_ms > 0),
    -- 문장별 실측 시작·끝(초). 자막·화면 전환을 글자수 비례 추정이 아니라 이 값으로 붙인다.
    --   [{"index":0,"text":"...","start":0.0,"end":4.68}, ...]
    sentence_timings jsonb       not null default '[]',
    created_at       timestamptz not null default now()
);

-- 장면·슬롯당 최신 음성 하나. null은 유니크에서 중복 취급이라 부분 인덱스로 나눠 건다
create unique index idx_scene_audio_shared
    on scene_audio(scene_id, slot) where child_id is null;
create unique index idx_scene_audio_per_child
    on scene_audio(scene_id, slot, child_id) where child_id is not null;
create index idx_scene_audio_scene_id on scene_audio(scene_id);

commit;
