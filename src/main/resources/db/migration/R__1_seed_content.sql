-- ============================================================
-- 굿퀘스천 콘텐츠 시드 - Repeatable 마이그레이션
--
-- Flyway가 파일 체크섬이 바뀔 때마다 다시 실행한다 - 콘텐츠팀이 이야기 문구/아이템/
-- 캐릭터 설정을 수정하고 앱을 재기동하면 그 변경이 반영된다. 새 파일(V3, V4...)을
-- 만들 필요 없다.
--
-- 편집 규칙
--   - 행 수정      -> 이 파일의 VALUES를 고친다. 다음 기동에서 upsert된다
--   - 행 추가      -> 새 (id, ...) 를 VALUES에 덧붙인다
--   - 행 삭제      -> 이 파일에서 지우고, FK가 걸린 데모 또는 실사용 데이터가 있다면
--                    수동 DELETE + 정합성 확인이 필요하다 (upsert만으로는 삭제 안 됨)
--
-- 담는 것: topics / stories / story_topics / story_scenes / characters / items (6종)
-- 안 담는 것: parents/children/sessions/보상 이력 등 - 데모 데이터는 R__2_seed_demo_data.sql
--
-- 실행 순서 (Flyway): 모든 V__ 완료 후 R__ 파일들이 알파벳순 실행
--   R__1_seed_content.sql (이 파일) -> R__2_seed_demo_data.sql (데모)
--
-- 원본 시드 문서의 확인 필요 값
-- - 콘텐츠 문서 문자열 ID(s_banggui_..., sc_banggui_01~09)는 uuid로 치환 (주석에 원본 병기)
-- - preferred_turns는 문서에 없어 제안값 (max_turns - 2)
-- - 대화1 요소는 콘텐츠 문서 3절 표의 배열(REASON 포함 4종)을 채택. 5절의 'EXPRESSION'은
--   사고 요소 8종에 없는 미정의 값이라 제외했다 (2026-08 확정)
-- ============================================================


-- ------------------------------------------------------------
-- 1. topics - 이야기 주제 3종
-- ------------------------------------------------------------
insert into topics (id, name, display_order) values
    ('22222222-2222-2222-2222-000000000001', '다름',      1),
    ('22222222-2222-2222-2222-000000000002', '자기이해',  2),
    ('22222222-2222-2222-2222-000000000003', '장점 발견', 3)
on conflict (id) do update set
    name = excluded.name,
    display_order = excluded.display_order;

-- ------------------------------------------------------------
-- 2. stories - 방귀 뀌는 며느리 (원본 ID: s_banggui_daughter_in_law_001)
-- ------------------------------------------------------------
insert into stories (id, title, summary, image_url, difficulty, estimated_minutes, post_activity_config, status) values
(
    '11111111-1111-1111-1111-111111111111',
    '방귀 뀌는 며느리',
    '큰 방귀를 부끄러워하던 며느리가 자신의 다름을 장점으로 바꾸는 이야기',
    '/stories/banggui/cover.jpg',
    '보통',
    20,
    '{
      "cards": [
        { "id": "card_1", "text": "며느리가 방귀를 꾹꾹 참아서 배가 아팠어요.", "correct_order": 1 },
        { "id": "card_2", "text": "며느리가 방귀를 뀌자 집안이 흔들리고 시아버지의 갓이 날아갔어요.", "correct_order": 2 },
        { "id": "card_3", "text": "시아버지가 며느리를 친정에 데려다주러 길을 나섰어요.", "correct_order": 3 },
        { "id": "card_4", "text": "며느리가 방귀로 높은 배나무의 배를 우수수 떨어뜨렸어요.", "correct_order": 4 },
        { "id": "card_5", "text": "시아버지가 사과했고, 며느리는 방귀를 특별한 힘으로 여기게 되었어요.", "correct_order": 5 }
      ],
      "retelling_keywords": ["방귀", "며느리", "배나무", "시아버지"]
    }'::jsonb,
    'PUBLISHED'
)
on conflict (id) do update set
    title = excluded.title,
    summary = excluded.summary,
    image_url = excluded.image_url,
    difficulty = excluded.difficulty,
    estimated_minutes = excluded.estimated_minutes,
    post_activity_config = excluded.post_activity_config,
    status = excluded.status;

insert into story_topics (story_id, topic_id) values
    ('11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-000000000001'),
    ('11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-000000000002'),
    ('11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-000000000003')
on conflict (story_id, topic_id) do nothing;

-- ------------------------------------------------------------
-- 3. story_scenes - 9개 장면 (도입1 + 전개4 + 대화4)
-- ------------------------------------------------------------

