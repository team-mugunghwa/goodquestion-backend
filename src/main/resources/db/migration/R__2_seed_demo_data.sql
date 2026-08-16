-- ============================================================
-- 굿퀘스천 데모 데이터 - Repeatable 마이그레이션 (개발/시연 전용)
--
-- TODO(배포 전): 이 파일을 삭제하거나 dev 프로파일로 옮긴다.
--   운영 DB에도 그대로 적용되면 demo@goodquestion.kr 계정과 가짜 진행 기록이 들어간다.
--
-- 모든 INSERT에 ON CONFLICT DO NOTHING이 붙어 있어 재실행 시 기존 행을 덮지 않는다  - 
-- 데모 아이가 실제로 아이템을 사거나 배치를 바꾸어도 그 상태가 보존된다.
-- 새 행(예: 데모 아이 한 명 추가)만 다음 기동에서 들어간다.
--
-- 데모 로그인
--   이메일   demo@goodquestion.kr
--   비밀번호 demo1234!
--   password_hash는 BCryptPasswordEncoder 기본 설정(강도 10)으로 생성해 검증한 값이다.
--
-- 실행 순서
--   R__1_seed_content.sql (콘텐츠) -> 이 파일 (데모가 콘텐츠를 FK 참조하므로 순서 중요)
-- ============================================================


-- ------------------------------------------------------------
-- 7. parents - 보호자 2명 (이메일 가입 1 / 카카오 1)
--    provider=LOCAL은 email+password_hash로, KAKAO는 provider_id로 식별한다.
-- ------------------------------------------------------------
insert into parents (id, email, password_hash, provider, provider_id, name) values
(
    '99999999-9999-9999-9999-000000000001',
    'demo@goodquestion.kr',
    '$2a$10$czUTK7R0nHlOmq4QJlHCReclMa8wPRUJfXWAqrlWYFudeAx.b3fkO',  -- demo1234!
    'LOCAL', null, '김보호'
),
(
    '99999999-9999-9999-9999-000000000002',
    null, null, 'KAKAO', 'kakao_1234567890', '이보호'
)
on conflict (id) do nothing;

-- ------------------------------------------------------------
-- 8. children - 아이 3명
--    아이 생성 API는 planets/stardust_wallets를 이벤트로 함께 만들지만,
--    시드는 API를 거치지 않으므로 아래 8/12번에서 직접 넣는다.
-- ------------------------------------------------------------
insert into children (id, parent_id, name, birth_year) values
('aaaaaaaa-aaaa-aaaa-aaaa-000000000001', '99999999-9999-9999-9999-000000000001', '지우', 2018),
('aaaaaaaa-aaaa-aaaa-aaaa-000000000002', '99999999-9999-9999-9999-000000000001', '하준', 2019),
('aaaaaaaa-aaaa-aaaa-aaaa-000000000003', '99999999-9999-9999-9999-000000000002', '서연', 2017)
on conflict (id) do nothing;

-- ------------------------------------------------------------
-- 9. child_consents - 아동 개인정보 처리 동의
--    지우: 유효 / 하준: 유효 / 서연: 철회됨(withdrawn_at) - 세션 생성이 막히는 경우 확인용
-- ------------------------------------------------------------
insert into child_consents (id, child_id, consent_version, verification_method, consented_at, withdrawn_at) values
('11110000-0000-0000-0000-000000000001', 'aaaaaaaa-aaaa-aaaa-aaaa-000000000001',
 'mvp_v1', 'AUTHENTICATED_PARENT', now() - interval '30 days', null),
('11110000-0000-0000-0000-000000000002', 'aaaaaaaa-aaaa-aaaa-aaaa-000000000002',
 'mvp_v1', 'AUTHENTICATED_PARENT', now() - interval '20 days', null),
('11110000-0000-0000-0000-000000000003', 'aaaaaaaa-aaaa-aaaa-aaaa-000000000003',
 'mvp_v1', 'AUTHENTICATED_PARENT', now() - interval '40 days', now() - interval '2 days')
on conflict (id) do nothing;

