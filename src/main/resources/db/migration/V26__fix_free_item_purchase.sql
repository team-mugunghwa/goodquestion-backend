-- 값 0 은 구매를 막는다. 되돌리고 별가루를 넉넉히 준다.
--
-- V25 가 값을 0 으로 내렸더니 구매가 통째로 실패했다. 차감 거래가 0 원짜리로 만들어지는데
-- stardust_transactions 에 `check (amount <> 0)` 이 걸려 있다(V1). 0 원 거래는 기록으로서
-- 의미가 없다는 뜻이라 제약 자체는 옳다 - 그러니 제약을 풀 것이 아니라 값을 되돌린다.
--
-- 시연 목표는 "값이 0" 이 아니라 **"별가루 걱정 없이 다 놓을 수 있다"** 였다.
-- 값을 1 로 두고 잔액을 크게 주면 같은 결과가 되고, 구매 흐름(차감·거래기록)도 그대로 산다.
-- 심사위원에게 별가루 경제가 작동하는 모습이 보이는 것은 오히려 낫다.
update items set price = 1 where price = 0;

-- 해금은 V25 그대로 전부 ALWAYS 로 둔다.

-- 모든 아이 지갑에 별가루를 채운다. 아이템이 48종에 1원씩이라 넉넉하다.
-- total_earned 도 같이 올린다 - 누적 해금 조건이 이 값을 보므로, 나중에 해금을
-- 원래대로 되돌려도 열린 상태가 유지된다.
update stardust_wallets
set balance = greatest(balance, 9999),
    total_earned = greatest(total_earned, 9999);

-- 지갑이 아직 없는 아이에게도 만들어 준다. 첫 로그인 보너스가 안 붙는 계정이 있다.
insert into stardust_wallets (child_id, balance, total_earned)
select c.id, 9999, 9999
from children c
where not exists (select 1 from stardust_wallets w where w.child_id = c.id);