-- 장면 1. 도입 (원본 ID: sc_banggui_01) - 전체 화면 스토리
insert into story_scenes (id, story_id, scene_order, scene_type, scene_description, image_url) values
(
    '33333333-3333-3333-3333-000000000001',
    '11111111-1111-1111-1111-111111111111',
    1, 'STORY',
    '옛날 어느 마을에 방귀를 아주 크게 뀌는 며느리가 살았습니다. 며느리는 시집에 온 뒤로 늘 얌전하고 예의 바르게 보이고 싶었습니다. 시댁 식구들이 자신을 이상하게 볼까 봐 걱정했기 때문입니다.',
    '/stories/banggui/scenes/01_intro.jpg'
)
on conflict (id) do update set
    story_id = excluded.story_id,
    scene_order = excluded.scene_order,
    scene_type = excluded.scene_type,
    scene_description = excluded.scene_description,
    image_url = excluded.image_url;

-- 장면 2. 전개1 (sc_banggui_02) - 말 못할 사정이 있는 며느리
insert into story_scenes (id, story_id, scene_order, scene_type, scene_description, image_url) values
(
    '33333333-3333-3333-3333-000000000002',
    '11111111-1111-1111-1111-111111111111',
    2, 'STORY',
    '그래서 며느리는 방귀가 나오려고 할 때마다 꾹꾹 참았습니다. 하루도 참고, 이틀도 참고, 그렇게 오래 참다 보니 배는 점점 빵빵하게 부풀어 올랐고 얼굴은 노랗게 변했습니다. 몸도 마음도 너무 힘들었지만, 며느리는 차마 가족들에게 솔직하게 말하지 못했습니다.',
    '/stories/banggui/scenes/02_holding.jpg'
)
on conflict (id) do update set
    story_id = excluded.story_id,
    scene_order = excluded.scene_order,
    scene_type = excluded.scene_type,
    scene_description = excluded.scene_description,
    image_url = excluded.image_url;

-- 장면 3. 대화1 (sc_banggui_03) - 방귀쟁이 며느리와의 대화
-- 요소는 문서 3절 표의 ["PERSPECTIVE", "EMOTION", "REASON", "SOLUTION"]을 채택 (2026-08 확정).
-- 5절의 'EXPRESSION'은 사고 요소 8종에 없는 미정의 값이라 제외했다.
-- REASON의 기준/걱정 문구는 문서에 없어 제안값이다 - 콘텐츠팀 검수 필요.
insert into story_scenes (id, story_id, scene_order, scene_type, scene_description, conflict, image_url,
                          character_name, character_opening, character_closing,
                          scene_goal, required_elements, element_criteria, remaining_worries,
                          preferred_turns, max_turns) values
(
    '33333333-3333-3333-3333-000000000003',
    '11111111-1111-1111-1111-111111111111',
    3, 'DIALOGUE',
    '며느리가 방귀를 참는 것이 너무 힘들지만, 가족들이 자신을 이상하게 볼까 봐 걱정하고 있다.',
    '방귀를 뀌고 싶지만 가족들이 이상하게 생각할까 봐 솔직하게 말하지 못한다.',
    '/stories/banggui/scenes/03_dialogue1.jpg',
    '방귀쟁이 며느리',
    'ㅇㅇ아, 내 방귀가 너무 크다는 걸 알면 가족들이 나를 이상하게 생각하지 않을까?',
    '그래도 아직은 못 말하겠어. 조금만 더 참아 볼게.',
    '방귀를 숨기고 싶어하는 며느리의 입장을 이해하고, 공감해주며 문제를 숨기지 않고 솔직하게 말할 수 있는 용기를 준다.',
    array['PERSPECTIVE', 'EMOTION', 'REASON', 'SOLUTION'],
    '{
      "PERSPECTIVE": "며느리나 가족의 상황/입장을 헤아려 말함 (예: 가족들도 놀라긴 하겠지만 이해해 줄 거예요)",
      "EMOTION": "며느리의 감정이나 그 상황에 대한 자신의 감정을 직접 표현함 (예: 많이 힘들겠어요, 답답할 것 같아요)",
      "REASON": "참지 말고 솔직하게 말해야 하는 까닭을 설명함 (예: 계속 참으면 몸이 아프니까요)",
      "SOLUTION": "며느리가 할 수 있는 구체적인 행동을 제안함 (예: 가족들에게 솔직하게 말해 보세요)"
    }'::jsonb,
    '{
      "PERSPECTIVE": "가족들이 나를 어떻게 생각할지 아직도 무서워.",
      "EMOTION": "참자니 몸이 힘들고, 말하자니 부끄러워서 마음이 복잡해.",
      "REASON": "솔직하게 말하면 뭐가 좋아지는 걸까? 왜 말해야 하는지 아직 모르겠어.",
      "SOLUTION": "어떻게 하면 좋을지 도무지 방법을 모르겠어."
    }'::jsonb,
    2, 4
)
on conflict (id) do update set
    story_id = excluded.story_id,
    scene_order = excluded.scene_order,
    scene_type = excluded.scene_type,
    scene_description = excluded.scene_description,
    conflict = excluded.conflict,
    image_url = excluded.image_url,
    character_name = excluded.character_name,
    character_opening = excluded.character_opening,
    character_closing = excluded.character_closing,
    scene_goal = excluded.scene_goal,
    required_elements = excluded.required_elements,
    element_criteria = excluded.element_criteria,
    remaining_worries = excluded.remaining_worries,
    preferred_turns = excluded.preferred_turns,
    max_turns = excluded.max_turns;