-- ------------------------------------------------------------
-- 10. refresh_tokens - 리프레시 토큰
--    원문은 저장하지 않고 해시만 둔다. 아래 값은 실제 발급 토큰의 해시가 아니라
--    자리 확인용이라 이 행으로는 재발급이 되지 않는다 - 실제 값은 로그인으로만 생긴다.
--    유효 1건 + 로그아웃으로 무효화된 1건.
-- ------------------------------------------------------------
insert into refresh_tokens (id, parent_id, token_hash, expires_at, revoked_at) values
('22220000-0000-0000-0000-000000000001', '99999999-9999-9999-9999-000000000001',
 encode(digest('demo-refresh-token-active', 'sha256'), 'hex'),
 now() + interval '14 days', null),
('22220000-0000-0000-0000-000000000002', '99999999-9999-9999-9999-000000000001',
 encode(digest('demo-refresh-token-revoked', 'sha256'), 'hex'),
 now() + interval '7 days', now() - interval '1 day')
on conflict (id) do nothing;

-- ------------------------------------------------------------
-- 11. story_sessions - 세션 3개
--    A 완주 세션(리포트/후속 활동/별가루 지급까지 완결)
--    B 대화 진행 중(장면5, 유도 1회 사용 - 장면 보너스 대상 아님)
--    C 도입 장면만 본 상태
-- ------------------------------------------------------------
insert into story_sessions (
    id, child_id, story_id, current_scene_id,
    current_child_turn_count, accumulated_elements, last_detected_elements,
    last_response_mode, last_guidance_target,
    turns_without_new_element, consecutive_low_information_turns,
    scene_goal_met, scene_end_reason, guided_used_in_scene,
    mission_exposed, mission_completed,
    safety_flagged, safety_categories, safety_flagged_at,
    status, version, started_at, completed_at, last_activity_at
) values
(
    'bbbbbbbb-bbbb-bbbb-bbbb-000000000001',
    'aaaaaaaa-aaaa-aaaa-aaaa-000000000001',
    '11111111-1111-1111-1111-111111111111',
    '33333333-3333-3333-3333-000000000009',
    2, array['EMOTION', 'PERSPECTIVE', 'RESULT', 'SOLUTION'], array['SOLUTION'],
    'CLOSING', null,
    0, 0,
    true, 'GOAL_MET', false,
    true, true,
    false, '{}', null,
    'COMPLETED', 7, now() - interval '3 days', now() - interval '3 days' + interval '22 minutes',
    now() - interval '3 days' + interval '22 minutes'
),
(
    'bbbbbbbb-bbbb-bbbb-bbbb-000000000002',
    'aaaaaaaa-aaaa-aaaa-aaaa-000000000001',
    '11111111-1111-1111-1111-111111111111',
    '33333333-3333-3333-3333-000000000005',
    1, array['PERSPECTIVE'], array['PERSPECTIVE'],
    'GUIDED', 'REASON',
    0, 0,
    false, null, true,
    false, false,
    false, '{}', null,
    'IN_PROGRESS', 3, now() - interval '2 hours', null, now() - interval '35 minutes'
),
(
    'bbbbbbbb-bbbb-bbbb-bbbb-000000000003',
    'aaaaaaaa-aaaa-aaaa-aaaa-000000000002',
    '11111111-1111-1111-1111-111111111111',
    '33333333-3333-3333-3333-000000000001',
    0, '{}', '{}',
    null, null,
    0, 0,
    false, null, false,
    false, false,
    false, '{}', null,
    'IN_PROGRESS', 0, now() - interval '1 day', null, now() - interval '1 day'
)
on conflict (id) do nothing;

-- ------------------------------------------------------------
-- 12. messages - 대화 기록
--    turn_order는 세션 안에서 유일하다(장면이 바뀌어도 이어진다).
--    도입/전개 내레이션은 저장하지 않는다 - 콘텐츠에서 재생한다.
--    stt_confidence는 아이 발화에만 있고, 낮은 신뢰 1건을 섞어 뒀다.
-- ------------------------------------------------------------
insert into messages (id, session_id, scene_id, speaker_type, turn_order, text,
                      stt_raw_text, stt_confidence, stt_low_confidence, stt_retry_count,
                      character_emotion, created_at) values
