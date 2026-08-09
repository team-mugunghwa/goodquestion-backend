-- ============================================================
-- 굿퀘스천 시드 데이터 — 전체 24개 테이블
--   콘텐츠: MVP '방귀 뀌는 며느리' (이야기·장면·캐릭터·주제·아이템)
--   계정·진행: 데모 보호자·아이와 완주/진행 중 세션, 별가루·행성 내역
--
-- 실행: schema.sql 적용 후 이 파일 하나만 실행한다. 전체가 한 트랜잭션이다.
--
-- 데모 로그인
--   이메일   demo@goodquestion.kr
--   비밀번호 demo1234!
--   password_hash는 BCryptPasswordEncoder 기본 설정(강도 10)으로 생성해 검증한 값이다.
--
-- 교체가 필요한 플레이스홀더
-- · image_url / model_url / thumbnail_url — 에셋 업로드 후 실제 경로로
-- · scene_audio의 engine·voice·duration_ms·sentence_timings — TTS manifest.json 실측값으로
--   (text_hash만은 원본 텍스트에서 실제로 계산한 값이라 그대로 쓴다)
-- · characters.tts_voice — TTS 엔진 확정 후
--
-- 콘텐츠팀 확인이 필요한 값
-- · 콘텐츠 문서의 문자열 ID(s_banggui_..., sc_banggui_01~09)는
--   스키마의 uuid PK 정책에 따라 고정 uuid로 치환. 주석에 원본 ID 병기.
-- · preferred_turns는 문서에 없어 제안값 (max_turns - 2).
-- · 대화1의 target_elements 중 'EXPRESSION'은 사고 요소 8종에 정의되지 않은 값이라
--   시드에서 제외 (아래 대화1 주석 참고).
-- ============================================================

begin;

-- ------------------------------------------------------------
-- 1. topics — 이야기 주제 3종
-- ------------------------------------------------------------
insert into topics (id, name, display_order) values
    ('22222222-2222-2222-2222-000000000001', '다름',      1),
    ('22222222-2222-2222-2222-000000000002', '자기이해',  2),
    ('22222222-2222-2222-2222-000000000003', '장점 발견', 3);

-- ------------------------------------------------------------
-- 2. stories — 방귀 뀌는 며느리 (원본 ID: s_banggui_daughter_in_law_001)
-- ------------------------------------------------------------
insert into stories (id, title, summary, image_url, difficulty, estimated_minutes, post_activity_config, status) values
(
    '11111111-1111-1111-1111-111111111111',
    '방귀 뀌는 며느리',
    '큰 방귀를 부끄러워하던 며느리가 자신의 다름을 장점으로 바꾸는 이야기',
    '/stories/banggui/cover.png',
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
);

insert into story_topics (story_id, topic_id) values
    ('11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-000000000001'),
    ('11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-000000000002'),
    ('11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-000000000003');

-- ------------------------------------------------------------
-- 3. story_scenes — 9개 장면 (도입1 + 전개4 + 대화4)
-- ------------------------------------------------------------

-- 장면 1. 도입 (원본 ID: sc_banggui_01) — 전체 화면 스토리
insert into story_scenes (id, story_id, scene_order, scene_type, scene_description, image_url) values
(
    '33333333-3333-3333-3333-000000000001',
    '11111111-1111-1111-1111-111111111111',
    1, 'STORY',
    '옛날 어느 마을에 방귀를 아주 크게 뀌는 며느리가 살았습니다. 며느리는 시집에 온 뒤로 늘 얌전하고 예의 바르게 보이고 싶었습니다. 시댁 식구들이 자신을 이상하게 볼까 봐 걱정했기 때문입니다.',
    '/stories/banggui/scenes/01_intro.png'
);

-- 장면 2. 전개1 (sc_banggui_02) — 말 못할 사정이 있는 며느리
insert into story_scenes (id, story_id, scene_order, scene_type, scene_description, image_url) values
(
    '33333333-3333-3333-3333-000000000002',
    '11111111-1111-1111-1111-111111111111',
    2, 'STORY',
    '그래서 며느리는 방귀가 나오려고 할 때마다 꾹꾹 참았습니다. 하루도 참고, 이틀도 참고, 그렇게 오래 참다 보니 배는 점점 빵빵하게 부풀어 올랐고 얼굴은 노랗게 변했습니다. 몸도 마음도 너무 힘들었지만, 며느리는 차마 가족들에게 솔직하게 말하지 못했습니다.',
    '/stories/banggui/scenes/02_holding.png'
);

