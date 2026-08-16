-- 이야기 어휘 사전 - 이야기 단위 단어장(뜻 포함).
--
-- 고정 대사의 낱말은 시드로 관리되는 폐집합이라 뜻을 미리 검수해 둘 수 있다.
-- 아이가 단어를 담을 때 여기 있으면 뜻 생성 LLM을 부르지 않고 복사한다.
-- (아이별 wordbook과 다르다 - 이 테이블은 이야기당 한 벌인 원본 사전이다)
--
-- word는 표제어(조사를 뗀 원형)로 넣는다. 저장 경로가 WordLemmatizer로
-- 정규화한 표제어를 키로 조회하기 때문이다.
create table story_vocabulary
(
    id               uuid         primary key default gen_random_uuid(),
    story_id         uuid         not null references stories (id) on delete cascade,
    word             varchar(50)  not null,
    meaning          varchar(200) not null,
    example_sentence varchar(300),
    created_at       timestamptz  not null default now(),

    constraint uq_story_vocabulary_story_word unique (story_id, word)
);

comment on table story_vocabulary is '이야기 어휘 사전 - 고정 대사 낱말의 검수된 뜻/예문. 단어 담기가 LLM 대신 먼저 조회한다';
comment on column story_vocabulary.word is '표제어(조사를 뗀 원형)';