-- 세션 A / 장면3 (며느리)
('cccccccc-cccc-cccc-cccc-000000000001', 'bbbbbbbb-bbbb-bbbb-bbbb-000000000001',
 '33333333-3333-3333-3333-000000000003', 'CHARACTER', 1,
 '지우야, 내 방귀가 너무 크다는 걸 알면 가족들이 나를 이상하게 생각하지 않을까?',
 null, null, false, 0, 'WORRIED', now() - interval '3 days'),
('cccccccc-cccc-cccc-cccc-000000000002', 'bbbbbbbb-bbbb-bbbb-bbbb-000000000001',
 '33333333-3333-3333-3333-000000000003', 'CHILD', 2,
 '많이 힘들겠어요. 참으면 배가 아프잖아요.',
 '많이 힘들겠어요 참으면 배가 아프자나요', 0.910, false, 0,
 null, now() - interval '3 days' + interval '1 minute'),
('cccccccc-cccc-cccc-cccc-000000000003', 'bbbbbbbb-bbbb-bbbb-bbbb-000000000001',
 '33333333-3333-3333-3333-000000000003', 'CHARACTER', 3,
 '맞아, 배가 너무 아파. 그래도 가족들이 나를 어떻게 볼지 무서워서 말을 못 하겠어.',
 null, null, false, 0, 'SAD', now() - interval '3 days' + interval '2 minutes'),
('cccccccc-cccc-cccc-cccc-000000000004', 'bbbbbbbb-bbbb-bbbb-bbbb-000000000001',
 '33333333-3333-3333-3333-000000000003', 'CHILD', 4,
 '가족들한테 솔직하게 말해 보세요. 가족이니까 이해해 줄 거예요.',
 '가족들한테 솔직하게 말해 보세요 가족이니까 이해해 줄 거예요', 0.960, false, 0,
 null, now() - interval '3 days' + interval '3 minutes'),
('cccccccc-cccc-cccc-cccc-000000000005', 'bbbbbbbb-bbbb-bbbb-bbbb-000000000001',
 '33333333-3333-3333-3333-000000000003', 'CHARACTER', 5,
 '그래도 아직은 못 말하겠어. 조금만 더 참아 볼게.',
 null, null, false, 0, 'NEUTRAL', now() - interval '3 days' + interval '4 minutes'),
-- 세션 A / 장면9 (며느리, 마지막 대화)
('cccccccc-cccc-cccc-cccc-000000000006', 'bbbbbbbb-bbbb-bbbb-bbbb-000000000001',
 '33333333-3333-3333-3333-000000000009', 'CHARACTER', 6,
 '지우 덕분에 내 방귀가 누군가에게 도움이 될 수 있다는 걸 처음 알았어. 이제는 방귀 소리가 큰 걸 부끄러워하지 않아도 될까?',
 null, null, false, 0, 'SURPRISED', now() - interval '3 days' + interval '18 minutes'),
('cccccccc-cccc-cccc-cccc-000000000007', 'bbbbbbbb-bbbb-bbbb-bbbb-000000000001',
 '33333333-3333-3333-3333-000000000009', 'CHILD', 7,
 '부끄러워하지 않아도 돼요. 소리가 큰 방귀 덕분에 마을 사람들이 배를 먹을 수 있었잖아요. 좋은 일에 쓰면 멋진 거예요.',
 '부끄러워하지 않아도 돼요 소리가 큰 방귀 덕분에 마을 사람들이 배를 먹을 수 있었자나요 좋은 일에 쓰면 멋진 거예요',
 0.880, false, 1, null, now() - interval '3 days' + interval '19 minutes'),
('cccccccc-cccc-cccc-cccc-000000000008', 'bbbbbbbb-bbbb-bbbb-bbbb-000000000001',
 '33333333-3333-3333-3333-000000000009', 'CHARACTER', 8,
 '이제는 부끄러워하며 숨기지 않고, 조심해서 좋은 일에 써 볼게.',
 null, null, false, 0, 'RELIEVED', now() - interval '3 days' + interval '20 minutes'),
-- 세션 B / 장면5 (시아버지, 진행 중 - 마지막 응답이 유도 모드)
('cccccccc-cccc-cccc-cccc-000000000009', 'bbbbbbbb-bbbb-bbbb-bbbb-000000000002',
 '33333333-3333-3333-3333-000000000005', 'CHARACTER', 1,
 '아이고, 이게 무슨 일이냐! 우리 집안이 다 흔들리는구나! 이렇게 창피한 며느리와 함께 못 살겠다! 그렇지 않니?',
 null, null, false, 0, 'SURPRISED', now() - interval '40 minutes'),