-- 장면 3. 대화1 (sc_banggui_03) — 방귀쟁이 며느리와의 대화
-- 문서 target_elements: PERSPECTIVE, EMOTION, EXPRESSION, SOLUTION
-- ※ EXPRESSION은 사고 요소 8종에 없는 미정의 값 → 제외하고 시드. 콘텐츠팀 확인 필요.
insert into story_scenes (id, story_id, scene_order, scene_type, scene_description, conflict, image_url,
                          character_name, character_persona, character_opening, character_closing,
                          scene_goal, required_elements, element_criteria, remaining_worries,
                          preferred_turns, max_turns) values
(
    '33333333-3333-3333-3333-000000000003',
    '11111111-1111-1111-1111-111111111111',
    3, 'DIALOGUE',
    '며느리가 방귀를 참는 것이 너무 힘들지만, 가족들이 자신을 이상하게 볼까 봐 걱정하고 있다.',
    '방귀를 뀌고 싶지만 가족들이 이상하게 생각할까 봐 솔직하게 말하지 못한다.',
    '/stories/banggui/scenes/03_dialogue1.png',
    '방귀쟁이 며느리',
    '남을 많이 의식해 조심스럽지만 따뜻한 인물. 가족에게 폐를 끼치거나 이상하게 보이는 것을 걱정하고, 자신의 불편함보다 주변 사람의 반응을 먼저 생각한다. 부끄러움이 많아 자신의 특별한 특징을 쉽게 드러내지 못한다. 이 장면에서는 방귀를 오래 참아 몸이 힘든데도 걱정 때문에 말하지 못하는 상태다.',
    'ㅇㅇ아, 내 방귀가 너무 크다는 걸 알면 가족들이 나를 이상하게 생각하지 않을까?',
    '그래도 아직은 못 말하겠어. 조금만 더 참아 볼게.',
    '며느리의 걱정에 공감하며, 며느리의 마음과 상황에 대한 자기 생각을 표현하고 해결 방법을 함께 생각한다.',
    array['PERSPECTIVE', 'EMOTION', 'SOLUTION'],
    '{
      "PERSPECTIVE": "며느리나 가족의 상황·입장을 헤아려 말함 (예: 가족들도 놀라긴 하겠지만 이해해 줄 거예요)",
      "EMOTION": "며느리의 감정이나 그 상황에 대한 자신의 감정을 직접 표현함 (예: 많이 힘들겠어요, 답답할 것 같아요)",
      "SOLUTION": "며느리가 할 수 있는 구체적인 행동을 제안함 (예: 가족들에게 솔직하게 말해 보세요)"
    }'::jsonb,
    '{
      "PERSPECTIVE": "가족들이 나를 어떻게 생각할지 아직도 무서워.",
      "EMOTION": "참자니 몸이 힘들고, 말하자니 부끄러워서 마음이 복잡해.",
      "SOLUTION": "어떻게 하면 좋을지 도무지 방법을 모르겠어."
    }'::jsonb,
    2, 4
);

-- 장면 4. 전개2 (sc_banggui_04) — 며느리의 엄청난 방귀
insert into story_scenes (id, story_id, scene_order, scene_type, scene_description, image_url) values
(
    '33333333-3333-3333-3333-000000000004',
    '11111111-1111-1111-1111-111111111111',
    4, 'STORY',
    '며느리는 더 이상 참을 수 없어 몰래 살짝만 방귀를 뀌려고 합니다. 하지만 오래 참았던 탓에 방귀가 크게 터져 나왔습니다. 마당의 먼지가 휘리릭 날아가고, 기왓장이 달그락거리고, 시아버지의 갓까지 휙 날아가 버렸습니다.',
    '/stories/banggui/scenes/04_bigfart.png'
);

-- 장면 5. 대화2 (sc_banggui_05) — 시아버지와의 대화
insert into story_scenes (id, story_id, scene_order, scene_type, scene_description, conflict, image_url,
                          character_name, character_persona, character_opening, character_closing,
                          scene_goal, required_elements, element_criteria, remaining_worries,
                          preferred_turns, max_turns) values
