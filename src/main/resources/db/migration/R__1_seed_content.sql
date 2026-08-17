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
-- - preferred_turns는 전 장면 2로 확정 (2026-08). 문서에 수치가 없고(임계값은 운영 정책
--   위임) 역할이 "한 발화에 요소를 다 채워도 최소 두 번은 주고받게 하는 하한"뿐이라
--   장면 분량을 정하는 요소/max_turns와 달리 장면별로 다를 이유가 없다
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
--
-- child_role / intro 는 상세 화면(선택-03)이 쓰는 값이다. MVP 요건의
-- "상세 화면에 이야기 도입, 상황, 아이 역할 표시"가 데이터베이스_설계 3.1 에서
-- 이 두 컬럼으로 내려온 것이다. 그동안 비어 있어서 상세 화면의 역할·도입이
-- 통째로 안 보였다 (장면 9개가 다 있는 이야기인데도).
--   - intro : 콘텐츠 문서 1절 '도입' 단락을 아이 눈높이 두 문장으로 줄였다.
--             앞 문장이 도입(누가), 뒤 문장이 상황(지금 무슨 일)이다.
--   - child_role : **콘텐츠팀 검수 필요.** 문서에 아이 역할을 명시한 곳이 없어
--             대화 4장면에서 역산한 제안값이다. 아이는 며느리(대화1·4)뿐 아니라
--             시아버지(대화2)·마을 이장(대화3)과도 이야기하므로 한 인물의 친구가
--             아니라 '마을 사람들의 고민을 듣는 상대'로 잡았다.
-- ------------------------------------------------------------
insert into stories (id, title, summary, child_role, intro, image_url, difficulty,
                     estimated_minutes, post_activity_config, status, display_order) values
(
    '11111111-1111-1111-1111-111111111111',
    '방귀 뀌는 며느리',
    '큰 방귀를 부끄러워하던 며느리가 자신의 다름을 장점으로 바꾸는 이야기',
    '마을 사람들의 고민을 들어주는 아이',
    '옛날 어느 마을에, 방귀를 아주 크게 뀌는 며느리가 살았어요. 이상하게 볼까 봐 걱정이 되어서, 며느리는 방귀를 꾹꾹 참고 있어요.',
    '/stories/banggui/cover.jpg',
    '보통',
    20,
    -- 카드는 프론트에 그림(assets/images/recap/banggui/scene_N.webp)이 있는 4장에 맞춘다.
    -- id의 숫자가 그림 파일 번호와 대응하므로 순서와 개수를 바꿀 때 그림도 함께 봐야 한다.
    -- 핵심 단어는 카드와 1:1로 짝지어 2단계에서 각 카드에 붙는다.
    '{
      "cards": [
        { "id": "card_1", "text": "며느리가 방귀를 참느라 시무룩하게 서 있어요", "correct_order": 1 },
        { "id": "card_2", "text": "며느리의 방귀에 시아버지의 갓이 날아가 시아버지가 화를 냈어요", "correct_order": 2 },
        { "id": "card_3", "text": "며느리가 방귀로 배나무의 배를 우수수 떨어뜨렸어요", "correct_order": 3 },
        { "id": "card_4", "text": "마을 사람들이 배를 얻고 며느리에게 고마워했어요", "correct_order": 4 }
      ],
      "retelling_keywords": ["참다", "쫓겨나다", "떨어뜨리다", "자신감"]
    }'::jsonb,
    'PUBLISHED',
    -- 목록과 홈의 첫 칸. 장면 대본과 음성이 다 있어 끝까지 진행되는 유일한 이야기라
    -- 아이가 들어오자마자 이걸 만나야 한다. 나머지는 R__3에서 10 단위로 뒤에 놓는다.
    1
)
on conflict (id) do update set
    title = excluded.title,
    summary = excluded.summary,
    child_role = excluded.child_role,
    intro = excluded.intro,
    image_url = excluded.image_url,
    difficulty = excluded.difficulty,
    estimated_minutes = excluded.estimated_minutes,
    post_activity_config = excluded.post_activity_config,
    status = excluded.status,
    display_order = excluded.display_order;

insert into story_topics (story_id, topic_id) values
    ('11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-000000000001'),
    ('11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-000000000002'),
    ('11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-000000000003')
on conflict (story_id, topic_id) do nothing;

-- ------------------------------------------------------------
-- 3. story_scenes - 9개 장면 (도입1 + 전개4 + 대화4)
-- ------------------------------------------------------------

-- 장면 1. 도입 (원본 ID: sc_banggui_01) - 전체 화면 스토리
insert into story_scenes (id, story_id, scene_order, scene_type, scene_description, image_url, video_url) values
(
    '33333333-3333-3333-3333-000000000001',
    '11111111-1111-1111-1111-111111111111',
    1, 'STORY',
    E'옛날 어느 마을에 방귀를 아주 크게 뀌는 며느리가 살았습니다.\n며느리는 시집에 온 뒤로 늘 얌전하고 예의 바르게 보이고 싶었습니다.\n시댁 식구들이 자신을 이상하게 볼까 봐 걱정했기 때문입니다.',
    '/stories/banggui/scenes/01_intro.jpg',
    '/stories/banggui/scenes/01_intro_loop.mp4'
)
on conflict (id) do update set
    story_id = excluded.story_id,
    scene_order = excluded.scene_order,
    scene_type = excluded.scene_type,
    scene_description = excluded.scene_description,
    image_url = excluded.image_url,
    video_url = excluded.video_url;

-- 장면 2. 전개1 (sc_banggui_02) - 말 못할 사정이 있는 며느리
insert into story_scenes (id, story_id, scene_order, scene_type, scene_description, image_url, video_url) values
(
    '33333333-3333-3333-3333-000000000002',
    '11111111-1111-1111-1111-111111111111',
    2, 'STORY',
    E'그래서 며느리는 방귀가 나오려고 할 때마다 꾹꾹 참았습니다.\n하루도 참고, 이틀도 참고, 그렇게 오래 참다 보니 배는 점점 빵빵하게 부풀어 올랐고 얼굴은 노랗게 변했습니다.\n몸도 마음도 너무 힘들었지만, 며느리는 차마 가족들에게 솔직하게 말하지 못했습니다.',
    '/stories/banggui/scenes/02_holding.jpg',
    '/stories/banggui/scenes/02_holding_loop.mp4'
)
on conflict (id) do update set
    story_id = excluded.story_id,
    scene_order = excluded.scene_order,
    scene_type = excluded.scene_type,
    scene_description = excluded.scene_description,
    image_url = excluded.image_url,
    video_url = excluded.video_url;