-- 장면 4. 전개2 (sc_banggui_04) - 며느리의 엄청난 방귀
insert into story_scenes (id, story_id, scene_order, scene_type, scene_description, image_url) values
(
    '33333333-3333-3333-3333-000000000004',
    '11111111-1111-1111-1111-111111111111',
    4, 'STORY',
    '며느리는 더 이상 참을 수 없어 몰래 살짝만 방귀를 뀌려고 합니다. 하지만 오래 참았던 탓에 방귀가 크게 터져 나왔습니다. 마당의 먼지가 휘리릭 날아가고, 기왓장이 달그락거리고, 시아버지의 갓까지 휙 날아가 버렸습니다.',
    '/stories/banggui/scenes/04_bigfart.jpg'
)
on conflict (id) do update set
    story_id = excluded.story_id,
    scene_order = excluded.scene_order,
    scene_type = excluded.scene_type,
    scene_description = excluded.scene_description,
    image_url = excluded.image_url;

-- 장면 5. 대화2 (sc_banggui_05) - 시아버지와의 대화
-- 요소는 5절의 ["PERSPECTIVE", "EMPATHY", "REASON", "REQUEST"]를 채택 (2026-08 확정).
-- 3절 표의 배열은 대화1과 동일해 복붙 정황이 있고, 장면 목표("설득한다")와도 맞지 않는다.
insert into story_scenes (id, story_id, scene_order, scene_type, scene_description, conflict, image_url,
                          character_name, character_opening, character_closing,
                          scene_goal, required_elements, element_criteria, remaining_worries,
                          preferred_turns, max_turns) values
(
    '33333333-3333-3333-3333-000000000005',
    '11111111-1111-1111-1111-111111111111',
    5, 'DIALOGUE',
    '시아버지가 며느리의 요란한 방귀에 깜짝 놀라 화를 내며, 이런 며느리와는 함께 살 수 없다고 말한다.',
    '시아버지는 창피한 며느리와 함께 살 수 없다고 생각하지만, 며느리는 일부러 그런 것이 아니다.',
    '/stories/banggui/scenes/05_dialogue2.jpg',
    '시아버지',
    '아이고, 이게 무슨 일이냐! 우리 집안이 다 흔들리는구나! 이렇게 창피한 며느리와 함께 못 살겠다! 그렇지 않니?',
    '흥, 그래도 도저히 이런 며느리와는 함께 살 수 없으니 친정으로 데려다줘야겠다.',
    '시아버지가 놀란 마음을 이해하면서도, 며느리가 일부러 그런 것이 아니라 오래 참아서 힘들었던 것임을 말하고, 며느리를 따뜻하게 이해해 달라고 설득한다.',
    array['PERSPECTIVE', 'EMPATHY', 'REASON', 'REQUEST'],
    '{
      "PERSPECTIVE": "시아버지 또는 며느리의 상황/입장을 고려해 말함 (예: 며느리도 일부러 그런 게 아니에요)",
      "EMPATHY": "며느리의 부끄럽고 속상한 마음을 이해하고 배려함 (예: 며느리가 얼마나 창피하고 속상하겠어요)",
      "REASON": "며느리가 방귀를 오래 참았던 까닭이나 크게 터진 까닭을 설명함 (예: 오래 참아서 그런 거예요)",
      "REQUEST": "시아버지에게 며느리를 이해해 달라고 구체적으로 요청함 (예: 한 번만 며느리 이야기를 들어 주세요)"
    }'::jsonb,
    '{
      "PERSPECTIVE": "며느리가 대체 왜 그런 것인지 도무지 알 수가 없구나.",
      "EMPATHY": "다들 놀라서 며느리의 마음까지는 생각할 겨를이 없었구나.",
      "REASON": "어째서 그렇게 요란한 방귀를 뀌게 되었는지 까닭을 모르겠구나.",
      "REQUEST": "그래서 나더러 어쩌란 말인지 모르겠구나."
    }'::jsonb,
    3, 5
)
on conflict (id) do update set
    story_id = excluded.story_id,
    scene_order = excluded.scene_order,
    scene_type = excluded.scene_type,
    scene_description = excluded.scene_description,
    conflict = excluded.conflict,
    image_url = excluded.image_url,
    character_name = excluded.character_name,
    character_opening = excluded.character_opening,
    character_closing = excluded.character_closing,
    scene_goal = excluded.scene_goal,
    required_elements = excluded.required_elements,
    element_criteria = excluded.element_criteria,
    remaining_worries = excluded.remaining_worries,
    preferred_turns = excluded.preferred_turns,
    max_turns = excluded.max_turns;

