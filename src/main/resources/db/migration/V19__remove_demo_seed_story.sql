-- 흐름 확인용 데모 이야기 "작은 씨앗"을 서비스에서 내린다.
--
-- 대화 턴 파이프라인이 501이던 시절 R__2_seed_demo_data.sql이 넣은 이야기다. 장면
-- 3개가 전부 STORY라 후속 활동과 별가루 지급까지 실제로 이어지는지 확인하는 용도였다.
-- 파이프라인이 붙은 지금은 역할이 끝났는데 status가 PUBLISHED라 실제 사용자 목록에
-- 그대로 노출된다. 시드에서 빼는 것만으로는 이미 적재된 DB에서 사라지지 않아 여기서 지운다.
--
-- story_topics와 story_scenes는 on delete cascade라 함께 지워진다. 반면
-- story_sessions.story_id와 items.unlock_story_id는 cascade가 없어서, 누가 이미
-- 플레이한 DB에서 하드 삭제하면 FK 위반으로 마이그레이션이 실패한다. 그래서 참조가
-- 없을 때만 지우고, 있으면 ARCHIVED로 내려 목록에서만 감춘다 - 사용자에게 나가는
-- 조회는 전부 PUBLISHED만 보므로 결과는 같고 남의 진행 기록은 그대로 남는다.
delete from stories s
 where s.id = '11111111-1111-1111-1111-222222222222'
   and not exists (select 1 from story_sessions ss where ss.story_id = s.id)
   and not exists (select 1 from items i where i.unlock_story_id = s.id);

-- 위에서 지워졌으면 맞는 행이 없어 아무 일도 하지 않는다.
update stories set status = 'ARCHIVED'
 where id = '11111111-1111-1111-1111-222222222222';