-- 장면 3. 대화1 (sc_banggui_03) - 방귀쟁이 며느리와의 대화
-- 요소는 문서 3절 표의 ["PERSPECTIVE", "EMOTION", "REASON", "SOLUTION"] 4종이다.
-- 한때 검수 의견으로 REASON을 제외했었으나(#44, #45) 2026-08-15 회의에서 REASON
-- 포함으로 재확정되어 복원했다. 문서 5절 상세의 'EXPRESSION'은 사고 요소 8종에
-- 없는 미정의 값이라 계속 제외한다.
-- REASON의 기준/걱정 문구는 확정 문구가 아직 없어 제안값이다 - 확정되면 교체한다.
insert into story_scenes (id, story_id, scene_order, scene_type, scene_description, conflict, image_url, video_url,
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
    -- 대화 장면은 루프본을 쓴다. 아이가 말하는 동안 계속 돌아야 하는데
    -- 5초본을 반복하면 이음매에서 튄다 (5초본 03_dialogue1.mp4도 함께 배치돼 있다).
    '/stories/banggui/scenes/03_dialogue1_loop.mp4',
    '방귀쟁이 며느리',
    '내 방귀가 너무 크다는 걸 알면 가족들이 나를 이상하게 생각하지 않을까?',
    '그래도 아직은 못 말하겠어. 조금만 더 참아 볼게.',
    '방귀를 숨기고 싶어하는 며느리의 입장을 이해하고, 공감해주며 문제를 숨기지 않고 솔직하게 말할 수 있는 용기를 준다.',
    array['PERSPECTIVE', 'EMOTION', 'REASON', 'SOLUTION'],
    '{
      "PERSPECTIVE": "며느리 또는 가족의 입장과 반응을 헤아려 말함. 충족 예: \"가족들도 처음에는 놀라지만 이해해 줄 거예요\". 미충족 예: \"방귀는 원래 나오는 거예요\". \"그냥 방귀 뀌어요\"처럼 가족의 입장을 고려하지 않은 말은 인정하지 않는다.",
      "EMOTION": "며느리의 감정이나 아이 자신의 감정을 직접 표현함. 충족 예: \"계속 참으면 많이 힘들 것 같아요\". 미충족 예: \"괜찮아요\". 놀리거나 비난하는 말은 감정 단어가 들어가더라도 인정하지 않는다.",
      "REASON": "참지 말고 솔직하게 말해야 하는 까닭을 설명함 (예: 계속 참으면 몸이 아프니까요)",
      "SOLUTION": "현재의 어려움을 줄일 수 있는 구체적인 행동을 제안함. 충족 예: \"가족들에게 먼저 솔직하게 말해 보세요\". 미충족 예: \"어떻게든 해봐요\". \"계속 참아요\"는 행동이지만 현재 문제를 반복·악화시키므로 인정하지 않는다. \"그냥 방귀 뀌어요\"는 몸의 불편을 해소하는 구체적 행동이므로 인정한다."
    }'::jsonb,
    -- 2026-08-16 걱정 문안 개정: 유도-판정 정합(교차 배선·예/아니오·정답 선점 제거). 콘텐츠팀 검수 전 제안값.
    '{
      "PERSPECTIVE": "가족들이 나를 어떻게 생각할지 아직도 무서워.",
      "EMOTION": "이대로 매일 꾹 참고만 지내면 내 마음이 어떻게 될까? 나도 잘 모르겠어.",
      "REASON": "솔직하게 말하면 뭐가 좋아지는 걸까? 왜 말해야 하는지 아직 모르겠어.",
      "SOLUTION": "참는 것 말고 가족들 앞에서 내가 해 볼 수 있는 일이 있을까? 도무지 떠오르지 않아."
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
    video_url = excluded.video_url,
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
insert into story_scenes (id, story_id, scene_order, scene_type, scene_description, image_url, video_url) values
(
    '33333333-3333-3333-3333-000000000004',
    '11111111-1111-1111-1111-111111111111',
    4, 'STORY',
    E'며느리는 더 이상 참을 수 없어 몰래 살짝만 방귀를 뀌려고 합니다.\n하지만 오래 참았던 탓에 방귀가 크게 터져 나왔습니다.\n마당의 먼지가 휘리릭 날아가고, 기왓장이 달그락거리고, 시아버지의 갓까지 휙 날아가 버렸습니다.',
    '/stories/banggui/scenes/04_bigfart.jpg',
    '/stories/banggui/scenes/04_bigfart_loop.mp4'
)
on conflict (id) do update set
    story_id = excluded.story_id,
    scene_order = excluded.scene_order,
    scene_type = excluded.scene_type,
    scene_description = excluded.scene_description,
    image_url = excluded.image_url,
    video_url = excluded.video_url;

-- 장면 5. 대화2 (sc_banggui_05) - 시아버지와의 대화
-- 요소는 5절의 ["PERSPECTIVE", "EMPATHY", "REASON", "REQUEST"]를 채택 (2026-08 확정).
-- 3절 표의 배열은 대화1과 동일해 복붙 정황이 있고, 장면 목표("설득한다")와도 맞지 않는다.
insert into story_scenes (id, story_id, scene_order, scene_type, scene_description, conflict, image_url, video_url,
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
    '/stories/banggui/scenes/05_dialogue2_loop.mp4',
    '시아버지',
    '아이고 이게 무슨 일이냐! 우리 집안이 다 흔들리는구나! 이렇게 창피한 며느리와 함께 못살겠다! 그렇지 않니?',
    '흥, 그래도 도저히 이런 며느리와는 함께 살 수 없으니 친정으로 데려다줘야겠다.',
    '시아버지가 놀란 마음을 이해하면서도, 며느리가 일부러 그런 것이 아니라 오래 참아서 힘들었던 것임을 말하고, 며느리를 따뜻하게 이해해 달라고 설득한다.',
    array['PERSPECTIVE', 'EMPATHY', 'REASON', 'REQUEST'],
    '{
      "PERSPECTIVE": "시아버지 또는 며느리의 상황과 입장을 고려해 말함. 충족 예: \"며느리도 일부러 그런 게 아니에요\", \"시아버지도 놀라셨을 것 같아요\". 미충족 예: \"방귀가 정말 크네요\".",
      "EMPATHY": "며느리의 부끄럽고 속상한 마음을 이해하고 배려함. 충족 예: \"며느리가 얼마나 창피하고 속상하겠어요\". 미충족 예: \"며느리가 잘못했어요\".",
      "REASON": "며느리가 방귀를 오래 참았던 까닭이나 크게 터진 까닭을 설명함. 충족 예: \"오래 참아서 한꺼번에 크게 나온 거예요\". 미충족 예: \"그냥 실수한 거예요\". \"일부러 그런 게 아니에요\"처럼 구체적인 까닭이 없는 말은 인정하지 않는다.",
      "REQUEST": "시아버지에게 며느리를 이해하기 위한 구체적인 행동을 요청함. 충족 예: \"친정으로 보내기 전에 며느리 이야기를 한 번만 들어 주세요\". 미충족 예: \"잘해주세요\", \"화내지 마세요\"(무엇을 해야 하는지 구체적이지 않다). \"며느리를 더 혼내 주세요\", \"친정으로 보내세요\"는 구체적이더라도 현재 갈등을 악화시키므로 인정하지 않는다."
    }'::jsonb,
    -- 2026-08-16 걱정 문안 개정: 유도-판정 정합(교차 배선·예/아니오·정답 선점 제거). 콘텐츠팀 검수 전 제안값.
    '{
      "PERSPECTIVE": "나는 놀라서 화만 냈지, 저 아이 입장에서는 이 일이 어떤 일이었을지 미처 생각하지 못했구나.",
      "EMPATHY": "다들 놀라서 며느리의 마음까지는 생각할 겨를이 없었구나.",
      "REASON": "어째서 그렇게 요란한 방귀를 뀌게 되었는지 까닭을 모르겠구나.",
      "REQUEST": "이대로 친정에 데려다줄 참인데, 그 전에 이 늙은이가 무얼 어떻게 해야 할지 모르겠구나."
    }'::jsonb,
    2, 5
)
on conflict (id) do update set
    story_id = excluded.story_id,
    scene_order = excluded.scene_order,
    scene_type = excluded.scene_type,
    scene_description = excluded.scene_description,
    conflict = excluded.conflict,
    image_url = excluded.image_url,
    video_url = excluded.video_url,
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
insert into story_scenes (id, story_id, scene_order, scene_type, scene_description, image_url, video_url) values
(
    '33333333-3333-3333-3333-000000000006',
    '11111111-1111-1111-1111-111111111111',
    6, 'STORY',
    E'한참 걷다 보니 아랫마을 길가에 아주 높은 배나무가 한 그루 서 있었습니다.\n나무 꼭대기에는 노랗고 탐스러운 배들이 주렁주렁 매달려 있었습니다.\n시아버지는 배를 보자 군침이 돌았습니다.\n마침 아랫마을 사람들도 그 배를 먹고 싶어 했지만, 나무가 너무 높아 아무도 딸 수 없었습니다.',
    '/stories/banggui/scenes/06_peartree.jpg',
    -- 6번은 i2v가 아니라 켄번즈(10초 루프) - 인물 6명 미디엄 와이드는 i2v가 무너지는 유형
    '/stories/banggui/scenes/06_peartree.mp4'
)
on conflict (id) do update set
    story_id = excluded.story_id,
    scene_order = excluded.scene_order,
    scene_type = excluded.scene_type,
    scene_description = excluded.scene_description,
    image_url = excluded.image_url,
    video_url = excluded.video_url;

