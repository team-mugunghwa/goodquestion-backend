-- ============================================================
-- 환영 별가루 (보상 -> 행성 꾸미기 사이클의 시작점)
--
-- 새 아이의 지갑은 잔액 0으로 만들어진다. 첫 이야기를 완주하기 전에는 상점에서
-- 아무것도 살 수 없어, 행성 탭을 먼저 열어 본 아이는 빈 화면만 만난다.
-- 가입 직후 소품 한둘을 사서 놓아 보게 하면 "학습하면 별가루 -> 행성이 자란다"는
-- 사이클을 시작 전에 한 바퀴 맛보게 된다.
--
-- reason은 체크 제약으로 값이 묶여 있어 함께 고친다. 지급 자체는 아이 생성 시
-- RewardProvisioningListener가 한다 - 여기서는 사유만 연다.
-- ============================================================

alter table stardust_transactions drop constraint stardust_transactions_reason_check;
alter table stardust_transactions add constraint stardust_transactions_reason_check
    check (reason in ('STORY_COMPLETED', 'SCENE_BONUS', 'ITEM_PURCHASE',
                      'WELCOME', 'ADMIN_ADJUST'));