(
    '33333333-3333-3333-3333-000000000005',
    '11111111-1111-1111-1111-111111111111',
    5, 'DIALOGUE',
    '시아버지가 며느리의 요란한 방귀에 깜짝 놀라 화를 내며, 이런 며느리와는 함께 살 수 없다고 말한다.',
    '시아버지는 창피한 며느리와 함께 살 수 없다고 생각하지만, 며느리는 일부러 그런 것이 아니다.',
    '/stories/banggui/scenes/05_dialogue2.png',
    '시아버지',
    '체면을 중시하고 호들갑스러우며, 고집이 있지만 익살스러운 어른. 집안의 체면과 다른 사람의 시선을 중요하게 생각한다. 놀라면 반응이 크고 과장되어 웃음을 준다. 아이의 말에 반박하거나 따져 묻기도 하지만 호통치거나 위압적으로 대하지 않으며, 일리가 있으면 인정하지만 곧바로 결정을 뒤집지는 않는다. 갈등 상황에서도 동화적인 재미를 유지한다.',
    '아이고, 이게 무슨 일이냐! 우리 집안이 다 흔들리는구나! 이렇게 창피한 며느리와 함께 못 살겠다! 그렇지 않니?',
    '흥, 그래도 도저히 이런 며느리와는 함께 살 수 없으니 친정으로 데려다줘야겠다.',
    '시아버지의 관점을 이해하면서도, 며느리가 그렇게 행동한 이유를 설명하고 며느리를 이해해 달라고 요청한다.',
    array['PERSPECTIVE', 'EMPATHY', 'REASON', 'REQUEST'],
    '{
      "PERSPECTIVE": "시아버지 또는 며느리의 상황·입장을 고려해 말함 (예: 며느리도 일부러 그런 게 아니에요)",
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
);

-- 장면 6. 전개3 (sc_banggui_06) — 높은 배나무를 만난 시아버지와 며느리
insert into story_scenes (id, story_id, scene_order, scene_type, scene_description, image_url) values
(
    '33333333-3333-3333-3333-000000000006',
    '11111111-1111-1111-1111-111111111111',
    6, 'STORY',
    '한참 걷다 보니 아랫마을 길가에 아주 높은 배나무가 한 그루 서 있었습니다. 나무 꼭대기에는 노랗고 탐스러운 배들이 주렁주렁 매달려 있었습니다. 시아버지는 배를 보자 군침이 돌았습니다. 마침 아랫마을 사람들도 그 배를 먹고 싶어 했지만, 나무가 너무 높아 아무도 딸 수 없었습니다.',
    '/stories/banggui/scenes/06_peartree.png'
);

-- 장면 7. 대화3 (sc_banggui_07) — 마을 이장과의 대화 + 미션1
insert into story_scenes (id, story_id, scene_order, scene_type, scene_description, conflict, image_url,
                          character_name, character_persona, character_opening, character_closing,
                          scene_goal, required_elements, element_criteria, remaining_worries,
                          mission_config, preferred_turns, max_turns) values
(
    '33333333-3333-3333-3333-000000000007',
    '11111111-1111-1111-1111-111111111111',
    7, 'DIALOGUE',
    '마을 이장이 너무 높아 아무도 딸 수 없는 배나무를 두고 좋은 방법이 없는지 고민하고 있다.',
    '탐스러운 배가 열렸지만 나무가 너무 높아 긴 장대로도 닿지 않고, 올라갈 수도 없다.',
    '/stories/banggui/scenes/07_dialogue3.png',
    '마을 이장',
    '마을 일을 챙기며 현실적인 문제 해결을 중요하게 생각하는, 친근하고 반응이 큰 어른. 마을의 불편이나 문제를 먼저 살피고 사람들의 의견을 모은다. 특이하거나 낯선 방법이라도 실제로 도움이 된다면 받아들이며, 새로운 생각을 들으면 "그게 정말 되겠소?" 하며 관심을 보인다. 해결 방법만큼 주변 사람들이 다치지 않는지도 신경 쓰고, 좋은 결과는 편견 없이 감탄하고 칭찬한다.',
    '이 배나무는 해마다 탐스러운 배가 열리지만, 너무 높아서 아무도 딸 수가 없었단다. 무슨 뾰족한 방법이 없겠는가?',
    '아이고, 방귀 뀌는 며느리 덕분에 온 마을이 배 잔치를 할 수 있겠구려, 고맙소!',
    '높은 배나무의 배를 떨어뜨릴 구체적인 해결 방법을 제안하고, 그 방법이 가능한 까닭과 부탁하는 방법, 예상되는 결과까지 이야기한다.',
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
      "name": "높이 있는 배 따기 미션",
      "purpose": "높은 배나무의 배를 떨어뜨리기 위해 며느리의 큰 방귀를 안전하게 사용하는 방법을 구성한다.",
      "check_points": ["무엇을 사용할 것인지", "왜 그 방법이 가능한지", "며느리에게 어떻게 부탁할 것인지", "그 결과 어떤 일이 생길지"],
      "exposure_principle": "대화 시작과 동시에 보여주지 않고, 해결 방법을 실제로 구성해야 하는 시점에 노출한다.",
      "exposure_conditions": [
        "아이가 며느리의 방귀를 활용할 수 있다고 제안한 경우",
        "아이가 해결 방향은 말했지만 방법이 구체적이지 않은 경우",
        "2회 이상 대화했지만 실행 방법이 나오지 않은 경우",
        "캐릭터 질문만으로 해결 방법을 구체화하기 어려운 경우"
      ]
    }'::jsonb,
    3, 5
);