('cccccccc-cccc-cccc-cccc-00000000000a', 'bbbbbbbb-bbbb-bbbb-bbbb-000000000002',
 '33333333-3333-3333-3333-000000000005', 'CHILD', 2,
 '음... 며느리도 그러고 싶어서 그런 건 아닐 거예요.',
 '음 며느리도 그러고 시퍼서 그런건 아닐꺼예요', 0.420, true, 2,
 null, now() - interval '38 minutes'),
('cccccccc-cccc-cccc-cccc-00000000000b', 'bbbbbbbb-bbbb-bbbb-bbbb-000000000002',
 '33333333-3333-3333-3333-000000000005', 'CHARACTER', 3,
 '흠, 일부러 그런 게 아니라고? 그래도 어째서 그렇게 요란한 방귀를 뀌게 되었는지 까닭을 모르겠구나.',
 null, null, false, 0, 'WORRIED', now() - interval '35 minutes')
on conflict (id) do nothing;

-- ------------------------------------------------------------
-- 13. utterance_analyses - 아이 발화 분석 (CHILD 메시지 1건당 1건)
--    detected_elements는 서버 후처리를 통과한 요소만 담는다.
--    dropped_evidence는 근거 문구가 발화 원문에 없어 폐기된 것 - 프롬프트 개선 추적용.
-- ------------------------------------------------------------
insert into utterance_analyses (id, message_id, child_intent, main_point, detected_elements,
                                utterance_validity, analysis_version, model_id, dropped_evidence) values
('dddddddd-dddd-dddd-dddd-000000000001', 'cccccccc-cccc-cccc-cccc-000000000002',
 'EMOTION', '며느리가 힘들 것 같다고 공감함',
 '[{"type": "EMOTION", "evidence": "많이 힘들겠어요"}]'::jsonb,
 'VALID', 'mvp_v1', 'gemini-2.5-flash', '[]'::jsonb),
('dddddddd-dddd-dddd-dddd-000000000002', 'cccccccc-cccc-cccc-cccc-000000000004',
 'SOLUTION', '가족에게 솔직하게 말하라고 제안함',
 '[{"type": "SOLUTION", "evidence": "가족들한테 솔직하게 말해 보세요"},
   {"type": "PERSPECTIVE", "evidence": "가족이니까 이해해 줄 거예요"}]'::jsonb,
 'VALID', 'mvp_v1', 'gemini-2.5-flash', '[]'::jsonb),
('dddddddd-dddd-dddd-dddd-000000000003', 'cccccccc-cccc-cccc-cccc-000000000007',
 'PERSPECTIVE', '큰 방귀가 남을 도운 장점이라고 다시 봄',
 '[{"type": "PERSPECTIVE", "evidence": "좋은 일에 쓰면 멋진 거예요"},
   {"type": "RESULT", "evidence": "마을 사람들이 배를 먹을 수 있었잖아요"}]'::jsonb,
 'VALID', 'mvp_v1', 'gemini-2.5-flash',
 '[{"type": "REASON", "evidence": "며느리가 오래 참았기 때문에"}]'::jsonb),
('dddddddd-dddd-dddd-dddd-000000000004', 'cccccccc-cccc-cccc-cccc-00000000000a',
 'PERSPECTIVE', '며느리가 일부러 그런 것은 아니라고 말함',
 '[{"type": "PERSPECTIVE", "evidence": "그러고 싶어서 그런 건 아닐 거예요"}]'::jsonb,
 'VALID', 'mvp_v1', 'gemini-2.5-flash', '[]'::jsonb)
on conflict (id) do nothing;

-- ------------------------------------------------------------
-- 14. planets / stardust_wallets - 아이당 1개씩
--    운영에서는 아이 생성 시 ChildCreatedEvent로 함께 만들어진다.
-- ------------------------------------------------------------
insert into planets (id, child_id, name, tutorial_completed) values
('eeee0000-0000-0000-0000-000000000001', 'aaaaaaaa-aaaa-aaaa-aaaa-000000000001', '지우의 행성', true),
('eeee0000-0000-0000-0000-000000000002', 'aaaaaaaa-aaaa-aaaa-aaaa-000000000002', '내 행성', false),
('eeee0000-0000-0000-0000-000000000003', 'aaaaaaaa-aaaa-aaaa-aaaa-000000000003', '내 행성', false)
on conflict (id) do nothing;

