-- ============================================================
-- 예문 따라 말하기 (학습 -> 보상 -> 행성 꾸미기 사이클)
--
-- 단어 말하기 연습(V9)은 단어를 넣어 새 문장을 만드는 학습이고, 이번에는 예문을
-- 그대로 따라 말하는 학습이다. 예문 3종(이야기/일상/심화, V14) 중 하나를 골라
-- 따라 말하면 STT 텍스트와 예문의 문자 일치율을 재고, 90% 이상이면 별가루 2개를 준다.
--
-- 지급 규칙: 예문(단어 x 유형)당 최초 1회 · 2개 · 하루 최대 2건.
--   예문당 1회는 아래 unique 제약이 보장하고, 하루 상한은 서비스가 이 표를 세어
--   판단한다. 행은 "보상이 나간 연습"만 남긴다 - 상한에 걸린 날의 성공은 기록하지
--   않아, 그 예문은 다음 날 다시 성공하면 보상받을 수 있다(V9와 같은 방식).
--
-- reason 체크 제약을 다시 여는데, V9까지의 전체 집합에 SENTENCE_PRACTICED를 더한다.
-- ============================================================

alter table stardust_transactions drop constraint stardust_transactions_reason_check;
alter table stardust_transactions add constraint stardust_transactions_reason_check
    check (reason in ('STORY_COMPLETED', 'SCENE_BONUS', 'ITEM_PURCHASE',
                      'WELCOME', 'WORD_PRACTICED', 'SENTENCE_PRACTICED', 'ADMIN_ADJUST'));

create table sentence_practices (
    id            uuid         primary key default gen_random_uuid(),
    wordbook_id   uuid         not null references wordbook(id) on delete cascade,
    child_id      uuid         not null references children(id) on delete cascade,
    -- 어떤 예문을 따라 말했는지 (이야기/일상/심화)
    sentence_type varchar(20)  not null check (sentence_type in ('STORY', 'DAILY', 'ADVANCED')),
    -- STT가 인식한 발화 텍스트. **음성은 저장하지 않는다** - V9와 같은 원칙이다.
    spoken_text   text         not null,
    -- 채점된 일치율(0.00~1.00). 보호자가 "얼마나 정확히 말했는지" 볼 수 있게 남긴다.
    similarity    numeric(3,2) not null,
    created_at    timestamptz  not null default now(),
    -- 예문당 최초 1회만 지급한다. 같은 예문을 다시 연습하는 것은 막지 않고 보상만 안 준다.
    unique (wordbook_id, sentence_type)
);

-- 하루 상한을 세는 조회의 인덱스. (아이, 시각) 순서라 범위 조건이 뒤에 온다(V9와 같은 꼴).
create index idx_sentence_practices_child_created on sentence_practices(child_id, created_at);
