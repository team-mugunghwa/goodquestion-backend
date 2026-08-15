-- ============================================================
-- 단어장 말하기 연습 (학습 -> 보상 -> 행성 꾸미기 사이클)
--
-- 단어장이 모으기만 하고 학습도 보상도 없어 사이클이 끊겨 있었다.
-- 말하기 학습 서비스이므로 연습도 말하기다 - 단어를 넣어 문장을 만들어 말하면
-- 별가루를 준다.
--
-- 지급 규칙: 단어당 최초 1회 · 1개 · 하루 최대 3개.
--   단어당 1회는 아래 unique 제약이 보장하고, 하루 상한은 서비스가 이 표를 세어
--   판단한다. 행은 "보상이 나간 연습"만 남긴다 - 상한에 걸린 날의 성공은 기록하지
--   않아, 그 단어는 다음 날 다시 성공하면 보상받을 수 있다.
--
-- V8(환영 별가루) 뒤에 온다. reason 체크 제약을 다시 여는데, V8이 넣은 WELCOME을
-- 포함한 전체 집합으로 만든다.
-- ============================================================

alter table stardust_transactions drop constraint stardust_transactions_reason_check;
alter table stardust_transactions add constraint stardust_transactions_reason_check
    check (reason in ('STORY_COMPLETED', 'SCENE_BONUS', 'ITEM_PURCHASE',
                      'WELCOME', 'WORD_PRACTICED', 'ADMIN_ADJUST'));

create table word_practices (
    id           uuid        primary key default gen_random_uuid(),
    -- 단어당 최초 1회만 지급한다. 같은 단어를 다시 연습하는 것은 막지 않고 보상만 안 준다.
    wordbook_id  uuid        not null unique references wordbook(id) on delete cascade,
    child_id     uuid        not null references children(id) on delete cascade,
    -- 아이가 만든 문장. **음성은 저장하지 않는다** - 텍스트만 남긴다.
    -- 보호자가 "무슨 문장을 만들었는지" 볼 수 있게 원문 그대로 둔다.
    spoken_text  text        not null,
    created_at   timestamptz not null default now()
);

-- 하루 상한을 세는 조회의 인덱스. (아이, 시각) 순서라 범위 조건이 뒤에 온다.
create index idx_word_practices_child_created on word_practices(child_id, created_at);