-- 장면 8. 전개4 (sc_banggui_08) — 후회하고 사과하는 시아버지
insert into story_scenes (id, story_id, scene_order, scene_type, scene_description, image_url) values
(
    '33333333-3333-3333-3333-000000000008',
    '11111111-1111-1111-1111-111111111111',
    8, 'STORY',
    '시아버지는 며느리의 방귀가 시끄럽고 별난 것이 아니라, 모두를 도울 수 있는 특별한 힘이라는 것을 깨닫습니다. 자신이 며느리를 구박했던 일을 후회하고 사과합니다.',
    '/stories/banggui/scenes/08_apology.png'
);

-- 장면 9. 대화4 (sc_banggui_09) — 방귀쟁이 며느리와의 마지막 대화 + 미션2
insert into story_scenes (id, story_id, scene_order, scene_type, scene_description, conflict, image_url,
                          character_name, character_persona, character_opening, character_closing,
                          scene_goal, required_elements, element_criteria, remaining_worries,
                          mission_config, preferred_turns, max_turns) values
(
    '33333333-3333-3333-3333-000000000009',
    '11111111-1111-1111-1111-111111111111',
    9, 'DIALOGUE',
    '며느리가 자신의 방귀가 누군가에게 도움이 될 수 있다는 것을 처음 알고, 이제는 부끄러워하지 않아도 되는지 아이에게 묻는다.',
    '자신의 특징이 도움이 된다는 것을 알았지만, 아직 부끄러운 마음이 남아 있다.',
    '/stories/banggui/scenes/09_dialogue4.png',
    '방귀쟁이 며느리',
    '남을 많이 의식해 조심스럽지만, 자신의 모습을 조금씩 받아들이는 따뜻한 인물. 자신의 능력을 과시하기보다는 다른 사람을 돕는 데 사용한다. 이 장면에서는 자신의 특징이 무조건 나쁜 것만은 아니라는 점을 알아가며 조금씩 당당해지고 있지만, 아직 부끄러움이 남아 있는 상태다.',
    'ㅇㅇ이 덕분에 내 방귀가 누군가에게 도움이 될 수 있다는 걸 처음 알았어. 이제는 방귀 소리가 큰 걸 부끄러워하지 않아도 될까?',
    '이제는 부끄러워하며 숨기지 않고, 조심해서 좋은 일에 써 볼게.',
    '단점처럼 보이는 특징을 다른 관점에서 바라보고, 장점이나 가능성으로 바꾸어 말하며 며느리의 달라진 마음을 함께 이야기한다.',
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
      "name": "친구들의 단점을 장점으로 바꾸기 미션",
      "purpose": "친구나 주변 사람의 특징을 다른 관점에서 바라보고, 단점처럼 보이는 특징을 장점이나 가능성으로 바꾸어 말한다.",
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
);

-- ------------------------------------------------------------
-- 4. characters — 캐릭터 레지스트리 (3명)
--    장면에 흩어져 있던 캐릭터 속성을 모은다. personality는 장면과 무관한 공통 성격이고,
--    장면마다 달라지는 부분은 story_scenes.scene_stance에 둔다.
--
--    story_scenes.character_persona는 기존 파이프라인 호환을 위해 그대로 남겨 둔다 —
--    지금은 personality + scene_stance와 내용이 겹친다.
--    TODO: 캐릭터 LLM 프롬프트를 characters + scene_stance로 옮기면 character_persona를 없앤다.
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
);

