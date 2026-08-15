-- ============================================================
-- 고객센터 데모 데이터
--
-- 문의는 보호자가 있어야 성립하므로 R__2_seed_demo_data.sql의 데모 계정을 참조한다.
-- 실행 순서가 알파벳순이라 2_seed_demo_data 다음, 4_seed_service_desk가 뒤에 온다.
--
-- 공지와 이용안내는 여기 넣지 않는다. 그쪽은 보호자를 참조하지 않아 관리자 콘솔의
-- R__1_seed_admin.sql이 넣는다. 같은 DB이므로 서비스 앱에서도 그대로 보인다.
--
-- 충돌 정책은 do nothing이다. 관리자가 콘솔에서 답변을 달거나 상태를 바꿔 둔 것을
-- 다음 기동에 시드가 되돌리면 안 된다.
--
-- 데모 계정이 없는 환경(운영)에서는 where exists에 걸려 아무것도 넣지 않는다.
-- ============================================================

insert into inquiries (id, parent_id, category, title, content, status, answered_at, created_at)
select v.id, v.parent_id, v.category, v.title, v.content, v.status, v.answered_at, v.created_at
from (values
    ('d0000000-0000-4000-8000-000000000001'::uuid,
     '99999999-9999-9999-9999-000000000001'::uuid,
     'BUG', '아이 목소리가 가끔 안 들어가요',
     E'8살 아이와 방귀쟁이 며느리를 하는 중인데, 말하기 버튼을 눌러도 세 번 중 한 번은 인식이 안 됩니다.\n아이패드 9세대에서 사용하고 있어요.',
     'PENDING', null::timestamptz, now() - interval '2 days'),
    ('d0000000-0000-4000-8000-000000000002'::uuid,
     '99999999-9999-9999-9999-000000000001'::uuid,
     'ACCOUNT', '아이를 한 명 더 등록하고 싶어요',
     E'둘째도 같이 쓰게 하려는데 어디서 추가하는지 못 찾겠습니다.',
     'ANSWERED', now() - interval '4 days', now() - interval '5 days'),
    ('d0000000-0000-4000-8000-000000000003'::uuid,
     '99999999-9999-9999-9999-000000000002'::uuid,
     'SUGGESTION', '이야기를 더 넣어 주세요',
     E'아이가 방귀쟁이 며느리를 다섯 번째 하고 있습니다. 다른 이야기도 빨리 만나고 싶어요.',
     'PENDING', null::timestamptz, now() - interval '9 hours')
) as v(id, parent_id, category, title, content, status, answered_at, created_at)
where exists (select 1 from parents p where p.id = v.parent_id)
on conflict (id) do nothing;

insert into inquiry_answers (id, inquiry_id, admin_id, admin_name, content, created_at)
select
    'e0000000-0000-4000-8000-000000000001'::uuid,
    'd0000000-0000-4000-8000-000000000002'::uuid,
    null,
    '고객센터',
    E'안녕하세요, 굿퀘스천입니다.\n\n마이페이지 > 아이 프로필에서 "아이 추가"를 눌러 등록하실 수 있습니다.\n아이별로 진행 기록과 리포트, 별가루가 따로 관리되니 둘째도 처음부터 편하게 시작할 수 있어요.\n\n이용해 주셔서 감사합니다.',
    now() - interval '4 days'
where exists (select 1 from inquiries i where i.id = 'd0000000-0000-4000-8000-000000000002')
on conflict (id) do nothing;

-- 답변 알림. 실제로는 관리자가 답변을 등록할 때 서버가 만든다.
-- 데모에서 알림함이 비어 있지 않게 하려고 한 건 넣어 둔다.
insert into notifications (id, parent_id, type, title, body, link_path, created_at)
select
    'f0000000-0000-4000-8000-000000000001'::uuid,
    '99999999-9999-9999-9999-000000000001'::uuid,
    'INQUIRY_ANSWERED',
    '문의하신 내용에 답변이 등록되었습니다',
    '"아이를 한 명 더 등록하고 싶어요" 문의에 답변이 도착했어요. 눌러서 확인해 주세요.',
    '/support/d0000000-0000-4000-8000-000000000002',
    now() - interval '4 days'
where exists (select 1 from parents p where p.id = '99999999-9999-9999-9999-000000000001')
on conflict (id) do nothing;

-- 방문 기록. 대시보드의 방문자 추이가 빈 그래프로 보이지 않게 최근 2주를 채운다.
-- generate_series로 날짜를 만들고 데모 계정이 격일로 다녀간 것처럼 둔다.
insert into daily_visits (parent_id, visit_date, visit_count)
select p.id, d::date, 1 + (extract(day from d)::int % 3)
from generate_series(current_date - interval '13 days', current_date, interval '1 day') as d
cross join (
    select id from parents
    where id in ('99999999-9999-9999-9999-000000000001', '99999999-9999-9999-9999-000000000002')
) as p
where extract(day from d)::int % 2 = 0
on conflict (parent_id, visit_date) do nothing;