-- 장면 6. 전개3 (sc_banggui_06) - 높은 배나무를 만난 시아버지와 며느리
insert into story_scenes (id, story_id, scene_order, scene_type, scene_description, image_url) values
(
    '33333333-3333-3333-3333-000000000006',
    '11111111-1111-1111-1111-111111111111',
    6, 'STORY',
    '한참 걷다 보니 아랫마을 길가에 아주 높은 배나무가 한 그루 서 있었습니다. 나무 꼭대기에는 노랗고 탐스러운 배들이 주렁주렁 매달려 있었습니다. 시아버지는 배를 보자 군침이 돌았습니다. 마침 아랫마을 사람들도 그 배를 먹고 싶어 했지만, 나무가 너무 높아 아무도 딸 수 없었습니다.',
    '/stories/banggui/scenes/06_peartree.jpg'
)
on conflict (id) do update set
    story_id = excluded.story_id,
    scene_order = excluded.scene_order,
    scene_type = excluded.scene_type,
    scene_description = excluded.scene_description,
    image_url = excluded.image_url;

-- 장면 7. 대화3 (sc_banggui_07) - 마을 이장과의 대화 + 미션1
insert into story_scenes (id, story_id, scene_order, scene_type, scene_description, conflict, image_url,
                          character_name, character_opening, character_closing,
                          scene_goal, required_elements, element_criteria, remaining_worries,
                          mission_config, preferred_turns, max_turns) values
(
    '33333333-3333-3333-3333-000000000007',
    '11111111-1111-1111-1111-111111111111',
    7, 'DIALOGUE',
    '마을 이장이 너무 높아 아무도 딸 수 없는 배나무를 두고 좋은 방법이 없는지 고민하고 있다.',
    '탐스러운 배가 열렸지만 나무가 너무 높아 긴 장대로도 닿지 않고, 올라갈 수도 없다.',
    '/stories/banggui/scenes/07_dialogue3.jpg',
    '마을 이장',
    '이 배나무는 해마다 탐스러운 배가 열리지만, 너무 높아서 아무도 딸 수가 없었단다. 무슨 뾰족한 방법이 없겠는가?',
    '아이고, 방귀 뀌는 며느리 덕분에 온 마을이 배 잔치를 할 수 있겠구려, 고맙소!',
    '높은 배나무의 배를 떨어뜨릴 방법을 생각하고, 며느리의 큰 방귀를 안전하게 사용할 수 있는 해결책을 제안한다.',
    array['SOLUTION', 'REASON', 'REQUEST', 'RESULT'],
    '{
      "SOLUTION": "배를 떨어뜨릴 구체적인 방법을 제시함 (예: 며느리의 방귀로 배나무를 흔들어요)",
      "REASON": "그 방법이 가능한 까닭을 설명함 (예: 며느리 방귀는 지붕도 흔들 만큼 힘이 세니까요)",
      "REQUEST": "며느리에게 부탁하는 방법이나 사람들의 대피를 구체적으로 요청함 (예: 며느리에게 도와달라고 부탁해요, 사람들은 옆으로 피해요)",
      "RESULT": "그 방법을 쓰면 어떤 일이 생길지 예상함 (예: 배가 우수수 떨어져서 모두 나눠 먹을 수 있어요)"
    }'::jsonb,
    '{
      "SOLUTION": "무슨 수로 저 높은 배를 딴단 말인가, 뾰족한 방법이 떠오르지 않는구려.",
      "REASON": "그 방법이 정말 되겠소? 어째서 가능한지 궁금하구려.",
      "REQUEST": "누구에게 어떻게 부탁해야 할지, 사람들은 어찌해야 할지 모르겠구려.",
      "RESULT": "잘못하다가 사람이 다치거나 배가 다 깨지지는 않겠소?"
    }'::jsonb,
    '{
      "mission_id": "mission_1",
      "mission_type": "PROBLEM_SOLVING",
      "name": "높이 있는 배 따기 미션",
      "purpose": "높은 배나무의 배를 떨어뜨리기 위해 며느리의 큰 방귀를 안전하게 사용하는 방법을 구성한다.",
      "check_points": ["무엇을 사용할 것인지", "왜 그 방법이 가능한지", "며느리에게 어떻게 부탁할 것인지", "그 결과 어떤 일이 생길지"],
      "questions": [
        {"key": "tool", "label": "무엇을 사용할 것인지"},
        {"key": "safety", "label": "왜 그 방법이 가능한지"},
        {"key": "request", "label": "며느리에게 어떻게 부탁할 것인지"},
        {"key": "expectedResult", "label": "그 결과 어떤 일이 생길지"}
      ],
      "exposure_principle": "대화 시작과 동시에 보여주지 않고, 해결 방법을 실제로 구성해야 하는 시점에 노출한다.",
      "exposure_conditions": [
        "아이가 며느리의 방귀를 활용할 수 있다고 제안한 경우",
        "아이가 해결 방향은 말했지만 방법이 구체적이지 않은 경우",
        "2회 이상 대화했지만 실행 방법이 나오지 않은 경우",
        "캐릭터 질문만으로 해결 방법을 구체화하기 어려운 경우"
      ]
    }'::jsonb,
    3, 5
)
on conflict (id) do update set
    story_id = excluded.story_id,
    scene_order = excluded.scene_order,
    scene_type = excluded.scene_type,
    scene_description = excluded.scene_description,
    conflict = excluded.conflict,
    image_url = excluded.image_url,
    character_name = excluded.character_name,
    character_opening = excluded.character_opening,
    character_closing = excluded.character_closing,
    scene_goal = excluded.scene_goal,
    required_elements = excluded.required_elements,
    element_criteria = excluded.element_criteria,
    remaining_worries = excluded.remaining_worries,
    mission_config = excluded.mission_config,
    preferred_turns = excluded.preferred_turns,
    max_turns = excluded.max_turns;