-- 장면 7. 대화3 (sc_banggui_07) - 마을 이장과의 대화 + 미션1
-- video_url 없음 (영상 미제작) - 클라이언트는 image_url로 폴백한다.
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
    '이 배나무는 해마다 탐스러운 배가 열리지만, 너무 높아서 아무도 딸 수가 없었소. 무슨 뾰족한 방법이 없겠는가?',
    '아이고, 방귀 뀌는 며느리 덕분에 온 마을이 배 잔치를 할 수 있겠구려, 고맙소!',
    '높은 배나무의 배를 떨어뜨릴 방법을 생각하고, 며느리의 큰 방귀를 안전하게 사용할 수 있는 해결책을 제안한다.',
    array['SOLUTION', 'REASON', 'REQUEST', 'RESULT'],
    '{
      "SOLUTION": "높은 배를 떨어뜨릴 구체적인 방법을 제시함. 충족 예: \"며느리의 방귀로 배나무를 흔들어요\". 미충족 예: \"배를 따요\". 위험하거나 현재 문제를 악화시키는 제안은 인정하지 않는다.",
      "REASON": "제안한 방법이 가능한 까닭을 설명함. 충족 예: \"며느리 방귀는 지붕도 흔들 만큼 힘이 세니까요\". 미충족 예: \"잘될 것 같아요\".",
      "REQUEST": "며느리에게 부탁하는 방법 또는 사람들의 안전한 대피를 구체적으로 요청함. 충족 예: \"며느리에게 도와달라고 부탁하고, 사람들은 옆으로 피해요\". 미충족 예: \"부탁해요\". 위험하거나 현재 문제를 악화시키는 요청은 인정하지 않는다.",
      "RESULT": "그 방법을 쓰면 생길 일을 예상함. 충족 예: \"배가 우수수 떨어져서 모두 나눠 먹을 수 있어요\". 미충족 예: \"좋아질 거예요\"."
    }'::jsonb,
    -- 2026-08-16 걱정 문안 개정: 유도-판정 정합(교차 배선·예/아니오·정답 선점 제거). 콘텐츠팀 검수 전 제안값.
    -- SOLUTION에 '방귀'·'흔들' 금지: 충족 예의 메커니즘을 캐릭터가 선점하면 안 된다(연동 기준 14장).
    '{
      "SOLUTION": "긴 장대도 사다리도 다 소용이 없었소. 함께 오신 분들 가운데 저 나무를 어찌해 볼 힘을 가진 분이 정말 없겠소?",
      "REASON": "그 방법이 정말 되겠소? 어째서 가능한지 궁금하구려.",
      "REQUEST": "누구에게 어떻게 부탁해야 할지, 사람들은 어찌해야 할지 모르겠구려.",
      "RESULT": "그 방법을 쓰고 나면 그다음에 어떤 일이 벌어지겠소? 배는 어찌 되고 마을 사람들은 어찌 될지 눈앞에 그려지지 않는구려."
    }'::jsonb,
    '{
      "mission_id": "mission_1",
      "mission_type": "PROBLEM_SOLVING",
      "name": "높이 있는 배 따기 미션",
      "purpose": "높은 배나무의 배를 떨어뜨리기 위해 며느리의 큰 방귀를 안전하게 사용하는 방법을 구성한다.",
      "check_points": ["무엇을 사용할 것인지", "왜 그 방법이 가능한지", "며느리에게 어떻게 부탁할 것인지", "그 결과 어떤 일이 생길지"],
      "questions": [
        {"key": "tool", "label": "무엇을 사용할 것인지"},
        {"key": "reason", "label": "왜 그 방법이 가능한지"},
        {"key": "request", "label": "며느리에게 어떻게 부탁할 것인지"},
        {"key": "expectedResult", "label": "그 결과 어떤 일이 생길지"}
      ],
      "exposure_principle": "대화 시작과 동시에 보여주지 않고, 해결 방법을 실제로 구성해야 하는 시점에 노출한다.",
      "exposure_conditions": [
        "아이가 며느리의 방귀를 활용할 수 있다고 제안한 경우",
        "아이가 해결 방향은 말했지만 방법이 구체적이지 않은 경우",
        "2회 이상 대화했지만 실행 방법이 나오지 않은 경우",
        "캐릭터 질문만으로 해결 방법을 구체화하기 어려운 경우"
      ],
      "result_image_url": "/stories/banggui/scenes/07_result.jpg"
    }'::jsonb,
    2, 5
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
insert into story_scenes (id, story_id, scene_order, scene_type, scene_description, image_url, video_url) values
(
    '33333333-3333-3333-3333-000000000008',
    '11111111-1111-1111-1111-111111111111',
    8, 'STORY',
    E'시아버지는 며느리의 방귀가 시끄럽고 별난 것이 아니라, 모두를 도울 수 있는 특별한 힘이라는 것을 깨닫습니다.\n자신이 며느리를 구박했던 일을 후회하고 사과합니다.',
    '/stories/banggui/scenes/08_apology.jpg',
    '/stories/banggui/scenes/08_apology_loop.mp4'
)
on conflict (id) do update set
    story_id = excluded.story_id,
    scene_order = excluded.scene_order,
    scene_type = excluded.scene_type,
    scene_description = excluded.scene_description,
    image_url = excluded.image_url,
    video_url = excluded.video_url;