-- ------------------------------------------------------------
-- 5. story_scenes 보강 — 캐릭터 참조 · 장면별 입장 · STT 고유명사 힌트
--    scene_stance는 기존 character_persona의 "이 장면에서는 …" 부분을 옮긴 것이다.
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
-- 6. items — 꾸미기 아이템 마스터 16종
--    가격: 소품 3 / 중형 5 / 대형·동물 10
--    해금: ALWAYS 12 / STARDUST_CUMULATIVE 3 / STORY_COMPLETE 1
--
--    토끼·거북이는 후속 이야기(토끼전 등) 완주 보상으로 예약된 것이지만,
--    아직 그 이야기가 없어 unlock_story_id를 채울 수 없다 —
--    스키마 check가 STORY_COMPLETE에 대상 이야기를 요구하므로 누적 해금으로 둔다.
--    TODO: 후속 이야기가 들어오면 STORY_COMPLETE + unlock_story_id로 바꾼다.
--
--    model_url·thumbnail_url은 Kenney CC0 에셋 업로드 후 실제 경로로 교체한다(플레이스홀더).
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
('44444444-4444-4444-4444-000000000010', '거북이',   'ANIMAL',       10, 'STARDUST_CUMULATIVE', null, 50, '/items/models/turtle.glb', '/items/thumbs/turtle.png', 16);

-- ------------------------------------------------------------
-- 7. parents — 보호자 2명 (이메일 가입 1 · 카카오 1)
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
);

-- ------------------------------------------------------------
-- 8. children — 아이 3명
--    아이 생성 API는 planets·stardust_wallets를 이벤트로 함께 만들지만,
--    시드는 API를 거치지 않으므로 아래 8·12번에서 직접 넣는다.
-- ------------------------------------------------------------
insert into children (id, parent_id, name, birth_year) values
('aaaaaaaa-aaaa-aaaa-aaaa-000000000001', '99999999-9999-9999-9999-000000000001', '지우', 2018),
('aaaaaaaa-aaaa-aaaa-aaaa-000000000002', '99999999-9999-9999-9999-000000000001', '하준', 2019),
('aaaaaaaa-aaaa-aaaa-aaaa-000000000003', '99999999-9999-9999-9999-000000000002', '서연', 2017);

-- ------------------------------------------------------------
-- 9. child_consents — 아동 개인정보 처리 동의
--    지우: 유효 / 하준: 유효 / 서연: 철회됨(withdrawn_at) — 세션 생성이 막히는 경우 확인용
-- ------------------------------------------------------------
insert into child_consents (id, child_id, consent_version, verification_method, consented_at, withdrawn_at) values
('11110000-0000-0000-0000-000000000001', 'aaaaaaaa-aaaa-aaaa-aaaa-000000000001',
 'mvp_v1', 'AUTHENTICATED_PARENT', now() - interval '30 days', null),
('11110000-0000-0000-0000-000000000002', 'aaaaaaaa-aaaa-aaaa-aaaa-000000000002',
 'mvp_v1', 'AUTHENTICATED_PARENT', now() - interval '20 days', null),
('11110000-0000-0000-0000-000000000003', 'aaaaaaaa-aaaa-aaaa-aaaa-000000000003',
 'mvp_v1', 'AUTHENTICATED_PARENT', now() - interval '40 days', now() - interval '2 days');

-- ------------------------------------------------------------
-- 10. refresh_tokens — 리프레시 토큰
--    원문은 저장하지 않고 해시만 둔다. 아래 값은 실제 발급 토큰의 해시가 아니라
--    자리 확인용이라 이 행으로는 재발급이 되지 않는다 — 실제 값은 로그인으로만 생긴다.
--    유효 1건 + 로그아웃으로 무효화된 1건.
-- ------------------------------------------------------------
insert into refresh_tokens (id, parent_id, token_hash, expires_at, revoked_at) values
('22220000-0000-0000-0000-000000000001', '99999999-9999-9999-9999-000000000001',
 encode(digest('demo-refresh-token-active', 'sha256'), 'hex'),
 now() + interval '14 days', null),
('22220000-0000-0000-0000-000000000002', '99999999-9999-9999-9999-000000000001',
 encode(digest('demo-refresh-token-revoked', 'sha256'), 'hex'),
 now() + interval '7 days', now() - interval '1 day');

-- ------------------------------------------------------------
-- 11. story_sessions — 세션 3개
--    A 완주 세션(리포트·후속 활동·별가루 지급까지 완결)
--    B 대화 진행 중(장면5, 유도 1회 사용 — 장면 보너스 대상 아님)
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
);