-- 지우: 완주 3 + 장면 보너스 2 + 데모 보정 20 = 누적 25, 구매 11 -> 잔액 14
insert into stardust_wallets (id, child_id, balance, total_earned) values
('ffff0000-0000-0000-0000-000000000001', 'aaaaaaaa-aaaa-aaaa-aaaa-000000000001', 14, 25),
('ffff0000-0000-0000-0000-000000000002', 'aaaaaaaa-aaaa-aaaa-aaaa-000000000002',  0,  0),
('ffff0000-0000-0000-0000-000000000003', 'aaaaaaaa-aaaa-aaaa-aaaa-000000000003',  0,  0)
on conflict (id) do nothing;

-- ------------------------------------------------------------
-- 15. stardust_transactions - 별가루 증감 이력
--    장면 보너스가 한 세션에 2건 들어간다 - scene_id를 나눠 넣어야 멱등 인덱스에 걸리지 않는다.
--    acknowledged=false인 행이 있으면 행성 진입 시 떨어지는 연출을 재생한다.
-- ------------------------------------------------------------
insert into stardust_transactions (id, wallet_id, amount, reason, session_id, scene_id, item_id, acknowledged, created_at) values
-- 완주 보상 (세션 단위, scene_id 없음)
('a1110000-0000-0000-0000-000000000001', 'ffff0000-0000-0000-0000-000000000001',
  3, 'STORY_COMPLETED', 'bbbbbbbb-bbbb-bbbb-bbbb-000000000001', null, null, true, now() - interval '3 days'),
-- 장면 보너스 2건 (유도 없이 목표 통과한 장면 3/9)
('a1110000-0000-0000-0000-000000000002', 'ffff0000-0000-0000-0000-000000000001',
  1, 'SCENE_BONUS', 'bbbbbbbb-bbbb-bbbb-bbbb-000000000001', '33333333-3333-3333-3333-000000000003', null, true, now() - interval '3 days'),
('a1110000-0000-0000-0000-000000000003', 'ffff0000-0000-0000-0000-000000000001',
  1, 'SCENE_BONUS', 'bbbbbbbb-bbbb-bbbb-bbbb-000000000001', '33333333-3333-3333-3333-000000000009', null, true, now() - interval '3 days'),
-- 시연용 보정 (미확인 상태로 두어 떨어지는 연출을 확인할 수 있게 한다)
('a1110000-0000-0000-0000-000000000004', 'ffff0000-0000-0000-0000-000000000001',
 20, 'ADMIN_ADJUST', null, null, null, false, now() - interval '1 day'),
-- 구매 (사용은 연출 대상이 아니라 바로 확인 처리)
('a1110000-0000-0000-0000-000000000005', 'ffff0000-0000-0000-0000-000000000001',
 -3, 'ITEM_PURCHASE', null, null, '44444444-4444-4444-4444-000000000001', true, now() - interval '20 hours'),
('a1110000-0000-0000-0000-000000000006', 'ffff0000-0000-0000-0000-000000000001',
 -3, 'ITEM_PURCHASE', null, null, '44444444-4444-4444-4444-000000000002', true, now() - interval '20 hours'),
('a1110000-0000-0000-0000-000000000007', 'ffff0000-0000-0000-0000-000000000001',
 -5, 'ITEM_PURCHASE', null, null, '44444444-4444-4444-4444-000000000007', true, now() - interval '19 hours')
on conflict (id) do nothing;

-- ------------------------------------------------------------
-- 16. child_story_play_counts - 이야기별 완주 횟수
--     지우는 방귀 이야기를 1회 완주했다. 다음 완주는 절반(1), 3회차부터는 지급 없음.
-- ------------------------------------------------------------
insert into child_story_play_counts (child_id, story_id, play_count) values
('aaaaaaaa-aaaa-aaaa-aaaa-000000000001', '11111111-1111-1111-1111-111111111111', 1)
on conflict (child_id, story_id) do nothing;