-- 장면 8. 전개4 (sc_banggui_08) - 후회하고 사과하는 시아버지
insert into story_scenes (id, story_id, scene_order, scene_type, scene_description, image_url) values
(
    '33333333-3333-3333-3333-000000000008',
    '11111111-1111-1111-1111-111111111111',
    8, 'STORY',
    '시아버지는 며느리의 방귀가 시끄럽고 별난 것이 아니라, 모두를 도울 수 있는 특별한 힘이라는 것을 깨닫습니다. 자신이 며느리를 구박했던 일을 후회하고 사과합니다.',
    '/stories/banggui/scenes/08_apology.jpg'
)
on conflict (id) do update set
    story_id = excluded.story_id,
    scene_order = excluded.scene_order,
    scene_type = excluded.scene_type,
    scene_description = excluded.scene_description,
    image_url = excluded.image_url;

-- 장면 9. 대화4 (sc_banggui_09) - 방귀쟁이 며느리와의 마지막 대화 + 미션2
insert into story_scenes (id, story_id, scene_order, scene_type, scene_description, conflict, image_url,
                          character_name, character_opening, character_closing,
                          scene_goal, required_elements, element_criteria, remaining_worries,
                          mission_config, preferred_turns, max_turns) values
(
    '33333333-3333-3333-3333-000000000009',
    '11111111-1111-1111-1111-111111111111',
    9, 'DIALOGUE',
    '며느리가 자신의 방귀가 누군가에게 도움이 될 수 있다는 것을 처음 알고, 이제는 부끄러워하지 않아도 되는지 아이에게 묻는다.',
    '자신의 특징이 도움이 된다는 것을 알았지만, 아직 부끄러운 마음이 남아 있다.',
    '/stories/banggui/scenes/09_dialogue4.jpg',
    '방귀쟁이 며느리',
    'ㅇㅇ이 덕분에 내 방귀가 누군가에게 도움이 될 수 있다는 걸 처음 알았어. 이제는 방귀 소리가 큰 걸 부끄러워하지 않아도 될까?',
    '이제는 부끄러워하며 숨기지 않고, 조심해서 좋은 일에 써 볼게.',
    '다름을 인정하고, 자신의 특징을 긍정적으로 받아들이는 태도를 말한다.',
    array['EMOTION', 'PERSPECTIVE', 'RESULT', 'SOLUTION'],
    '{
      "EMOTION": "며느리의 달라진 마음이나 그에 대한 자신의 감정을 표현함 (예: 이제 당당해져서 기뻐요)",
      "PERSPECTIVE": "특징을 다른 관점에서 바라봄 (예: 부끄러운 게 아니라 특별한 힘이에요)",
      "RESULT": "특징을 좋은 일에 쓰면 생길 결과를 예상함 (예: 높은 곳의 열매를 딸 때 도와줄 수 있어요)",
      "SOLUTION": "특징을 좋게 쓰는 구체적인 방법을 제안함 (예: 미리 알려 주고 사람 없는 쪽에서 뀌면 돼요)"
    }'::jsonb,
    '{
      "EMOTION": "그래도 소리가 크게 나면 아직은 창피한 마음이 들 것 같아.",
      "PERSPECTIVE": "다른 사람들도 내 방귀를 좋게 봐 줄까?",
      "RESULT": "함부로 뀌면 또 사람들이 깜짝 놀라지 않을까?",
      "SOLUTION": "언제 어떻게 써야 좋을지 아직 잘 모르겠어."
    }'::jsonb,
    '{
      "mission_id": "mission_2",
      "mission_type": "PERSPECTIVE_SHIFT",
      "name": "친구들의 단점을 장점으로 바꾸기 미션",
      "purpose": "친구나 주변 사람의 특징을 다른 관점에서 바라보고, 단점처럼 보이는 특징을 장점이나 가능성으로 바꾸어 말한다.",
      "cards": [
        {"key": "loud_voice", "label": "목소리가 큰 친구", "image_url": null, "template": "목소리가 큰 친구는 ___ 할 수 있어요."},
        {"key": "many_questions", "label": "질문이 많은 친구", "image_url": null, "template": "질문이 많은 친구는 ___ 할 수 있어요."},
        {"key": "strong", "label": "힘이 센 친구", "image_url": null, "template": "힘이 센 친구는 ___ 할 수 있어요."},
        {"key": "quiet", "label": "조용한 친구", "image_url": null, "template": "조용한 친구는 ___ 할 수 있어요."}
      ],
      "exposure_principle": "처음부터 보여주면 정해진 답을 찾으려 할 수 있으므로, 며느리의 질문에 먼저 자신의 생각을 말한 뒤 노출한다.",
      "exposure_flow": [
        "아이가 며느리에게 자신의 특징을 부끄러워하지 않아도 된다고 말함",
        "자신의 특징을 긍정하는 이유 확인",
        "다른 사람의 특징으로 관점을 확장할 필요가 생김",
        "미션2 노출"
      ],
      "examples": [
        "목소리가 큰 친구는 멀리 있는 사람을 부를 수 있어요.",
        "질문이 많은 친구는 새로운 생각을 찾을 수 있어요.",
        "힘이 센 친구는 무거운 물건을 옮길 때 도울 수 있어요.",
        "조용한 친구는 다른 사람의 말을 잘 들어 줄 수 있어요."
      ]
    }'::jsonb,
    2, 4
)
on conflict (id) do update set
    story_id = excluded.story_id,
    scene_order = excluded.scene_order,
    scene_type = excluded.scene_type,
    scene_description = excluded.scene_description,
    conflict = excluded.conflict,
    image_url = excluded.image_url,
    character_name = excluded.character_name,
    character_opening = excluded.character_opening,
    character_closing = excluded.character_closing,
    scene_goal = excluded.scene_goal,
    required_elements = excluded.required_elements,
    element_criteria = excluded.element_criteria,
    remaining_worries = excluded.remaining_worries,
    mission_config = excluded.mission_config,
    preferred_turns = excluded.preferred_turns,
    max_turns = excluded.max_turns;

