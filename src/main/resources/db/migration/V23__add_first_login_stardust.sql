-- ============================================================
-- 최초 로그인 별가루 (계정당 1회 · 아이 전원)
--
-- 지갑은 아이당 1개인데 로그인은 보호자 단위라, "계정당 1회"를 별가루 이력만으로는
-- 표현할 수 없다 - 나중에 아이가 늘면 그 아이에게는 이력이 없어 다시 대상이 된다.
-- 계정 단위 선점 기록을 따로 둔다. 행이 있으면 그 계정은 지급이 끝난 것이다.
--
-- 지급 자체는 FirstLoginBonusService가 로그인 커밋 뒤 새 트랜잭션에서 한다.
-- 아이가 아직 없는 계정(가입 직후 첫 로그인)은 선점하지 않고 넘어가므로,
-- 여기 행이 생겼다는 것은 실제로 별가루가 나갔다는 뜻이다.
--
-- reason은 체크 제약으로 값이 묶여 있어 함께 고친다. V15까지의 전체 집합에
-- FIRST_LOGIN을 더한다.
-- ============================================================

alter table stardust_transactions drop constraint stardust_transactions_reason_check;
alter table stardust_transactions add constraint stardust_transactions_reason_check
    check (reason in ('STORY_COMPLETED', 'SCENE_BONUS', 'ITEM_PURCHASE',
                      'WELCOME', 'FIRST_LOGIN', 'WORD_PRACTICED', 'SENTENCE_PRACTICED',
                      'ADMIN_ADJUST'));

create table first_login_bonus_grants (
    -- 계정 자체가 키다. 계정당 1행이라는 규칙을 PK가 그대로 강제한다.
    parent_id  uuid        primary key references parents(id) on delete cascade,
    granted_at timestamptz not null default now()
);