-- ------------------------------------------------------------
-- 17. child_items / planet_items - 보유 아이템과 배치
--     좌표는 프론트와 같은 축좌표(q, r). 원점 기준이라 음수가 나온다.
--     배치되지 않은 child_items가 보관함이다 - 풀 1개가 보관함에 남아 있다.
-- ------------------------------------------------------------
insert into child_items (id, child_id, item_id, acquired_at) values
('b2220000-0000-0000-0000-000000000001', 'aaaaaaaa-aaaa-aaaa-aaaa-000000000001',
 '44444444-4444-4444-4444-000000000001', now() - interval '20 hours'),  -- 돌
('b2220000-0000-0000-0000-000000000002', 'aaaaaaaa-aaaa-aaaa-aaaa-000000000001',
 '44444444-4444-4444-4444-000000000002', now() - interval '20 hours'),  -- 풀 (보관함)
('b2220000-0000-0000-0000-000000000003', 'aaaaaaaa-aaaa-aaaa-aaaa-000000000001',
 '44444444-4444-4444-4444-000000000007', now() - interval '19 hours')
on conflict (id) do nothing;  -- 작은나무

insert into planet_items (id, planet_id, child_item_id, placed_q, placed_r) values
('c3330000-0000-0000-0000-000000000001', 'eeee0000-0000-0000-0000-000000000001',
 'b2220000-0000-0000-0000-000000000001',  0,  0),
('c3330000-0000-0000-0000-000000000002', 'eeee0000-0000-0000-0000-000000000001',
 'b2220000-0000-0000-0000-000000000003',  1, -1)
on conflict (id) do nothing;

-- ------------------------------------------------------------
-- 18. mission_results - 미션 결과 (세션/미션당 1건)
-- ------------------------------------------------------------
insert into mission_results (id, session_id, scene_id, mission_id, mission_type, result) values
('d4440000-0000-0000-0000-000000000001', 'bbbbbbbb-bbbb-bbbb-bbbb-000000000001',
 '33333333-3333-3333-3333-000000000007', 'mission_1', 'PROBLEM_SOLVING',
 '{"answers": {"who": "며느리가", "how": "방귀를 세게 뀌어서", "why": "장대로는 닿지 않으니까", "result": "배가 우수수 떨어졌어요"}}'::jsonb),
('d4440000-0000-0000-0000-000000000002', 'bbbbbbbb-bbbb-bbbb-bbbb-000000000001',
 '33333333-3333-3333-3333-000000000009', 'mission_2', 'PERSPECTIVE_SHIFT',
 '{"cards": [{"trait": "목소리가 큰 친구", "strength": "멀리 있는 사람을 부를 수 있어요"}, {"trait": "말이 많은 친구", "strength": "재미있는 이야기를 들려줄 수 있어요"}]}'::jsonb)
on conflict (id) do nothing;

-- ------------------------------------------------------------
-- 19. post_activity_results - 말하기 후 활동 (세션당 1건)
--     card_order_seed로 카드 셔플을 고정한다 - 재진입해도 같은 순서가 나온다.
--     attempt_count=2는 한 번 틀린 뒤 맞혔다는 뜻이다.
-- ------------------------------------------------------------
insert into post_activity_results (id, session_id, card_order_seed, submitted_order,
                                   is_order_correct, attempt_count, retelling_text, completed_at) values
('e5550000-0000-0000-0000-000000000001', 'bbbbbbbb-bbbb-bbbb-bbbb-000000000001',
 'seed-banggui-0001',
 array['card_1', 'card_2', 'card_3', 'card_4', 'card_5'],
 true, 2,
 '며느리가 방귀를 참다가 배가 아팠어요. 참았던 방귀가 터져서 집이 흔들렸어요. 시아버지가 화나서 친정에 데려다주려고 했는데, 며느리가 방귀로 높은 배나무에서 배를 떨어뜨려서 모두가 좋아했어요.',
 now() - interval '3 days' + interval '22 minutes')
on conflict (id) do nothing;