-- ------------------------------------------------------------
-- 4. characters - 캐릭터 레지스트리 (3명)
--    장면에 흩어져 있던 캐릭터 속성을 모은다. personality는 장면과 무관한 공통 성격이고,
--    장면마다 달라지는 부분은 story_scenes.scene_stance에 둔다.
--    캐릭터 LLM 입력은 personality + scene_stance 조합이다 (기존 character_persona는 V5에서 제거).
-- ------------------------------------------------------------
insert into characters (id, story_id, character_key, name, personality, guidance_style,
                        tts_voice, tts_style, tts_gender, expression_keys) values
(
    '55555555-5555-5555-5555-000000000001',
    '11111111-1111-1111-1111-111111111111',
    'daughter_in_law', '방귀쟁이 며느리',
    '남을 많이 의식해 조심스럽지만 따뜻한 인물. 가족에게 폐를 끼치거나 이상하게 보이는 것을 걱정하고, 자신의 불편함보다 주변 사람의 반응을 먼저 생각한다. 부끄러움이 많아 자신의 특별한 특징을 쉽게 드러내지 못한다.',
    '아이를 가르치듯 묻지 않는다. 아직 풀리지 않은 걱정을 혼잣말처럼 흘려 아이가 먼저 말하고 싶게 만든다.',
    null,   -- TODO: TTS 엔진 확정 후 TTS초안 manifest.json의 실제 보이스로 채운다
    '20대 후반 여성. 조심스럽고 낮은 목소리로, 부끄러움이 묻어나게 읽는다.',
    'FEMALE',
    array['NEUTRAL', 'WORRIED', 'SAD', 'SURPRISED', 'RELIEVED', 'HAPPY']
),
(
    '55555555-5555-5555-5555-000000000002',
    '11111111-1111-1111-1111-111111111111',
    'father_in_law', '시아버지',
    '체면을 중시하고 호들갑스러우며, 고집이 있지만 익살스러운 어른. 집안의 체면과 다른 사람의 시선을 중요하게 생각한다. 놀라면 반응이 크고 과장되어 웃음을 준다. 아이의 말에 반박하거나 따져 묻기도 하지만 호통치거나 위압적으로 대하지 않으며, 일리가 있으면 인정하지만 곧바로 결정을 뒤집지는 않는다. 갈등 상황에서도 동화적인 재미를 유지한다.',
    '납득이 안 된 부분을 되물어 아이가 까닭을 더 설명하게 만든다. 다그치지 않고 투덜대듯 말한다.',
    null,
    '60대 남성. 굵고 낮은 목소리로, 놀랄 때는 과장되게 읽는다.',
    'MALE',
    array['NEUTRAL', 'SURPRISED', 'WORRIED', 'RELIEVED', 'HAPPY']
),
(
    '55555555-5555-5555-5555-000000000003',
    '11111111-1111-1111-1111-111111111111',
    'village_chief', '마을 이장',
    '마을 일을 챙기며 현실적인 문제 해결을 중요하게 생각하는, 친근하고 반응이 큰 어른. 마을의 불편이나 문제를 먼저 살피고 사람들의 의견을 모은다. 특이하거나 낯선 방법이라도 실제로 도움이 된다면 받아들인다. 해결 방법만큼 주변 사람들이 다치지 않는지도 신경 쓰고, 좋은 결과는 편견 없이 감탄하고 칭찬한다.',
    '아직 해결되지 않은 걸림돌을 짚어 아이가 방법을 더 구체적으로 말하게 만든다.',
    null,
    '50대 남성. 밝고 시원시원한 목소리로, 감탄할 때 크게 읽는다.',
    'MALE',
    array['NEUTRAL', 'SURPRISED', 'WORRIED', 'HAPPY']
)
on conflict (id) do update set
    story_id = excluded.story_id,
    character_key = excluded.character_key,
    name = excluded.name,
    personality = excluded.personality,
    guidance_style = excluded.guidance_style,
    tts_voice = excluded.tts_voice,
    tts_style = excluded.tts_style,
    tts_gender = excluded.tts_gender,
    expression_keys = excluded.expression_keys;

