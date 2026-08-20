-- 시연용: 아이템을 전부 공짜로, 전부 열린 상태로 둔다.
--
-- **왜 시드(R__)가 아니라 여기인가.**
-- R__ 는 체크섬이 바뀌면 다시 도는데, 운영 DB 의 story_scenes 행이 시드와 id·순서가
-- 어긋나 있어서 재실행되는 순간 유니크 위반으로 죽는다(2026-08-20 배포 8회 연속 실패).
-- 그래서 R__ 는 마지막으로 성공한 내용 그대로 두어 **아예 재실행되지 않게** 하고,
-- 시연에 필요한 변경만 이 일회성 마이그레이션으로 넣는다.
--
-- 값 0 은 V24 가 제약을 `price >= 0` 으로 풀어 둔 덕에 통과한다(V24 -> V25 순서).
-- unlock_type 은 'ALWAYS' 로만 바꾸고 unlock_stardust_total·unlock_story_id 는
-- 남겨 둔다 - 제약이 단방향이라 값이 남아도 통과하고, 되돌릴 때 unlock_type 한 칸만
-- 뒤집으면 원래 규칙이 살아난다.
--
-- 되돌리기: update items set price = 1;  (기념 아이템은 2)
--           update items set unlock_type = 'STARDUST_CUMULATIVE' where ...;
update items set price = 0 where price <> 0;
update items set unlock_type = 'ALWAYS' where unlock_type <> 'ALWAYS';