-- ------------------------------------------------------------
-- 20. reports - 보호자 리포트 (세션당 1건)
--     대표 발화는 저장하지 않고 조회 시 messages에서 구성한다.
-- ------------------------------------------------------------
insert into reports (id, session_id, summary, strengths, next_focus) values
('f6660000-0000-0000-0000-000000000001', 'bbbbbbbb-bbbb-bbbb-bbbb-000000000001',
 '지우는 며느리의 마음을 먼저 헤아리고, 큰 방귀를 단점이 아니라 남을 돕는 장점으로 바꾸어 말했습니다. 이야기 전체를 순서대로 다시 들려주는 것도 잘 해냈습니다.',
 '[{"element": "EMOTION", "comment": "상대가 힘들겠다는 마음을 먼저 말해 주었어요."},
   {"element": "PERSPECTIVE", "comment": "같은 일을 다른 쪽에서 바라보고 장점으로 바꾸어 말했어요."}]'::jsonb,
 '[{"element": "REASON", "comment": "왜 그렇게 생각했는지 까닭을 덧붙이면 더 잘 전해져요."}]'::jsonb)
on conflict (session_id) do nothing;

-- ------------------------------------------------------------
-- 21. wordbook - 단어장
--     meaning이 비어 있으면 서버가 LLM으로 아이 수준의 뜻을 채운다 - 미채움 상태 1건을 둔다.
-- ------------------------------------------------------------
insert into wordbook (id, child_id, word, meaning, example_sentence, entry_type, source_scene_id) values
('17770000-0000-0000-0000-000000000001', 'aaaaaaaa-aaaa-aaaa-aaaa-000000000001',
 '며느리', '아들과 결혼한 여자를 그 부모가 부르는 말이에요.',
 '옛날 어느 마을에 방귀를 아주 크게 뀌는 며느리가 살았습니다.',
 'UNKNOWN', '33333333-3333-3333-3333-000000000001'),
('17770000-0000-0000-0000-000000000002', 'aaaaaaaa-aaaa-aaaa-aaaa-000000000001',
 '장대', null,
 '너무 높아서 긴 장대로도 배에 닿지 않았습니다.',
 'UNKNOWN', '33333333-3333-3333-3333-000000000007'),
('17770000-0000-0000-0000-000000000003', 'aaaaaaaa-aaaa-aaaa-aaaa-000000000001',
 '탐스럽다', '보기 좋고 갖고 싶을 만큼 먹음직스럽다는 뜻이에요.',
 '이 배나무는 해마다 탐스러운 배가 열립니다.',
 'FAVORITE', '33333333-3333-3333-3333-000000000007')
on conflict (id) do nothing;

-- ------------------------------------------------------------
-- 22. scene_audio - R__1_seed_content.sql 로 옮겼다
--     이 파일은 머리말대로 배포 전 삭제되거나 dev 프로파일로 빠질 데모 데이터인데,
--     사전 렌더 음성은 데모가 아니라 이야기 콘텐츠라 같이 사라지면 안 된다.
--     자리표시자였던 engine/voice/duration_ms/sentence_timings 도 그쪽에서 실측값으로 채웠다.
-- ------------------------------------------------------------


-- ------------------------------------------------------------
-- 23. 흐름 확인용 이야기 - 장면이 전부 STORY다
--     대화 턴 파이프라인(POST /utterances)이 아직 501이라 DIALOGUE 장면을 벗어날 수 없다.
--     STORY 장면만으로 이루어진 이야기를 하나 두면 마지막 장면에서 다음 장면이 없어
--     세션이 POST_ACTIVITY로 전이되고, 후속 활동과 별가루 지급까지 실제로 이어진다.
--
--     대화 파이프라인이 붙으면 이 이야기는 필요 없다. 이 파일과 함께 지운다.
-- ------------------------------------------------------------
insert into stories (id, title, summary, child_role, intro, image_url,
                     difficulty, estimated_minutes, post_activity_config, status) values