-- ------------------------------------------------------------
-- 5. story_scenes 보강 - 캐릭터 참조 / 장면별 입장 / STT 고유명사 힌트
--    scene_stance는 페르소나 문장 중 "이 장면에서는 ..." 부분만 떼어 옮긴 것이다.
--    proper_nouns는 아동 발화에서 오인식이 잦은 낱말이라 STT 디코딩 힌트로 넘긴다.
-- ------------------------------------------------------------
update story_scenes set
    character_id = '55555555-5555-5555-5555-000000000001',
    scene_stance = '방귀를 오래 참아 몸이 힘든데도 걱정 때문에 말하지 못하는 상태다.'
where id = '33333333-3333-3333-3333-000000000003';

update story_scenes set
    character_id = '55555555-5555-5555-5555-000000000002',
    scene_stance = '며느리의 요란한 방귀에 깜짝 놀라 화가 났고, 이런 며느리와는 함께 살 수 없다고 생각한다.'
where id = '33333333-3333-3333-3333-000000000005';

update story_scenes set
    character_id = '55555555-5555-5555-5555-000000000003',
    scene_stance = '너무 높아 아무도 딸 수 없는 배나무를 두고 좋은 방법이 없는지 고민한다.'
where id = '33333333-3333-3333-3333-000000000007';

update story_scenes set
    character_id = '55555555-5555-5555-5555-000000000001',
    scene_stance = '자신의 특징이 무조건 나쁜 것만은 아니라는 점을 알아가며 조금씩 당당해지고 있지만, 아직 부끄러움이 남아 있는 상태다.'
where id = '33333333-3333-3333-3333-000000000009';

-- 이야기 전체 공통 고유명사 + 장면별 추가분
update story_scenes set proper_nouns = array['며느리', '시아버지', '방귀']
where story_id = '11111111-1111-1111-1111-111111111111';

update story_scenes set proper_nouns = array['며느리', '시아버지', '방귀', '친정', '갓']
where id = '33333333-3333-3333-3333-000000000005';

update story_scenes set proper_nouns = array['며느리', '방귀', '이장', '배나무', '장대']
where id = '33333333-3333-3333-3333-000000000007';