-- ------------------------------------------------------------
-- 12. messages — 대화 기록
--    turn_order는 세션 안에서 유일하다(장면이 바뀌어도 이어진다).
--    도입·전개 내레이션은 저장하지 않는다 — 콘텐츠에서 재생한다.
--    stt_confidence는 아이 발화에만 있고, 낮은 신뢰 1건을 섞어 뒀다.
-- ------------------------------------------------------------
insert into messages (id, session_id, scene_id, speaker_type, turn_order, text,
                      stt_raw_text, stt_confidence, stt_low_confidence, stt_retry_count,
                      character_emotion, created_at) values
-- 세션 A · 장면3 (며느리)
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
-- 세션 A · 장면9 (며느리, 마지막 대화)
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
-- 세션 B · 장면5 (시아버지, 진행 중 — 마지막 응답이 유도 모드)
('cccccccc-cccc-cccc-cccc-000000000009', 'bbbbbbbb-bbbb-bbbb-bbbb-000000000002',
 '33333333-3333-3333-3333-000000000005', 'CHARACTER', 1,
 '아이고, 이게 무슨 일이냐! 우리 집안이 다 흔들리는구나! 이렇게 창피한 며느리와 함께 못 살겠다! 그렇지 않니?',
 null, null, false, 0, 'SURPRISED', now() - interval '40 minutes'),
('cccccccc-cccc-cccc-cccc-00000000000a', 'bbbbbbbb-bbbb-bbbb-bbbb-000000000002',
 '33333333-3333-3333-3333-000000000005', 'CHILD', 2,
 '음... 며느리도 그러고 싶어서 그런 건 아닐 거예요.',
 '음 며느리도 그러고 시퍼서 그런건 아닐꺼예요', 0.520, true, 2,
 null, now() - interval '38 minutes'),
('cccccccc-cccc-cccc-cccc-00000000000b', 'bbbbbbbb-bbbb-bbbb-bbbb-000000000002',
 '33333333-3333-3333-3333-000000000005', 'CHARACTER', 3,
 '흠, 일부러 그런 게 아니라고? 그래도 어째서 그렇게 요란한 방귀를 뀌게 되었는지 까닭을 모르겠구나.',
 null, null, false, 0, 'WORRIED', now() - interval '35 minutes');

-- ------------------------------------------------------------
-- 13. utterance_analyses — 아이 발화 분석 (CHILD 메시지 1건당 1건)
--    detected_elements는 서버 후처리를 통과한 요소만 담는다.
--    dropped_evidence는 근거 문구가 발화 원문에 없어 폐기된 것 — 프롬프트 개선 추적용.
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
 'VALID', 'mvp_v1', 'gemini-2.5-flash', '[]'::jsonb);

-- ------------------------------------------------------------
-- 14. planets / stardust_wallets — 아이당 1개씩
--    운영에서는 아이 생성 시 ChildCreatedEvent로 함께 만들어진다.
-- ------------------------------------------------------------
insert into planets (id, child_id, name, tutorial_completed) values
('eeee0000-0000-0000-0000-000000000001', 'aaaaaaaa-aaaa-aaaa-aaaa-000000000001', '지우의 행성', true),
('eeee0000-0000-0000-0000-000000000002', 'aaaaaaaa-aaaa-aaaa-aaaa-000000000002', '내 행성', false),
('eeee0000-0000-0000-0000-000000000003', 'aaaaaaaa-aaaa-aaaa-aaaa-000000000003', '내 행성', false);

-- 지우: 완주 3 + 장면 보너스 2 + 데모 보정 20 = 누적 25, 구매 11 -> 잔액 14
insert into stardust_wallets (id, child_id, balance, total_earned) values
('ffff0000-0000-0000-0000-000000000001', 'aaaaaaaa-aaaa-aaaa-aaaa-000000000001', 14, 25),
('ffff0000-0000-0000-0000-000000000002', 'aaaaaaaa-aaaa-aaaa-aaaa-000000000002',  0,  0),
('ffff0000-0000-0000-0000-000000000003', 'aaaaaaaa-aaaa-aaaa-aaaa-000000000003',  0,  0);