-- 장면 9. 대화4 (sc_banggui_09) - 방귀쟁이 며느리와의 마지막 대화 + 미션2
-- video_url 없음 (i2v 생성본이 인물 붕괴로 탈락) - 클라이언트는 image_url로 폴백한다.
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
    '네 덕분에 내 방귀가 누군가에게 도움이 될 수 있다는 걸 처음 알았어. 이제는 방귀 소리가 큰 걸 부끄러워하지 않아도 될까?',
    '이제는 부끄러워하며 숨기지 않고, 조심해서 좋은 일에 써 볼게.',
    '다름을 인정하고, 자신의 특징을 긍정적으로 받아들이는 태도를 말한다.',
    array['EMOTION', 'PERSPECTIVE', 'RESULT', 'SOLUTION'],
    '{
      "EMOTION": "며느리의 달라진 마음이나 그에 대한 아이 자신의 감정을 표현함. 충족 예: \"이제 당당해져서 기뻐요\". 미충족 예: \"방귀 소리가 커요\".",
      "PERSPECTIVE": "큰 방귀라는 특징을 부끄러운 단점만이 아닌 다른 관점으로 바라봄. 충족 예: \"부끄러운 게 아니라 특별한 힘이에요\". 미충족 예: \"큰 방귀는 무조건 이상해요\".",
      "RESULT": "특징을 좋은 일에 사용하면 생길 결과를 예상함. 충족 예: \"높은 곳의 열매를 딸 때 도와줄 수 있어요\". 미충족 예: \"좋은 일이 생겨요\".",
      "SOLUTION": "특징을 좋게 사용하는 구체적인 방법을 제안함. 충족 예: \"미리 알려 주고 사람 없는 쪽에서 뀌면 돼요\". 미충족 예: \"그냥 마음대로 뀌어요\". 위험하거나 현재 문제를 악화시키는 제안은 인정하지 않는다."
    }'::jsonb,
    -- 2026-08-16 걱정 문안 개정: 유도-판정 정합(교차 배선·예/아니오·정답 선점 제거). 콘텐츠팀 검수 전 제안값.
    '{
      "EMOTION": "예전엔 숨고만 싶었는데, 지금은 내 마음이 어떻게 달라진 건지 나도 잘 모르겠어.",
      "PERSPECTIVE": "누가 또 방귀쟁이라고 놀리면, 내 방귀를 뭐라고 생각하면 좋을까? 아직 잘 모르겠어.",
      "RESULT": "조심해서 좋은 일에 쓰면 어떤 일이 생길까? 아직 눈앞에 잘 그려지지 않아.",
      "SOLUTION": "언제 어떻게 써야 좋을지 아직 잘 모르겠어."
    }'::jsonb,
    '{
      "mission_id": "mission_2",
      "mission_type": "PERSPECTIVE_SHIFT",
      "name": "친구들의 단점을 장점으로 바꾸기 미션",
      "purpose": "친구나 주변 사람의 특징을 다른 관점에서 바라보고, 단점처럼 보이는 특징을 장점이나 가능성으로 바꾸어 말한다.",
      "cards": [
        {"key": "loud_voice", "label": "목소리가 큰 친구", "image_url": null, "template": "목소리가 큰 친구는 ___ 할 수 있어요."},
        {"key": "talkative", "label": "말이 많은 친구", "image_url": null, "template": "말이 많은 친구는 ___ 할 수 있어요."},
        {"key": "fearful", "label": "겁이 많은 친구", "image_url": null, "template": "겁이 많은 친구는 ___ 할 수 있어요."},
        {"key": "playful", "label": "장난을 많이 치는 친구", "image_url": null, "template": "장난을 많이 치는 친구는 ___ 할 수 있어요."}
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
        "말이 많은 친구는 재미있는 이야기를 들려줄 수 있어요.",
        "겁이 많은 친구는 위험한 일을 먼저 알아차릴 수 있어요.",
        "장난을 많이 치는 친구는 친구들을 즐겁게 해 줄 수 있어요."
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
--    가격: 소품 1 / 중형 2 / 대형과 동물 3 (2026-08 인하 확정)
--    해금: ALWAYS 12 / STARDUST_CUMULATIVE 3 (집 3, 토끼 4, 거북이 5) / STORY_COMPLETE 1
--
--    가격과 해금 근거: 이야기 1편의 평생 획득이 최대 6(1회차 완주 3 + 장면 보너스 2,
--    2회차 완주 1)이다. 첫 완주만으로 동물 하나와 소품 두엇을 살 수 있어야 상점이
--    보상으로 기능한다. 이전 값(가격 3/5/10, 해금 15/30/50)은 닿을 수 없었다.
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
('44444444-4444-4444-4444-000000000001', '작은 돌', 'TERRAIN_PROP', 1, 'ALWAYS', null, null, '/items/models/rock_smallA.glb', null, 1),
('44444444-4444-4444-4444-000000000003', '납작 돌', 'TERRAIN_PROP', 1, 'ALWAYS', null, null, '/items/models/rock_smallFlatA.glb', null, 2),
('44444444-4444-4444-4444-000000000004', '조약돌', 'TERRAIN_PROP', 1, 'ALWAYS', null, null, '/items/models/stone_smallG.glb', null, 3),
('44444444-4444-4444-4444-000000000005', '돌무더기', 'TERRAIN_PROP', 1, 'ALWAYS', null, null, '/items/models/forest/stones.glb', null, 4),
('44444444-4444-4444-4444-000000000006', '통나무', 'TERRAIN_PROP', 1, 'ALWAYS', null, null, '/items/models/log.glb', null, 5),
('44444444-4444-4444-4444-000000000008', '장작더미', 'TERRAIN_PROP', 1, 'ALWAYS', null, null, '/items/models/log_stack.glb', null, 6),
('44444444-4444-4444-4444-000000000009', '돌길', 'TERRAIN_PROP', 1, 'ALWAYS', null, null, '/items/models/path_stone.glb', null, 7),
('44444444-4444-4444-4444-000000000002', '풀', 'PLANT', 1, 'ALWAYS', null, null, '/items/models/grass_large.glb', null, 8),
('44444444-4444-4444-4444-00000000000a', '잔디밭', 'PLANT', 1, 'ALWAYS', null, null, '/items/models/forest/patch-grass.glb', null, 9),
('44444444-4444-4444-4444-00000000000b', '빨간 꽃', 'PLANT', 1, 'ALWAYS', null, null, '/items/models/flower_redA.glb', null, 10),
('44444444-4444-4444-4444-00000000000c', '노란 꽃', 'PLANT', 1, 'ALWAYS', null, null, '/items/models/flower_yellowA.glb', null, 11),
('aef875b6-9c7a-5d41-8724-f4715b13ae58', '버섯', 'PLANT', 1, 'ALWAYS', null, null, '/items/models/mushroom_redGroup.glb', null, 12),
('b1abdfdc-6565-5fc6-975d-12732df2663b', '덤불', 'PLANT', 1, 'ALWAYS', null, null, '/items/models/plant_bushDetailed.glb', null, 13),
('44444444-4444-4444-4444-000000000007', '작은 나무', 'PLANT', 1, 'ALWAYS', null, null, '/items/models/forest/plant.glb', null, 14),
('4ebfa1f3-70ff-5f92-87de-8e61eca016ea', '나무', 'STRUCTURE', 1, 'ALWAYS', null, null, '/items/models/tree_default.glb', null, 15),
('e52adb24-0b4c-5925-8aa0-2fd57c169d89', '울타리', 'STRUCTURE', 1, 'ALWAYS', null, null, '/items/models/fence_simple.glb', null, 16),
('86d95881-2e92-5a0e-a262-ad22a57bebd6', '가로등', 'STRUCTURE', 1, 'ALWAYS', null, null, '/items/models/town/lantern.glb', null, 17),
('78de4d62-e656-5e98-960b-c596f4a5d964', '모닥불', 'STRUCTURE', 2, 'STARDUST_CUMULATIVE', null, 4, '/items/models/campfire_stones.glb', null, 18),
('158c687a-b363-5afe-94f1-8dbfc90e0551', '텐트', 'STRUCTURE', 2, 'STARDUST_CUMULATIVE', null, 6, '/items/models/tent_smallOpen.glb', null, 19),
('44444444-4444-4444-4444-00000000000d', '우리 집', 'STRUCTURE', 2, 'STARDUST_CUMULATIVE', null, 9, '/items/models/city/building-type-a.glb', null, 20),
('4bf48af0-5cef-56db-a507-90848e5b7668', '이웃 집', 'STRUCTURE', 2, 'STARDUST_CUMULATIVE', null, 12, '/items/models/city/building-type-e.glb', null, 21),
('2012eb9a-4d30-528a-84af-1b97d46e8b83', '분수대', 'STRUCTURE', 2, 'STARDUST_CUMULATIVE', null, 16, '/items/models/town/fountain-round.glb', null, 22),
('9e710e15-059c-5c38-962f-b6f5d7263eb2', '풍차', 'STRUCTURE', 2, 'STARDUST_CUMULATIVE', null, 20, '/items/models/town/windmill.glb', null, 23),
('7ee5477f-435b-52c0-8d9b-99be60551ff5', '당근', 'PLANT', 1, 'ALWAYS', null, null, '/items/models/nature/crop_carrot.glb', null, 24),
('3c452708-e764-578d-a883-7eccd0dee5f5', '호박', 'PLANT', 1, 'ALWAYS', null, null, '/items/models/nature/crop_pumpkin.glb', null, 25),
('3ee27e55-efae-5b97-8c2c-7c559c665b7d', '수박', 'PLANT', 1, 'ALWAYS', null, null, '/items/models/nature/crop_melon.glb', null, 26),
('c315deea-a319-5dac-abc2-5ced933d8486', '참나무', 'STRUCTURE', 1, 'ALWAYS', null, null, '/items/models/nature/tree_oak.glb', null, 27),
('858c7f62-bd62-51a4-b5f5-f8a32d7263a1', '소나무', 'STRUCTURE', 1, 'ALWAYS', null, null, '/items/models/nature/tree_pineRoundC.glb', null, 28),
('89ad73a7-ed3d-5b8b-90ea-4e3dfcdd434d', '야자수', 'STRUCTURE', 1, 'ALWAYS', null, null, '/items/models/nature/tree_palmDetailedTall.glb', null, 29),
('a26ac50e-94cc-5445-9fed-4f1cf2991a4b', '단풍나무', 'STRUCTURE', 1, 'ALWAYS', null, null, '/items/models/nature/tree_default_fall.glb', null, 30),
('9e6b0b25-015e-59e1-a914-f4b9194d6346', '갈색 버섯', 'PLANT', 1, 'ALWAYS', null, null, '/items/models/nature/mushroom_tanGroup.glb', null, 31),
('32239f1f-1f3c-501c-8c68-025366b56d9a', '선인장', 'PLANT', 1, 'ALWAYS', null, null, '/items/models/nature/cactus_tall.glb', null, 32),
('914b3629-c913-54c1-a65d-dd7240334327', '나무 그루터기', 'TERRAIN_PROP', 1, 'ALWAYS', null, null, '/items/models/nature/stump_roundDetailed.glb', null, 33),
('513723fe-3c47-59af-9182-b4c19666c799', '연꽃', 'PLANT', 1, 'ALWAYS', null, null, '/items/models/nature/lily_large.glb', null, 34),
('0f045dcc-5f54-5ada-a44a-dfdbc7de2e14', '표지판', 'STRUCTURE', 1, 'ALWAYS', null, null, '/items/models/nature/sign.glb', null, 35),
('1bcd86b9-c047-56b7-b65f-3c9e2f80b7b9', '나룻배', 'STRUCTURE', 1, 'ALWAYS', null, null, '/items/models/nature/canoe.glb', null, 36),
('3a89aa26-4182-5958-b039-0b59a78822f9', '돌 조각상', 'STRUCTURE', 2, 'STARDUST_CUMULATIVE', null, 8, '/items/models/nature/statue_head.glb', null, 37),
('44444444-4444-4444-4444-00000000000e', '토끼', 'ANIMAL', 2, 'STORY_COMPLETE', '11111111-1111-1111-1111-111111111111', null, '/items/models/pets/animal-bunny.glb', null, 38),
('44444444-4444-4444-4444-00000000000f', '고양이', 'ANIMAL', 2, 'STARDUST_CUMULATIVE', null, 5, '/items/models/pets/animal-cat.glb', null, 39),
('059d3974-b8bd-52e4-a6d1-dd0114890baa', '강아지', 'ANIMAL', 2, 'STARDUST_CUMULATIVE', null, 8, '/items/models/pets/animal-dog.glb', null, 40),
('44444444-4444-4444-4444-000000000010', '여우', 'ANIMAL', 2, 'STARDUST_CUMULATIVE', null, 11, '/items/models/pets/animal-fox.glb', null, 41),
('873ccc94-dc7a-59c6-beca-4f0333f7e1ca', '사슴', 'ANIMAL', 2, 'STARDUST_CUMULATIVE', null, 14, '/items/models/pets/animal-deer.glb', null, 42),
('d8e565b7-1efa-57d2-88af-68f49c9fde99', '돼지', 'ANIMAL', 2, 'STARDUST_CUMULATIVE', null, 18, '/items/models/pets/animal-pig.glb', null, 43),
('e3230e20-b6e0-5a2b-9f4b-9ac3d5ccb62a', '펭귄', 'ANIMAL', 2, 'STARDUST_CUMULATIVE', null, 22, '/items/models/pets/animal-penguin.glb', null, 44),
('50d78626-5754-5351-ba1b-c85793d80b0f', '판다', 'ANIMAL', 2, 'STARDUST_CUMULATIVE', null, 25, '/items/models/pets/animal-panda.glb', null, 45)
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


-- ------------------------------------------------------------
-- scene_audio - 방귀 이야기 사전 렌더 음성 13건
--   내레이션 5(도입/전개1~4) + 고정 첫/마지막 대사 8 = 13.
--
--   원래 R__2_seed_demo_data.sql 에 자리표시자로 있었는데 여기로 옮겼다.
--   그 파일은 머리말이 "배포 전 삭제하거나 dev 프로파일로 옮긴다"고 적어 둔 데모 데이터인데,
--   음성은 데모가 아니라 이야기 콘텐츠라 같이 사라지면 안 된다.
--
--   engine/voice/speaking_rate/duration_ms/sentence_timings 는 실제 렌더 산출물의
--   메타데이터다(장면음성_교체본/manifest.json 실측). duration_ms 는 ffprobe 실측이고,
--   sentence_timings 는 문장별로 따로 합성해 이어 붙이며 잰 실제 시각이라
--   자막을 글자수 비례로 추정하지 않아도 된다.
--
--   text_hash 는 여기서 적지 않고 원문에서 계산한다 - 손으로 적으면 대사를 고쳤을 때
--   같이 안 고쳐져서, 화면엔 새 문장 스피커엔 옛 문장인 상태를 오히려 못 잡는다.
--
--   충돌 대상은 부분 유니크 인덱스 idx_scene_audio_shared 라, ON CONFLICT 에
--   predicate(where child_id is null)를 같이 적어야 그 인덱스로 잡힌다. 빼면 실패한다.
-- ------------------------------------------------------------

-- 장면1 도입 (NARRATION)
insert into scene_audio (scene_id, slot, child_id, storage_path, text_hash,
                         engine, voice, speaking_rate, duration_ms, sentence_timings)
select s.id, 'NARRATION', null, 'tts/banggui/sc_banggui_01.mp3',
       encode(digest(s.scene_description, 'sha256'), 'hex'),
       'gemini-2.5-flash-preview-tts', 'Kore', 0.95, 20011,
       '[{"index": 0, "start": 0.0, "end": 6.291, "text": "옛날 어느 마을에 방귀를 아주 크게 뀌는 며느리가 살았습니다."}, {"index": 1, "start": 6.891, "end": 13.222, "text": "며느리는 시집에 온 뒤로 늘 얌전하고 예의 바르게 보이고 싶었습니다."}, {"index": 2, "start": 13.822, "end": 19.793, "text": "시댁 식구들이 자신을 이상하게 볼까 봐 걱정했기 때문입니다."}]'::jsonb
from story_scenes s
where s.story_id = '11111111-1111-1111-1111-111111111111' and s.scene_order = 1
on conflict (scene_id, slot) where child_id is null do update set
    storage_path     = excluded.storage_path,
    text_hash        = excluded.text_hash,
    engine           = excluded.engine,
    voice            = excluded.voice,
    speaking_rate    = excluded.speaking_rate,
    duration_ms      = excluded.duration_ms,
    sentence_timings = excluded.sentence_timings;

-- 장면2 전개1 (NARRATION)
insert into scene_audio (scene_id, slot, child_id, storage_path, text_hash,
                         engine, voice, speaking_rate, duration_ms, sentence_timings)
select s.id, 'NARRATION', null, 'tts/banggui/sc_banggui_02.mp3',
       encode(digest(s.scene_description, 'sha256'), 'hex'),
       'gemini-2.5-flash-preview-tts', 'Kore', 0.95, 33731,
       '[{"index": 0, "start": 0.0, "end": 7.331, "text": "그래서 며느리는 방귀가 나오려고 할 때마다 꾹꾹 참았습니다."}, {"index": 1, "start": 7.931, "end": 23.782, "text": "하루도 참고, 이틀도 참고, 그렇게 오래 참다 보니 배는 점점 빵빵하게 부풀어 올랐고 얼굴은 노랗게 변했습니다."}, {"index": 2, "start": 24.382, "end": 33.513, "text": "몸도 마음도 너무 힘들었지만, 며느리는 차마 가족들에게 솔직하게 말하지 못했습니다."}]'::jsonb
from story_scenes s
where s.story_id = '11111111-1111-1111-1111-111111111111' and s.scene_order = 2
on conflict (scene_id, slot) where child_id is null do update set
    storage_path     = excluded.storage_path,
    text_hash        = excluded.text_hash,
    engine           = excluded.engine,
    voice            = excluded.voice,
    speaking_rate    = excluded.speaking_rate,
    duration_ms      = excluded.duration_ms,
    sentence_timings = excluded.sentence_timings;

-- 장면3 대화1 첫 대사 (OPENING)
insert into scene_audio (scene_id, slot, child_id, storage_path, text_hash,
                         engine, voice, speaking_rate, duration_ms, sentence_timings)
select s.id, 'OPENING', null, 'tts/banggui/sc_banggui_03_opening.mp3',
       encode(digest(s.character_opening, 'sha256'), 'hex'),
       'gemini-2.5-flash-preview-tts', 'Leda', 0.95, 7491,
       '[{"index": 0, "start": 0.0, "end": 7.491, "text": "내 방귀가 너무 크다는 걸 알면 가족들이 나를 이상하게 생각하지 않을까?"}]'::jsonb
from story_scenes s
where s.story_id = '11111111-1111-1111-1111-111111111111' and s.scene_order = 3
on conflict (scene_id, slot) where child_id is null do update set
    storage_path     = excluded.storage_path,
    text_hash        = excluded.text_hash,
    engine           = excluded.engine,
    voice            = excluded.voice,
    speaking_rate    = excluded.speaking_rate,
    duration_ms      = excluded.duration_ms,
    sentence_timings = excluded.sentence_timings;

-- 장면3 대화1 마지막 대사 (CLOSING)
insert into scene_audio (scene_id, slot, child_id, storage_path, text_hash,
                         engine, voice, speaking_rate, duration_ms, sentence_timings)
select s.id, 'CLOSING', null, 'tts/banggui/sc_banggui_03_closing.mp3',
       encode(digest(s.character_closing, 'sha256'), 'hex'),
       'gemini-2.5-flash-preview-tts', 'Leda', 0.95, 7443,
       '[{"index": 0, "start": 0.0, "end": 3.931, "text": "그래도 아직은 못 말하겠어."}, {"index": 1, "start": 4.531, "end": 7.342, "text": "조금만 더 참아 볼게."}]'::jsonb
from story_scenes s
where s.story_id = '11111111-1111-1111-1111-111111111111' and s.scene_order = 3
on conflict (scene_id, slot) where child_id is null do update set
    storage_path     = excluded.storage_path,
    text_hash        = excluded.text_hash,
    engine           = excluded.engine,
    voice            = excluded.voice,
    speaking_rate    = excluded.speaking_rate,
    duration_ms      = excluded.duration_ms,
    sentence_timings = excluded.sentence_timings;

-- 장면4 전개2 (NARRATION)
insert into scene_audio (scene_id, slot, child_id, storage_path, text_hash,
                         engine, voice, speaking_rate, duration_ms, sentence_timings)
select s.id, 'NARRATION', null, 'tts/banggui/sc_banggui_04.mp3',
       encode(digest(s.scene_description, 'sha256'), 'hex'),
       'gemini-2.5-flash-preview-tts', 'Kore', 0.95, 27323,
       '[{"index": 0, "start": 0.0, "end": 8.051, "text": "며느리는 더 이상 참을 수 없어 몰래 살짝만 방귀를 뀌려고 합니다."}, {"index": 1, "start": 8.651, "end": 14.382, "text": "하지만 오래 참았던 탓에 방귀가 크게 터져 나왔습니다."}, {"index": 2, "start": 14.982, "end": 27.113, "text": "마당의 먼지가 휘리릭 날아가고, 기왓장이 달그락거리고, 시아버지의 갓까지 휙 날아가 버렸습니다."}]'::jsonb
from story_scenes s
where s.story_id = '11111111-1111-1111-1111-111111111111' and s.scene_order = 4
on conflict (scene_id, slot) where child_id is null do update set
    storage_path     = excluded.storage_path,
    text_hash        = excluded.text_hash,
    engine           = excluded.engine,
    voice            = excluded.voice,
    speaking_rate    = excluded.speaking_rate,
    duration_ms      = excluded.duration_ms,
    sentence_timings = excluded.sentence_timings;

-- 장면5 대화2 첫 대사 (OPENING)
insert into scene_audio (scene_id, slot, child_id, storage_path, text_hash,
                         engine, voice, speaking_rate, duration_ms, sentence_timings)
select s.id, 'OPENING', null, 'tts/banggui/sc_banggui_05_opening.mp3',
       encode(digest(s.character_opening, 'sha256'), 'hex'),
       'gemini-2.5-flash-preview-tts', 'Puck', 1.02, 14859,
       '[{"index": 0, "start": 0.0, "end": 3.211, "text": "아이고 이게 무슨 일이냐!"}, {"index": 1, "start": 3.811, "end": 7.222, "text": "우리 집안이 다 흔들리는구나!"}, {"index": 2, "start": 7.822, "end": 12.313, "text": "이렇게 창피한 며느리와 함께 못살겠다!"}, {"index": 3, "start": 12.913, "end": 14.524, "text": "그렇지 않니?"}]'::jsonb
from story_scenes s
where s.story_id = '11111111-1111-1111-1111-111111111111' and s.scene_order = 5
on conflict (scene_id, slot) where child_id is null do update set
    storage_path     = excluded.storage_path,
    text_hash        = excluded.text_hash,
    engine           = excluded.engine,
    voice            = excluded.voice,
    speaking_rate    = excluded.speaking_rate,
    duration_ms      = excluded.duration_ms,
    sentence_timings = excluded.sentence_timings;

-- 장면5 대화2 마지막 대사 (CLOSING)
insert into scene_audio (scene_id, slot, child_id, storage_path, text_hash,
                         engine, voice, speaking_rate, duration_ms, sentence_timings)
select s.id, 'CLOSING', null, 'tts/banggui/sc_banggui_05_closing.mp3',
       encode(digest(s.character_closing, 'sha256'), 'hex'),
       'gemini-2.5-flash-preview-tts', 'Puck', 1.02, 7811,
       '[{"index": 0, "start": 0.0, "end": 7.811, "text": "흥, 그래도 도저히 이런 며느리와는 함께 살 수 없으니 친정으로 데려다줘야겠다."}]'::jsonb
from story_scenes s
where s.story_id = '11111111-1111-1111-1111-111111111111' and s.scene_order = 5
on conflict (scene_id, slot) where child_id is null do update set
    storage_path     = excluded.storage_path,
    text_hash        = excluded.text_hash,
    engine           = excluded.engine,
    voice            = excluded.voice,
    speaking_rate    = excluded.speaking_rate,
    duration_ms      = excluded.duration_ms,
    sentence_timings = excluded.sentence_timings;

-- 장면6 전개3 (NARRATION)
insert into scene_audio (scene_id, slot, child_id, storage_path, text_hash,
                         engine, voice, speaking_rate, duration_ms, sentence_timings)
select s.id, 'NARRATION', null, 'tts/banggui/sc_banggui_06.mp3',
       encode(digest(s.scene_description, 'sha256'), 'hex'),
       'gemini-2.5-flash-preview-tts', 'Kore', 0.95, 34483,
       '[{"index": 0, "start": 0.0, "end": 9.251, "text": "한참 걷다 보니 아랫마을 길가에 아주 높은 배나무가 한 그루 서 있었습니다."}, {"index": 1, "start": 9.851, "end": 17.542, "text": "나무 꼭대기에는 노랗고 탐스러운 배들이 주렁주렁 매달려 있었습니다."}, {"index": 2, "start": 18.142, "end": 23.153, "text": "시아버지는 배를 보자 군침이 돌았습니다."}, {"index": 3, "start": 23.753, "end": 34.164, "text": "마침 아랫마을 사람들도 그 배를 먹고 싶어 했지만, 나무가 너무 높아 아무도 딸 수 없었습니다."}]'::jsonb
from story_scenes s
where s.story_id = '11111111-1111-1111-1111-111111111111' and s.scene_order = 6
on conflict (scene_id, slot) where child_id is null do update set
    storage_path     = excluded.storage_path,
    text_hash        = excluded.text_hash,
    engine           = excluded.engine,
    voice            = excluded.voice,
    speaking_rate    = excluded.speaking_rate,
    duration_ms      = excluded.duration_ms,
    sentence_timings = excluded.sentence_timings;

-- 장면7 대화3 첫 대사 (OPENING)
insert into scene_audio (scene_id, slot, child_id, storage_path, text_hash,
                         engine, voice, speaking_rate, duration_ms, sentence_timings)
select s.id, 'OPENING', null, 'tts/banggui/sc_banggui_07_opening.mp3',
       encode(digest(s.character_opening, 'sha256'), 'hex'),
       'gemini-2.5-flash-preview-tts', 'Charon', 1.0, 11579,
       '[{"index": 0, "start": 0.0, "end": 7.251, "text": "이 배나무는 해마다 탐스러운 배가 열리지만, 너무 높아서 아무도 딸 수가 없었소."}, {"index": 1, "start": 7.851, "end": 11.462, "text": "무슨 뾰족한 방법이 없겠는가?"}]'::jsonb
from story_scenes s
where s.story_id = '11111111-1111-1111-1111-111111111111' and s.scene_order = 7
on conflict (scene_id, slot) where child_id is null do update set
    storage_path     = excluded.storage_path,
    text_hash        = excluded.text_hash,
    engine           = excluded.engine,
    voice            = excluded.voice,
    speaking_rate    = excluded.speaking_rate,
    duration_ms      = excluded.duration_ms,
    sentence_timings = excluded.sentence_timings;

-- 장면7 대화3 마지막 대사 (CLOSING)
insert into scene_audio (scene_id, slot, child_id, storage_path, text_hash,
                         engine, voice, speaking_rate, duration_ms, sentence_timings)
select s.id, 'CLOSING', null, 'tts/banggui/sc_banggui_07_closing.mp3',
       encode(digest(s.character_closing, 'sha256'), 'hex'),
       'gemini-2.5-flash-preview-tts', 'Charon', 1.0, 6931,
       '[{"index": 0, "start": 0.0, "end": 6.931, "text": "아이고, 방귀 뀌는 며느리 덕분에 온 마을이 배 잔치를 할 수 있겠구려, 고맙소!"}]'::jsonb
from story_scenes s
where s.story_id = '11111111-1111-1111-1111-111111111111' and s.scene_order = 7
on conflict (scene_id, slot) where child_id is null do update set
    storage_path     = excluded.storage_path,
    text_hash        = excluded.text_hash,
    engine           = excluded.engine,
    voice            = excluded.voice,
    speaking_rate    = excluded.speaking_rate,
    duration_ms      = excluded.duration_ms,
    sentence_timings = excluded.sentence_timings;

-- 장면8 전개4 (NARRATION)
insert into scene_audio (scene_id, slot, child_id, storage_path, text_hash,
                         engine, voice, speaking_rate, duration_ms, sentence_timings)
select s.id, 'NARRATION', null, 'tts/banggui/sc_banggui_08.mp3',
       encode(digest(s.scene_description, 'sha256'), 'hex'),
       'gemini-2.5-flash-preview-tts', 'Kore', 0.95, 17899,
       '[{"index": 0, "start": 0.0, "end": 11.091, "text": "시아버지는 며느리의 방귀가 시끄럽고 별난 것이 아니라, 모두를 도울 수 있는 특별한 힘이라는 것을 깨닫습니다."}, {"index": 1, "start": 11.691, "end": 17.782, "text": "자신이 며느리를 구박했던 일을 후회하고 사과합니다."}]'::jsonb
from story_scenes s
where s.story_id = '11111111-1111-1111-1111-111111111111' and s.scene_order = 8
on conflict (scene_id, slot) where child_id is null do update set
    storage_path     = excluded.storage_path,
    text_hash        = excluded.text_hash,
    engine           = excluded.engine,
    voice            = excluded.voice,
    speaking_rate    = excluded.speaking_rate,
    duration_ms      = excluded.duration_ms,
    sentence_timings = excluded.sentence_timings;

-- 장면9 대화4 첫 대사 (OPENING)
insert into scene_audio (scene_id, slot, child_id, storage_path, text_hash,
                         engine, voice, speaking_rate, duration_ms, sentence_timings)
select s.id, 'OPENING', null, 'tts/banggui/sc_banggui_09_opening.mp3',
       encode(digest(s.character_opening, 'sha256'), 'hex'),
       'gemini-2.5-flash-preview-tts', 'Leda', 0.95, 14259,
       '[{"index": 0, "start": 0.0, "end": 8.211, "text": "네 덕분에 내 방귀가 누군가에게 도움이 될 수 있다는 걸 처음 알았어."}, {"index": 1, "start": 8.811, "end": 14.142, "text": "이제는 방귀 소리가 큰 걸 부끄러워하지 않아도 될까?"}]'::jsonb
from story_scenes s
where s.story_id = '11111111-1111-1111-1111-111111111111' and s.scene_order = 9
on conflict (scene_id, slot) where child_id is null do update set
    storage_path     = excluded.storage_path,
    text_hash        = excluded.text_hash,
    engine           = excluded.engine,
    voice            = excluded.voice,
    speaking_rate    = excluded.speaking_rate,
    duration_ms      = excluded.duration_ms,
    sentence_timings = excluded.sentence_timings;

-- 장면9 대화4 마지막 대사 (CLOSING)
insert into scene_audio (scene_id, slot, child_id, storage_path, text_hash,
                         engine, voice, speaking_rate, duration_ms, sentence_timings)
select s.id, 'CLOSING', null, 'tts/banggui/sc_banggui_09_closing.mp3',
       encode(digest(s.character_closing, 'sha256'), 'hex'),
       'gemini-2.5-flash-preview-tts', 'Leda', 0.95, 7451,
       '[{"index": 0, "start": 0.0, "end": 7.451, "text": "이제는 부끄러워하며 숨기지 않고, 조심해서 좋은 일에 써 볼게."}]'::jsonb
from story_scenes s
where s.story_id = '11111111-1111-1111-1111-111111111111' and s.scene_order = 9
on conflict (scene_id, slot) where child_id is null do update set
    storage_path     = excluded.storage_path,
    text_hash        = excluded.text_hash,
    engine           = excluded.engine,
    voice            = excluded.voice,
    speaking_rate    = excluded.speaking_rate,
    duration_ms      = excluded.duration_ms,
    sentence_timings = excluded.sentence_timings;