-- ------------------------------------------------------------
-- 6. items - 꾸미기 아이템 마스터 16종
--    가격: 소품 3 / 중형 5 / 대형/동물 10
--    해금: ALWAYS 12 / STARDUST_CUMULATIVE 3 / STORY_COMPLETE 1
--
--    토끼/거북이는 후속 이야기(토끼전 등) 완주 보상으로 예약된 것이지만,
--    아직 그 이야기가 없어 unlock_story_id를 채울 수 없다  - 
--    스키마 check가 STORY_COMPLETE에 대상 이야기를 요구하므로 누적 해금으로 둔다.
--    TODO: 후속 이야기가 들어오면 STORY_COMPLETE + unlock_story_id로 바꾼다.
--
--    model_url/thumbnail_url은 Kenney CC0 에셋 업로드 후 실제 경로로 교체한다(플레이스홀더).
-- ------------------------------------------------------------
insert into items (id, name, category, price, unlock_type, unlock_story_id, unlock_stardust_total,
                   model_url, thumbnail_url, display_order) values
-- 지형 소품 6종 (항상 열림, 3)
('44444444-4444-4444-4444-000000000001', '돌',       'TERRAIN_PROP',  3, 'ALWAYS', null, null, '/items/models/rock.glb',      '/items/thumbs/rock.png',       1),
('44444444-4444-4444-4444-000000000002', '풀',       'TERRAIN_PROP',  3, 'ALWAYS', null, null, '/items/models/grass.glb',     '/items/thumbs/grass.png',      2),
('44444444-4444-4444-4444-000000000003', '꽃',       'TERRAIN_PROP',  3, 'ALWAYS', null, null, '/items/models/flower.glb',    '/items/thumbs/flower.png',     3),
('44444444-4444-4444-4444-000000000004', '버섯',     'TERRAIN_PROP',  3, 'ALWAYS', null, null, '/items/models/mushroom.glb',  '/items/thumbs/mushroom.png',   4),
('44444444-4444-4444-4444-000000000005', '바위',     'TERRAIN_PROP',  3, 'ALWAYS', null, null, '/items/models/boulder.glb',   '/items/thumbs/boulder.png',    5),
('44444444-4444-4444-4444-000000000006', '그루터기', 'TERRAIN_PROP',  3, 'ALWAYS', null, null, '/items/models/stump.glb',     '/items/thumbs/stump.png',      6),
-- 식물 4종 (항상 열림, 5)
('44444444-4444-4444-4444-000000000007', '작은나무', 'PLANT',         5, 'ALWAYS', null, null, '/items/models/tree_small.glb','/items/thumbs/tree_small.png', 7),
('44444444-4444-4444-4444-000000000008', '큰나무',   'PLANT',         5, 'ALWAYS', null, null, '/items/models/tree_large.glb','/items/thumbs/tree_large.png', 8),
('44444444-4444-4444-4444-000000000009', '꽃덤불',   'PLANT',         5, 'ALWAYS', null, null, '/items/models/bush.glb',      '/items/thumbs/bush.png',       9),
('44444444-4444-4444-4444-00000000000a', '야자수',   'PLANT',         5, 'ALWAYS', null, null, '/items/models/palm.glb',      '/items/thumbs/palm.png',      10),
-- 구조물 3종
('44444444-4444-4444-4444-00000000000b', '울타리',   'STRUCTURE',     5, 'ALWAYS', null, null, '/items/models/fence.glb',     '/items/thumbs/fence.png',     11),
('44444444-4444-4444-4444-00000000000c', '표지판',   'STRUCTURE',     5, 'ALWAYS', null, null, '/items/models/sign.glb',      '/items/thumbs/sign.png',      12),
('44444444-4444-4444-4444-00000000000d', '집',       'STRUCTURE',    10, 'STARDUST_CUMULATIVE', null, 15, '/items/models/house.glb', '/items/thumbs/house.png', 13),
-- 동물 3종
('44444444-4444-4444-4444-00000000000e', '강아지',   'ANIMAL',       10, 'STORY_COMPLETE', '11111111-1111-1111-1111-111111111111', null, '/items/models/dog.glb', '/items/thumbs/dog.png', 14),
('44444444-4444-4444-4444-00000000000f', '토끼',     'ANIMAL',       10, 'STARDUST_CUMULATIVE', null, 30, '/items/models/rabbit.glb', '/items/thumbs/rabbit.png', 15),
('44444444-4444-4444-4444-000000000010', '거북이',   'ANIMAL',       10, 'STARDUST_CUMULATIVE', null, 50, '/items/models/turtle.glb', '/items/thumbs/turtle.png', 16)
on conflict (id) do update set
    name = excluded.name,
    category = excluded.category,
    price = excluded.price,
    unlock_type = excluded.unlock_type,
    unlock_story_id = excluded.unlock_story_id,
    unlock_stardust_total = excluded.unlock_stardust_total,
    model_url = excluded.model_url,
    thumbnail_url = excluded.thumbnail_url,
    display_order = excluded.display_order;