(
    '11111111-1111-1111-1111-222222222222',
    '작은 씨앗',
    '심어 둔 씨앗이 자라지 않아 걱정하던 아이가 날마다 물을 길어다 주는 이야기',
    '씨앗을 돌보는 아이',
    '마당 한구석에 씨앗을 심었어요. 그런데 아무리 기다려도 싹이 나지 않아요.',
    '/stories/seed/cover.png',
    '쉬움', 5,
    '{
      "cards": [
        { "id": "card_1", "text": "아이가 마당 한구석에 작은 씨앗을 심었어요.", "correct_order": 1 },
        { "id": "card_2", "text": "비가 오지 않아 땅이 바짝 말랐어요.", "correct_order": 2 },
        { "id": "card_3", "text": "아이가 날마다 물을 길어다 주었어요.", "correct_order": 3 },
        { "id": "card_4", "text": "어느 아침 초록 싹이 고개를 내밀었어요.", "correct_order": 4 }
      ],
      "retelling_keywords": ["씨앗", "물", "싹"]
    }'::jsonb,
    'PUBLISHED'
)
on conflict (id) do nothing;

insert into story_topics (story_id, topic_id) values
    ('11111111-1111-1111-1111-222222222222', '22222222-2222-2222-2222-000000000002')
on conflict do nothing;

insert into story_scenes (id, story_id, scene_order, scene_type, scene_description, image_url) values
('33333333-3333-3333-3333-100000000001', '11111111-1111-1111-1111-222222222222', 1, 'STORY',
 '마당 한구석에 아이가 작은 씨앗을 심었어요.
흙을 덮고 손바닥으로 토닥토닥 눌러 주었어요.
"빨리 자라렴." 아이가 속삭였어요.',
 '/stories/seed/sc_01.png'),
('33333333-3333-3333-3333-100000000002', '11111111-1111-1111-1111-222222222222', 2, 'STORY',
 '며칠이 지나도 싹은 보이지 않았어요.
비가 오지 않아 땅이 바짝 말라 있었거든요.
아이는 마른 흙을 만져 보며 한참을 앉아 있었어요.',
 '/stories/seed/sc_02.png'),
('33333333-3333-3333-3333-100000000003', '11111111-1111-1111-1111-222222222222', 3, 'STORY',
 '다음 날부터 아이는 날마다 물을 길어다 주었어요.
아침에 한 번, 저녁에 한 번씩요.
어느 아침, 흙을 뚫고 초록 싹이 고개를 내밀었어요.',
 '/stories/seed/sc_03.png')
on conflict do nothing;


-- ============================================================
-- 확인용 질의
--
--   -- 24개 테이블이 모두 찼는지
--   select 'parents' t, count(*) from parents
--   union all select 'children', count(*) from children
--   union all select 'child_consents', count(*) from child_consents
--   union all select 'refresh_tokens', count(*) from refresh_tokens
--   union all select 'stories', count(*) from stories
--   union all select 'topics', count(*) from topics
--   union all select 'story_topics', count(*) from story_topics
--   union all select 'characters', count(*) from characters
--   union all select 'story_scenes', count(*) from story_scenes
--   union all select 'story_sessions', count(*) from story_sessions
--   union all select 'messages', count(*) from messages
--   union all select 'utterance_analyses', count(*) from utterance_analyses
--   union all select 'mission_results', count(*) from mission_results
--   union all select 'post_activity_results', count(*) from post_activity_results
--   union all select 'reports', count(*) from reports
--   union all select 'wordbook', count(*) from wordbook
--   union all select 'items', count(*) from items
--   union all select 'child_items', count(*) from child_items
--   union all select 'stardust_wallets', count(*) from stardust_wallets
--   union all select 'stardust_transactions', count(*) from stardust_transactions
--   union all select 'child_story_play_counts', count(*) from child_story_play_counts
--   union all select 'planets', count(*) from planets
--   union all select 'planet_items', count(*) from planet_items
--   union all select 'scene_audio', count(*) from scene_audio;
--
--   -- 지갑 잔액이 이력 합계와 맞는지 (0건이어야 정상)
--   select w.child_id, w.balance, coalesce(sum(t.amount), 0) ledger
--   from stardust_wallets w left join stardust_transactions t on t.wallet_id = w.id
--   group by w.id having w.balance <> coalesce(sum(t.amount), 0);
--
--   -- 누적 획득이 지급 합계와 맞는지 (0건이어야 정상)
--   select w.child_id, w.total_earned, coalesce(sum(t.amount) filter (where t.amount > 0), 0) earned
--   from stardust_wallets w left join stardust_transactions t on t.wallet_id = w.id
--   group by w.id having w.total_earned <> coalesce(sum(t.amount) filter (where t.amount > 0), 0);
-- ============================================================