-- ------------------------------------------------------------
-- 15. stardust_transactions — 별가루 증감 이력
--    장면 보너스가 한 세션에 2건 들어간다 — scene_id를 나눠 넣어야 멱등 인덱스에 걸리지 않는다.
--    acknowledged=false인 행이 있으면 행성 진입 시 떨어지는 연출을 재생한다.
-- ------------------------------------------------------------
insert into stardust_transactions (id, wallet_id, amount, reason, session_id, scene_id, item_id, acknowledged, created_at) values
-- 완주 보상 (세션 단위, scene_id 없음)
('a1110000-0000-0000-0000-000000000001', 'ffff0000-0000-0000-0000-000000000001',
  3, 'STORY_COMPLETED', 'bbbbbbbb-bbbb-bbbb-bbbb-000000000001', null, null, true, now() - interval '3 days'),
-- 장면 보너스 2건 (유도 없이 목표 통과한 장면 3·9)
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
 -5, 'ITEM_PURCHASE', null, null, '44444444-4444-4444-4444-000000000007', true, now() - interval '19 hours');

-- ------------------------------------------------------------
-- 16. child_story_play_counts — 이야기별 완주 횟수
--     지우는 방귀 이야기를 1회 완주했다. 다음 완주는 절반(1), 3회차부터는 지급 없음.
-- ------------------------------------------------------------
insert into child_story_play_counts (child_id, story_id, play_count) values
('aaaaaaaa-aaaa-aaaa-aaaa-000000000001', '11111111-1111-1111-1111-111111111111', 1);

-- ------------------------------------------------------------
-- 17. child_items / planet_items — 보유 아이템과 배치
--     좌표는 프론트와 같은 축좌표(q, r). 원점 기준이라 음수가 나온다.
--     배치되지 않은 child_items가 보관함이다 — 풀 1개가 보관함에 남아 있다.
-- ------------------------------------------------------------
insert into child_items (id, child_id, item_id, acquired_at) values
('b2220000-0000-0000-0000-000000000001', 'aaaaaaaa-aaaa-aaaa-aaaa-000000000001',
 '44444444-4444-4444-4444-000000000001', now() - interval '20 hours'),  -- 돌
('b2220000-0000-0000-0000-000000000002', 'aaaaaaaa-aaaa-aaaa-aaaa-000000000001',
 '44444444-4444-4444-4444-000000000002', now() - interval '20 hours'),  -- 풀 (보관함)
('b2220000-0000-0000-0000-000000000003', 'aaaaaaaa-aaaa-aaaa-aaaa-000000000001',
 '44444444-4444-4444-4444-000000000007', now() - interval '19 hours');  -- 작은나무

insert into planet_items (id, planet_id, child_item_id, placed_q, placed_r) values
('c3330000-0000-0000-0000-000000000001', 'eeee0000-0000-0000-0000-000000000001',
 'b2220000-0000-0000-0000-000000000001',  0,  0),
('c3330000-0000-0000-0000-000000000002', 'eeee0000-0000-0000-0000-000000000001',
 'b2220000-0000-0000-0000-000000000003',  1, -1);

-- ------------------------------------------------------------
-- 18. mission_results — 미션 결과 (세션·미션당 1건)
-- ------------------------------------------------------------
insert into mission_results (id, session_id, scene_id, mission_id, mission_type, result) values
('d4440000-0000-0000-0000-000000000001', 'bbbbbbbb-bbbb-bbbb-bbbb-000000000001',
 '33333333-3333-3333-3333-000000000007', 'mission_1', 'PROBLEM_SOLVING',
 '{"answers": {"who": "며느리가", "how": "방귀를 세게 뀌어서", "why": "장대로는 닿지 않으니까", "result": "배가 우수수 떨어졌어요"}}'::jsonb),
('d4440000-0000-0000-0000-000000000002', 'bbbbbbbb-bbbb-bbbb-bbbb-000000000001',
 '33333333-3333-3333-3333-000000000009', 'mission_2', 'PERSPECTIVE_SHIFT',
 '{"cards": [{"trait": "목소리가 큰 친구", "strength": "멀리 있는 사람을 부를 수 있어요"}, {"trait": "질문이 많은 친구", "strength": "새로운 생각을 찾을 수 있어요"}]}'::jsonb);

-- ------------------------------------------------------------
-- 19. post_activity_results — 말하기 후 활동 (세션당 1건)
--     card_order_seed로 카드 셔플을 고정한다 — 재진입해도 같은 순서가 나온다.
--     attempt_count=2는 한 번 틀린 뒤 맞혔다는 뜻이다.
-- ------------------------------------------------------------
insert into post_activity_results (id, session_id, card_order_seed, submitted_order,
                                   is_order_correct, attempt_count, retelling_text, completed_at) values
