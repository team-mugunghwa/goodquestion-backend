-- ============================================================
-- 후속 자유 대화 (Free Talk) — 이야기를 끝낸 뒤 등장인물과 이어서 하는 대화
--
-- 학습(요소 채우기)이 아니라 관계다. 유도도 요소 판정도 별가루도 없고, 리포트에도
-- 반영하지 않는다 - 리포트는 학습 지표이고 자유 대화는 그 바깥이다. 그래서 기존
-- story_sessions/messages에 얹지 않고 표를 따로 둔다. 얹었다면 리포트·별가루가 읽는
-- 모든 조회에 "자유 대화는 빼고"라는 조건이 붙었을 것이고, 하나만 빠뜨려도 학습
-- 지표가 조용히 오염된다.
--
-- 진입 조건은 "그 이야기를 완주한 아이"다. 판정 근거는 story_sessions의 COMPLETED
-- 세션이며 여기에 따로 복사해 두지 않는다 - 같은 사실을 두 곳에 두면 갈린다.
--
-- 아이 음성 원본은 저장하지 않는다(기존 원칙). 텍스트만 남긴다.
-- ============================================================

create table free_talks (
    id           uuid        primary key default gen_random_uuid(),
    child_id     uuid        not null references children(id) on delete cascade,
    story_id     uuid        not null references stories(id),
    character_id uuid        not null references characters(id),
    -- 아이가 말한 횟수. 상한(10)에 닿으면 캐릭터가 마무리하고 ended_at이 찍힌다
    turn_count   smallint    not null default 0 check (turn_count >= 0),
    ended_at     timestamptz,
    created_at   timestamptz not null default now()
);

-- 인물 고르기 화면이 "이 아이가 이 이야기의 누구와 언제 마지막으로 이야기했는지"를 묻는다
create index idx_free_talks_child_story on free_talks(child_id, story_id);

create table free_talk_messages (
    id           uuid        primary key default gen_random_uuid(),
    free_talk_id uuid        not null references free_talks(id) on delete cascade,
    role         varchar(10) not null check (role in ('CHILD', 'CHARACTER')),
    text         text        not null,
    emotion      varchar(20),
    -- 설계 초안에 없던 컬럼이다. created_at만으로는 순서가 정해지지 않는다 -
    -- PostgreSQL의 now()는 트랜잭션 시작 시각이라 같은 트랜잭션에서 저장된 두 줄이
    -- 같은 값을 갖는다. 대화 이력은 LLM 입력이라 순서가 뒤집히면 문맥이 통째로 어긋난다.
    -- 유니크 제약은 덤으로 동시 제출을 막는다(messages의 turn_order와 같은 방식) -
    -- 겹친 요청은 무결성 충돌로 409가 되고, 중복 턴과 LLM 이중 과금이 생기지 않는다.
    turn_order   smallint    not null check (turn_order >= 0),
    created_at   timestamptz not null default now(),

    unique (free_talk_id, turn_order)
);

create index idx_free_talk_messages_free_talk on free_talk_messages(free_talk_id);
