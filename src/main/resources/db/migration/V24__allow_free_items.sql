-- 아이템 값이 0일 수 있게 한다.
--
-- V1 이 `check (price > 0)` 으로 잠가 뒀다. 값을 매기는 물건이니 0원은 없다는 뜻이었는데,
-- 시연에서는 심사위원이 별가루를 모으는 과정 없이 꾸미기를 바로 봐야 해서 전부 0으로 둔다.
-- 제약을 그대로 두고 시드만 0 으로 바꾸면 **Flyway 가 실패하고 앱이 아예 안 뜬다.**
--
-- 0 은 허용하되 음수는 계속 막는다 — 음수 가격은 별가루가 늘어나는 구매가 되어
-- 잔액 계산이 뒤집힌다. 느슨해지기만 하는 변경이라 기존 행은 전부 그대로 통과한다.
alter table items drop constraint if exists items_price_check;
alter table items add constraint items_price_check check (price >= 0);