('e5550000-0000-0000-0000-000000000001', 'bbbbbbbb-bbbb-bbbb-bbbb-000000000001',
 'seed-banggui-0001',
 array['card_1', 'card_2', 'card_3', 'card_4', 'card_5'],
 true, 2,
 '며느리가 방귀를 참다가 배가 아팠어요. 참았던 방귀가 터져서 집이 흔들렸어요. 시아버지가 화나서 친정에 데려다주려고 했는데, 며느리가 방귀로 높은 배나무에서 배를 떨어뜨려서 모두가 좋아했어요.',
 now() - interval '3 days' + interval '22 minutes');

-- ------------------------------------------------------------
-- 20. reports — 보호자 리포트 (세션당 1건)
--     대표 발화는 저장하지 않고 조회 시 messages에서 구성한다.
-- ------------------------------------------------------------
insert into reports (id, session_id, summary, strengths, next_focus) values
('f6660000-0000-0000-0000-000000000001', 'bbbbbbbb-bbbb-bbbb-bbbb-000000000001',
 '지우는 며느리의 마음을 먼저 헤아리고, 큰 방귀를 단점이 아니라 남을 돕는 장점으로 바꾸어 말했습니다. 이야기 전체를 순서대로 다시 들려주는 것도 잘 해냈습니다.',
 '[{"element": "EMOTION", "comment": "상대가 힘들겠다는 마음을 먼저 말해 주었어요."},
   {"element": "PERSPECTIVE", "comment": "같은 일을 다른 쪽에서 바라보고 장점으로 바꾸어 말했어요."}]'::jsonb,
 '[{"element": "REASON", "comment": "왜 그렇게 생각했는지 까닭을 덧붙이면 더 잘 전해져요."}]'::jsonb);

-- ------------------------------------------------------------
-- 21. wordbook — 단어장
--     meaning이 비어 있으면 서버가 LLM으로 아이 수준의 뜻을 채운다 — 미채움 상태 1건을 둔다.
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
 'FAVORITE', '33333333-3333-3333-3333-000000000007');

-- ------------------------------------------------------------
-- 22. scene_audio — TTS 사전 생성 음성 13건
--     내레이션 5(도입·전개1~4) + 고정 첫·마지막 대사 8 = 13.
--
--     ⚠ engine·voice·duration_ms·sentence_timings는 PLACEHOLDER다.
--       이 값들은 실제 렌더 산출물의 메타데이터라 만들어 낼 수 없다 —
--       TTS초안/{chirp3,gemini}/manifest.json의 실측값으로 반드시 교체한다.
--
--     text_hash만은 진짜다. 렌더 원본 텍스트에서 그대로 계산하므로,
--     대사를 고치면 곧바로 불일치가 잡힌다(음성이 조용히 옛것으로 남는 사고 방지).
-- ------------------------------------------------------------
insert into scene_audio (scene_id, slot, child_id, storage_path, text_hash,
                         engine, voice, duration_ms, sentence_timings)
select s.id, 'NARRATION', null,
       'tts/banggui/sc_banggui_0' || s.scene_order || '.mp3',
       encode(digest(s.scene_description, 'sha256'), 'hex'),
       'PLACEHOLDER', 'PLACEHOLDER', 1, '[]'::jsonb
from story_scenes s
where s.story_id = '11111111-1111-1111-1111-111111111111'
  and s.scene_type = 'STORY';

insert into scene_audio (scene_id, slot, child_id, storage_path, text_hash,
                         engine, voice, duration_ms, sentence_timings)
select s.id, 'OPENING', null,
       'tts/banggui/sc_banggui_0' || s.scene_order || '_opening.mp3',
       encode(digest(s.character_opening, 'sha256'), 'hex'),
       'PLACEHOLDER', 'PLACEHOLDER', 1, '[]'::jsonb
from story_scenes s
where s.story_id = '11111111-1111-1111-1111-111111111111'
  and s.scene_type = 'DIALOGUE';

insert into scene_audio (scene_id, slot, child_id, storage_path, text_hash,
                         engine, voice, duration_ms, sentence_timings)
select s.id, 'CLOSING', null,
       'tts/banggui/sc_banggui_0' || s.scene_order || '_closing.mp3',
       encode(digest(s.character_closing, 'sha256'), 'hex'),
       'PLACEHOLDER', 'PLACEHOLDER', 1, '[]'::jsonb
from story_scenes s
where s.story_id = '11111111-1111-1111-1111-111111111111'
  and s.scene_type = 'DIALOGUE';

commit;

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
