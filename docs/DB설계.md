# 굿퀘스천 DB 설계

> PostgreSQL. Supabase Auth 미사용 — 인증 포함 전 테이블을 자체 스키마로 관리한다.
> 스키마 원본은 [`db/schema.sql`](../src/main/resources/db/schema.sql)이고 이 문서는 그 설계 근거다.
> **불일치가 생기면 `schema.sql`이 맞다.** `ddl-auto=validate`라 엔티티도 함께 맞춰야 앱이 뜬다.

| | |
|---|---|
| 버전 | v4 (24개 테이블) |
| 신규 생성 | `schema.sql` |
| v3 → v4 이행 | `migration-to-v4.sql` (재실행 안전) |
| 시드 | `seed.sql` (콘텐츠 + 데모 계정, 단일 트랜잭션) |

---

## 1. 테이블 총괄

| # | 테이블 | 역할 |
|---|---|---|
| **계정** | | |
| 1 | `parents` | 보호자 계정(이메일·소셜) |
| 2 | `refresh_tokens` | 리프레시 토큰 회전·무효화 |
| 3 | `children` | 아이 프로필 |
| 4 | `child_consents` | 아동 개인정보 처리 동의 이력 |
| **콘텐츠** | | |
| 5 | `stories` | 이야기 메타 + 후속 활동 설정 |
| 6 | `topics` | 주제 마스터 |
| 7 | `story_topics` | 이야기–주제 연결(M:N) |
| 8 | `characters` | 캐릭터 레지스트리(페르소나·TTS 화자·표정) |
| 9 | `story_scenes` | 장면 콘텐츠 + 대화 기준 |
| 10 | `scene_audio` | TTS 사전 생성 음성 포인터 |
| **세션·대화** | | |
| 11 | `story_sessions` | 진행 상태(장면·누적 요소·모드·안전) |
| 12 | `messages` | 아이·캐릭터·시스템 발화 |
| 13 | `utterance_analyses` | 아이 발화 분석 결과 |
| 14 | `mission_results` | 미션 수행 결과 |
| **학습 결과** | | |
| 15 | `post_activity_results` | 말하기 후 활동 결과 |
| 16 | `reports` | 보호자 리포트 |
| 17 | `wordbook` | 단어장 |
| **보상(행성 꾸미기)** | | |
| 18 | `stardust_wallets` | 아이별 별가루 지갑 |
| 19 | `stardust_transactions` | 별가루 증감 이력 |
| 20 | `child_story_play_counts` | 이야기별 완주 횟수 |
| 21 | `items` | 꾸미기 아이템 마스터 |
| 22 | `child_items` | 보유 아이템(보관함의 원천) |
| 23 | `planets` | 아이의 행성(1:1) |
| 24 | `planet_items` | 행성 격자 배치 |

**공통 규약**

- PK는 전부 `uuid` (`gen_random_uuid()`). 콘텐츠 문서의 `sc_banggui_01` 같은 슬러그는 사람이 읽기 위한 예시 표기일 뿐이라 별도 컬럼으로 두지 않는다.
- 코드값은 대문자 스네이크케이스로 통일하고 서버 enum과 1:1로 맞춘다.
- 시각은 전부 `timestamptz`.
- 파생 가능한 값은 저장하지 않는다 (뒤의 §9 참고).

---

## 2. 계정·아이

### 2.1 `parents`

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| `id` | uuid | PK | |
| `email` | varchar(255) | 부분 UNIQUE | 이메일 계정만 값이 있다 |
| `password_hash` | varchar(100) | | bcrypt(60자). 소셜 계정은 null |
| `provider` | varchar(20) | NOT NULL | `LOCAL` / `KAKAO` |
| `provider_id` | varchar(100) | | 소셜 제공자 측 식별자 |
| `name` | varchar(50) | NOT NULL | |
| `created_at` | timestamptz | NOT NULL | |

```sql
create unique index idx_parents_email on parents(email) where email is not null;
create unique index idx_parents_provider_id on parents(provider, provider_id) where provider_id is not null;
```

**왜 부분 인덱스인가** — 이메일 계정은 `provider_id`가, 소셜 계정은 `email`이 비어 있다. 일반 UNIQUE는 NULL을 서로 다른 값으로 취급해 소셜 계정 중복을 못 막는다. NULL 행을 인덱스에서 빼야 중복 방지가 실제로 걸린다.

**왜 `LOCAL` 센티널인가** — "이메일 계정이면 provider가 null"로 두면 위 인덱스의 첫 컬럼이 NULL이 되어 같은 문제가 반복된다. 값을 채워 두면 판별도 인덱스도 단순해진다.

### 2.2 `refresh_tokens`

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| `id` | uuid | PK | |
| `parent_id` | uuid | FK→parents, NOT NULL | |
| `token_hash` | varchar(100) | UNIQUE, NOT NULL | 원문 미저장 |
| `expires_at` | timestamptz | NOT NULL | |
| `revoked_at` | timestamptz | | 로그아웃·회전 시 기록 |
| `created_at` | timestamptz | NOT NULL | |

토큰 원문은 저장하지 않는다. DB가 유출돼도 그 자체로는 재발급에 쓸 수 없어야 한다.

### 2.3 `children`

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| `id` | uuid | PK | |
| `parent_id` | uuid | FK→parents ON DELETE CASCADE, NOT NULL | |
| `name` | varchar(50) | NOT NULL | 대사의 "ㅇㅇ" 치환에 쓴다 |
| `birth_year` | smallint | NOT NULL, CHECK 2000~2100 | 연령 = 현재연도 − birth_year |
| `created_at` | timestamptz | NOT NULL | |

**아이 생성 시 `planets`·`stardust_wallets`가 1건씩 함께 만들어진다.** 둘 다 `child_id`가 UNIQUE라 나중에 lazy 생성하면 동시 요청에서 충돌한다.

`user` 패키지는 `learning`을 의존할 수 없어(ArchUnit 규칙) `ChildService`가 직접 만들지 않는다. `ChildCreatedEvent`를 발행하고 `RewardProvisioningListener`가 **같은 트랜잭션에서 동기로** 받아 만든다 — 실패하면 아이 생성도 함께 롤백된다.

### 2.4 `child_consents`

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| `id` | uuid | PK | |
| `child_id` | uuid | FK→children, NOT NULL | |
| `consent_version` | varchar(30) | NOT NULL | 예: `mvp_v1` |
| `verification_method` | varchar(30) | NOT NULL | `AUTHENTICATED_PARENT` / `INSTITUTION_PAPER` / `MOBILE_VERIFICATION` |
| `consented_at` | timestamptz | NOT NULL | |
| `withdrawn_at` | timestamptz | | null = 유효 |

세션 생성 시 유효 동의(`withdrawn_at IS NULL`)를 검증한다. 철회는 행 삭제가 아니라 시각 기록이다 — 동의했던 사실 자체가 이력으로 남아야 한다.

---

## 3. 콘텐츠

콘텐츠는 읽기 전용 참조다. **세션에 사용된 이야기는 수정하지 않고 복사해 새 id로 등록한다.** 진행 중인 세션의 근거가 바뀌면 분석·리포트를 재현할 수 없기 때문이다.

### 3.1 `stories`

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| `id` | uuid | PK | |
| `title` | varchar(100) | NOT NULL | |
| `summary` | text | NOT NULL | |
| `child_role` | varchar(50) | | 상세 화면 "아이 역할" |
| `intro` | text | | 상세 화면 도입·상황 소개 |
| `image_url` | text | | 대표 이미지 |
| `difficulty` | varchar(20) | NOT NULL | |
| `estimated_minutes` | smallint | | |
| `post_activity_config` | jsonb | | `cards[{id,text,correct_order}]`, `retelling_keywords[]` |
| `status` | varchar(20) | NOT NULL | `DRAFT` / `PUBLISHED` / `ARCHIVED` |
| `created_at` | timestamptz | NOT NULL | |

**후속 활동 카드를 별도 테이블로 만들지 않는다.** 카드는 이야기에 종속된 5장짜리 고정 데이터이고 개별 조회·검색 대상이 아니다. 테이블을 나누면 이야기 복사가 조인 복사로 번진다.

### 3.2 `topics` / `story_topics`

- `topics`: `id`, `name` varchar(30) UNIQUE NOT NULL, `display_order` smallint, `created_at`
- `story_topics`: `(story_id, topic_id)` 복합 PK, 각각 FK

### 3.3 `characters`

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| `id` | uuid | PK | |
| `story_id` | uuid | FK→stories, NOT NULL | |
| `character_key` | varchar(64) | NOT NULL | 표정 이미지 파일명의 키 |
| `name` | varchar(50) | NOT NULL | 화면 표시 이름 |
| `personality` | text | NOT NULL | 성격·말투(캐릭터 LLM 페르소나) |
| `guidance_style` | text | | 유도를 어떻게 드러낼지(GUIDED 모드 입력) |
| `tts_voice` | varchar(64) | | |
| `tts_style` | text | | 연기 지시문. 성별·연령을 반드시 포함 |
| `tts_gender` | varchar(10) | CHECK MALE/FEMALE | 합성 결과 검증 기대값 |
| `expression_keys` | text[] | NOT NULL DEFAULT `{}` | 이 캐릭터가 실제로 가진 표정 |
| `created_at` | timestamptz | NOT NULL | |

UNIQUE(`story_id`, `character_key`)

**왜 장면에서 분리했나 — TTS 화자 고정 때문이다.** 페르소나가 장면마다 따로 있으면 같은 캐릭터가 장면별로 다른 목소리로 합성되는 것을 막을 구조가 없다. 아이는 목소리가 달라진 것을 어른보다 훨씬 민감하게 알아챈다.

**`character_key`는 함부로 바꾸면 안 된다.** 표정 이미지 파일명이 `{character_key}_{expression}.png`라 키가 곧 매핑 테이블이다.

**`tts_gender`가 필요한 이유** — 보이스 이름이 성별을 보장하지 않는다. 실측에서 같은 시아버지 대사가 첫 대사 193Hz(여성), 마지막 대사 134Hz(남성)로 나왔다. 문장별 F0 검증에 쓸 기대값이 필요하다.

### 3.4 `story_scenes`

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| `id` | uuid | PK | |
| `story_id` | uuid | FK→stories, NOT NULL | |
| `scene_order` | smallint | NOT NULL | UNIQUE(story_id, scene_order) |
| `scene_type` | varchar(20) | NOT NULL, CHECK | `STORY`(내레이션) / `DIALOGUE`(대화) |
| `scene_description` | text | NOT NULL | 장면 본문. 내레이션 문장 분리는 서버가 한다 |
| `conflict` | text | | 갈등 요약 |
| `image_url` | text | | |
| `character_id` | uuid | FK→characters ON DELETE SET NULL | |
| `character_name` | varchar(50) | \* | 화면 표시용 |
| `scene_stance` | text | | 장면별 입장 |
| `proper_nouns` | text[] | NOT NULL DEFAULT `{}` | STT 디코딩 힌트 |
| `character_persona` | text | | 캐릭터 LLM 입력(레거시, §10 참고) |
| `character_opening` | text | \* | 고정 첫 대사 |
| `character_closing` | text | | 고정 마지막 대사 |
| `scene_goal` | text | \* | |
| `required_elements` | text[] | \* | 사고 요소 8종의 부분집합 |
| `element_criteria` | jsonb | NOT NULL DEFAULT `{}` | 요소별 장면 인정 기준 |
| `remaining_worries` | jsonb | NOT NULL DEFAULT `{}` | 요소별 캐릭터 걱정(유도 재료) |
| `mission_config` | jsonb | | 미션 정의. 미션 없는 장면은 null |
| `preferred_turns` | smallint | \* | |
| `max_turns` | smallint | \* | |

\* `scene_type='DIALOGUE'`일 때 NOT NULL — **애플리케이션이 아니라 DB CHECK로 강제한다.**

```sql
check (scene_type = 'STORY' or (
    character_name is not null and character_opening is not null
    and scene_goal is not null and required_elements is not null
    and preferred_turns is not null and max_turns is not null))
check (preferred_turns is null or max_turns is null or preferred_turns <= max_turns)
```

**`scene_stance`와 `character_id`로 나눈 이유** — 같은 캐릭터라도 장면마다 입장이 다르다. 시아버지는 대화2에서 며느리를 내치려 하고 전개4에서는 후회한다. 변하지 않는 성격은 `characters.personality`에, 장면마다 바뀌는 입장은 여기에 둔다.

**`proper_nouns`** — 아동 발화는 고유명사 오인식이 가장 많다(`자라`, `별주부`, `용왕`). STT에 디코딩 힌트로 주입한다.

### 3.5 `scene_audio`

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| `id` | uuid | PK | |
| `scene_id` | uuid | FK→story_scenes, NOT NULL | |
| `slot` | varchar(20) | NOT NULL, CHECK | `NARRATION` / `OPENING` / `CLOSING` |
| `child_id` | uuid | FK→children | null이면 공용 음성 |
| `storage_path` | text | NOT NULL | 오브젝트 스토리지 경로 |
| `text_hash` | char(64) | NOT NULL | 렌더 원본 텍스트의 SHA-256 |
| `engine` | varchar(64) | NOT NULL | 예: `chirp3-hd` |
| `voice` | varchar(64) | NOT NULL | |
| `style_prompt` | text | | 연기 지시문 |
| `speaking_rate` | numeric(4,2) | | |
| `duration_ms` | integer | NOT NULL, CHECK > 0 | |
| `sentence_timings` | jsonb | NOT NULL DEFAULT `[]` | 문장별 실측 시작·끝 |
| `created_at` | timestamptz | NOT NULL | |

```sql
create unique index idx_scene_audio_shared    on scene_audio(scene_id, slot) where child_id is null;
create unique index idx_scene_audio_per_child on scene_audio(scene_id, slot, child_id) where child_id is not null;
```

**오디오 바이너리는 DB에 넣지 않는다.** 오브젝트 스토리지에 두고 포인터와 메타데이터만 남긴다.

> **혼동 주의** — `messages`·`child_consents`의 "원본 음성 미저장"은 **아이가 말한 녹음**을 남기지 않는다는 개인정보 원칙이다. TTS 산출물 저장과는 다른 문제다.

**`text_hash`가 이 테이블의 핵심이다.** 없으면 대사를 고쳤을 때 음성이 조용히 옛것으로 남는다 — 화면엔 새 문장, 스피커엔 옛 문장인 상태가 되고 아무도 눈치채지 못한다. 재생 전에 현재 텍스트의 해시와 대조해 잡는다.

**`engine`/`voice`/`style_prompt`를 남기는 이유** — 나중에 소급이 안 된다. 보이스를 바꿔 재렌더할 때 어떤 게 옛 엔진 산출물인지 모르면 전수 재생성밖에 방법이 없다.

**사전 생성 vs 실시간 경계**

| 구분 | 방식 | 저장 |
|---|---|---|
| 내레이션(도입·전개) | 사전 생성 | `scene_audio` |
| 장면 첫·마지막 고정 대사 | 사전 생성 | `scene_audio` |
| 안전 문구·기본 문구·무응답 안내 | 사전 생성 | `scene_audio` 또는 정적 에셋 |
| **장면 중간 반응 대사** | **실시간** | 세션 스코프 캐시만 |

세 번째 줄이 중요하다. 장애 상황에서 쓰이는 문구인데 하필 그때 TTS를 또 호출하는 구조면 같이 죽는다.

중간 대사도 "다시 듣기"를 위해 **세션이 끝날 때까지는 들고 있어야 한다.** 재합성하면 같은 문장이어도 미세하게 다른 음성이 나오고 아이는 그 차이를 알아챈다. DB가 아니라 세션 캐시면 충분하다.

---

## 4. 세션·대화

### 4.1 `story_sessions`

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| `id` | uuid | PK | |
| `child_id` | uuid | FK→children, NOT NULL | |
| `story_id` | uuid | FK→stories, NOT NULL | |
| `current_scene_id` | uuid | FK→story_scenes | 이어하기 기준 |
| `current_child_turn_count` | smallint | NOT NULL DEFAULT 0 | ◆ |
| `accumulated_elements` | text[] | NOT NULL DEFAULT `{}` | ◆ 현재 장면 누적 |
| `last_detected_elements` | text[] | NOT NULL DEFAULT `{}` | ◆ |
| `last_response_mode` | varchar(20) | CHECK | `NORMAL` / `GUIDED` / `CLOSING` |
| `last_guidance_target` | varchar(20) | | |
| `turns_without_new_element` | smallint | NOT NULL DEFAULT 0 | ◆ |
| `consecutive_low_information_turns` | smallint | NOT NULL DEFAULT 0 | ◆ `SHORT`/`UNCLEAR`/`OFF_TOPIC`만 카운트, `PLAYFUL` 제외 |
| `scene_goal_met` | boolean | NOT NULL DEFAULT false | ◆ |
| `scene_end_reason` | varchar(20) | CHECK | ◆ `GOAL_MET` / `MAX_TURNS` |
| `guided_used_in_scene` | boolean | NOT NULL DEFAULT false | ◆ 장면 보너스 판정 |
| `mission_exposed` | boolean | NOT NULL DEFAULT false | ◆ |
| `mission_completed` | boolean | NOT NULL DEFAULT false | ◆ |
| `safety_flagged` | boolean | NOT NULL DEFAULT false | 위험 신호 확인 필요 표시 |
| `safety_categories` | text[] | NOT NULL DEFAULT `{}` | 범주만. 발화 원문은 남기지 않는다 |
| `safety_flagged_at` | timestamptz | | |
| `status` | varchar(20) | NOT NULL, CHECK | `IN_PROGRESS` / `POST_ACTIVITY` / `COMPLETED` / `STOPPED` |
| `version` | bigint | NOT NULL DEFAULT 0 | 낙관적 락(`@Version`) |
| `started_at` / `completed_at` / `last_activity_at` | timestamptz | NN / null / NN | |

◆ 표시는 **장면 단위 상태**로, 장면 전환 시 함께 초기화된다.

```sql
create index idx_story_sessions_child_recent on story_sessions(child_id, last_activity_at desc);
create index idx_story_sessions_story_id     on story_sessions(story_id);
create index idx_story_sessions_child_status on story_sessions(child_id, status);
create index idx_story_sessions_safety       on story_sessions(safety_flagged) where safety_flagged;
```

**`version`(낙관적 락)이 필요한 이유** — 한 턴 처리는 STT → 분석 LLM → 캐릭터 LLM으로 수 초가 걸린다(실측 중앙값 4.3초). 아이가 연타하면 턴 카운터와 누적 요소가 그대로 덮어써지고, `max_turns` 종료 판정까지 어긋난다. 덮어쓰기를 조용히 허용하는 대신 충돌로 드러내 409로 바꾼다.

**`safety_*`가 필요한 이유** — 자해·가정 폭력·학대 정황이 감지되면 대사 생성을 중단하고 안전 문구로 대체한 뒤 **해당 세션에 확인 필요 표시를 남겨야 한다.** 남길 자리가 없으면 감지해도 결과가 어디에도 안 남아 감지한 의미가 사라진다. 초1~3 대상 서비스에서 이건 타협 대상이 아니다.

**저장하지 않는 것**

- `missing_elements` — `required_elements − accumulated_elements`로 매번 계산한다.
- `current_phase` — `status`와 현재 장면 유형에서 파생한다(`resolvePhase()`). 저장하면 `status`와 이중 진실이 된다.

마지막 대화가 끝나면 **항상 후속 활동을 경유한다.** 종료로 직행하는 분기는 없다.

### 4.2 `messages`

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| `id` | uuid | PK | |
| `session_id` | uuid | FK→story_sessions, NOT NULL | |
| `scene_id` | uuid | FK→story_scenes, NOT NULL | |
| `speaker_type` | varchar(20) | NOT NULL, CHECK | `CHILD` / `CHARACTER` / `SYSTEM` |
| `turn_order` | integer | NOT NULL | UNIQUE(session_id, turn_order) |
| `text` | text | NOT NULL | 고정 대사도 저장(이름 치환본) |
| `stt_raw_text` | text | | 아이 발화만 |
| `stt_confidence` | numeric(4,3) | CHECK 0~1 | 아이 발화만 |
| `stt_low_confidence` | boolean | NOT NULL DEFAULT false | 리포트 대표 발화 후보에서 제외 |
| `stt_retry_count` | smallint | NOT NULL DEFAULT 0 | 다시 말한 횟수 |
| `character_emotion` | varchar(20) | | 캐릭터 표정 키 |
| `created_at` | timestamptz | NOT NULL | |

**도입·전개 내레이션은 저장하지 않는다.** 콘텐츠에서 재생하는 고정 텍스트라 대화 기록이 아니다.

**원본 음성은 저장하지 않는다.** STT 텍스트만 남긴다.

**`character_emotion`에 CHECK를 걸지 않는 이유** — 표정 키는 캐릭터마다 다르다(`characters.expression_keys`). 고정 6종으로 묶으면 캐릭터가 늘어날 때마다 DDL을 고쳐야 한다.

**STT 신뢰도 기준값은 아직 미정이라 판정은 애플리케이션이 한다.** 지금은 저장만 하고 걸러내지 않는다.

### 4.3 `utterance_analyses`

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| `id` | uuid | PK | |
| `message_id` | uuid | FK→messages, UNIQUE, NOT NULL | 아이 메시지 1:1 |
| `child_intent` | varchar(20) | NOT NULL | 13종 |
| `main_point` | text | | |
| `detected_elements` | jsonb | NOT NULL DEFAULT `[]` | `[{type, evidence}]` |
| `utterance_validity` | varchar(20) | NOT NULL, CHECK | `VALID`/`SHORT`/`UNCLEAR`/`OFF_TOPIC`/`PLAYFUL` |
| `analysis_version` | varchar(30) | NOT NULL DEFAULT `mvp_v1` | 프롬프트·규칙 버전 |
| `model_id` | varchar(64) | | 분석에 쓴 LLM 식별자 |
| `dropped_evidence` | jsonb | NOT NULL DEFAULT `[]` | 후처리에서 폐기된 근거 |
| `created_at` | timestamptz | NOT NULL | |

**`analysis_version`과 `model_id`를 둘 다 두는 이유** — 같은 프롬프트를 모델만 바꿔 돌리는 경우가 실제로 생긴다. 문자열 하나로는 구분할 수 없다. 둘 다 **나중에 소급이 안 되므로** 데이터가 쌓이기 전에 넣는다.

**`detected_elements`에는 서버 후처리를 통과한 요소만 담는다.** 근거 문구가 아이 발화 원문에 없으면 그 요소를 버린다. 버린 것은 `dropped_evidence`에 남겨 분석 LLM이 없는 요소를 만들어내는 빈도를 추적한다.

`analysis_versions` 별도 테이블은 두지 않는다. MVP에서는 문자열 하나로 충분하고, 필요해지면 그때 FK로 승격한다.

### 4.4 `mission_results`

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| `id` | uuid | PK | |
| `session_id` | uuid | FK→story_sessions, NOT NULL | |
| `scene_id` | uuid | FK→story_scenes, NOT NULL | |
| `mission_id` | varchar(30) | NOT NULL | UNIQUE(session_id, mission_id) |
| `mission_type` | varchar(30) | NOT NULL, CHECK | `PROBLEM_SOLVING` / `PERSPECTIVE_SHIFT` |
| `result` | jsonb | NOT NULL DEFAULT `{}` | 미션1: `answers` / 미션2: `cards` |
| `created_at` | timestamptz | NOT NULL | |

UNIQUE로 중복 제출을 막고 위반은 409(`MISSION_ALREADY_SUBMITTED`)로 변환한다.

---

## 5. 학습 결과

### 5.1 `post_activity_results`

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| `id` | uuid | PK | |
| `session_id` | uuid | FK→story_sessions, UNIQUE, NOT NULL | 1:1 |
| `card_order_seed` | varchar(64) | NOT NULL | 셔플 고정용 시드 |
| `submitted_order` | text[] | | |
| `is_order_correct` | boolean | | 서버 판정 |
| `attempt_count` | smallint | NOT NULL DEFAULT 0 | |
| `retelling_text` | text | | |
| `completed_at` | timestamptz | | |

**`card_order_seed`가 필요한 이유** — 없으면 앱을 껐다 켜거나 재시도할 때마다 카드 순서가 바뀐다. `submitted_order` 채점을 재현할 수 없고, 아이 입장에서도 방금 보던 화면이 달라진다.

정답 순서는 응답에 담지 않는다. 판정은 서버만 한다.

### 5.2 `reports`

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| `id` | uuid | PK | |
| `session_id` | uuid | FK→story_sessions, UNIQUE, NOT NULL | 세션당 1건 |
| `summary` | text | NOT NULL | |
| `strengths` | jsonb | NOT NULL DEFAULT `[]` | `[{element, comment}]` |
| `next_focus` | jsonb | NOT NULL DEFAULT `[]` | 형식 동일 |
| `created_at` | timestamptz | NOT NULL | |

대표 발화는 저장하지 않고 조회 시 `messages`에서 구성한다. `stt_low_confidence`가 true인 발화는 후보에서 제외한다.

> 대표 발화 선정 방식이 규칙 기반이 아니라 LLM 판단으로 확정되면 이 판단을 뒤집어야 한다. 매 조회마다 결과가 달라지면 리포트가 스냅샷이 아니게 된다. → §11

### 5.3 `wordbook`

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| `id` | uuid | PK | |
| `child_id` | uuid | FK→children, NOT NULL | |
| `word` | varchar(50) | NOT NULL | UNIQUE(child_id, word) |
| `meaning` | text | | 미입력이면 서버가 LLM으로 아이 수준의 뜻 생성 |
| `example_sentence` | text | | 이야기 속 문장 |
| `entry_type` | varchar(20) | NOT NULL, CHECK | `UNKNOWN` / `FAVORITE` |
| `source_scene_id` | uuid | FK→story_scenes | |
| `created_at` | timestamptz | NOT NULL | |

---

## 6. 보상 — 행성 꾸미기

도메인 용어는 **"행성"으로 통일한다.** 화면 문구와 테이블·클래스·API 경로가 같은 말을 쓴다.

### 6.1 `stardust_wallets`

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| `id` | uuid | PK | |
| `child_id` | uuid | FK→children, UNIQUE, NOT NULL | 아이당 1개 |
| `balance` | integer | NOT NULL DEFAULT 0, CHECK ≥ 0 | |
| `total_earned` | integer | NOT NULL DEFAULT 0, CHECK ≥ 0 | 누적 해금 판정용 |
| `created_at` | timestamptz | NOT NULL | |

`total_earned`는 **써도 줄지 않는다.** 누적 해금 조건의 기준이기 때문이다.

### 6.2 `stardust_transactions`

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| `id` | uuid | PK | |
| `wallet_id` | uuid | FK→stardust_wallets, NOT NULL | |
| `amount` | integer | NOT NULL, CHECK ≠ 0 | 지급 +, 사용 − |
| `reason` | varchar(30) | NOT NULL, CHECK | `STORY_COMPLETED` / `SCENE_BONUS` / `ITEM_PURCHASE` / `ADMIN_ADJUST` |
| `session_id` | uuid | FK→story_sessions ON DELETE SET NULL | 지급 근거 |
| `scene_id` | uuid | FK→story_scenes ON DELETE SET NULL | 장면 보너스만 |
| `item_id` | uuid | FK→items | 구매만 |
| `acknowledged` | boolean | NOT NULL DEFAULT false | 떨어지는 연출 확인 여부 |
| `created_at` | timestamptz | NOT NULL | |

**지급 규칙**

| 사유 | 금액 |
|---|---|
| 이야기 완주 | +3 |
| 유도 없이 목표를 통과한 장면 | 장면당 +1, 최대 2 |
| **세션 합계** | **3~5** |

반복 상한: 2회차는 완주 보상 절반(내림), 장면 보너스 없음. 3회차부터 지급 없음. 뽑기·확률 요소는 없다.

**멱등 인덱스를 둘로 나눈 것이 핵심이다.**

```sql
create unique index idx_stardust_tx_session_reason
    on stardust_transactions(session_id, reason)
    where session_id is not null and scene_id is null;      -- 세션 단위(완주)

create unique index idx_stardust_tx_scene_reason
    on stardust_transactions(session_id, scene_id, reason)
    where session_id is not null and scene_id is not null;  -- 장면 단위(보너스)
```

`(session_id, reason)` 하나로 묶으면 **장면 보너스 2건째가 유니크 위반으로 막혀** "세션 합계 3~5" 규칙이 성립하지 않는다. 세션 단위 지급과 장면 단위 지급은 멱등 키가 다르므로 인덱스도 나눠야 한다.

"최대 2회" 상한은 유니크로 표현할 수 없어 서버가 센다.

**`ADMIN_ADJUST`** — 지급 경로가 세션 완료 하나뿐이라 운영 보정 수단이 없으면 시연·장애 대응에 DB를 직접 건드려야 한다.

**별가루 적립 API는 클라이언트에 열지 않는다.** 세션 완료 처리 안에서 서버가 직접 넣는다. 클라이언트가 잔액을 계산해 올리면 위 유니크 제약이 무의미해진다.

### 6.3 `child_story_play_counts`

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| `child_id` | uuid | FK→children | PK(child_id, story_id) |
| `story_id` | uuid | FK→stories | |
| `play_count` | smallint | NOT NULL DEFAULT 0, CHECK ≥ 0 | |
| `updated_at` | timestamptz | NOT NULL | |

**왜 COUNT 대신 카운터 테이블인가** — `COMPLETED` 세션을 세고 나서 지급하면 조회와 지급 사이가 원자적이지 않아 동시 요청에 2회차 보상이 두 번 나간다. `insert … on conflict do update set play_count = play_count + 1 returning play_count` 한 문장으로 올리고 그 반환값으로 지급액을 정한다.

### 6.4 `items`

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| `id` | uuid | PK | |
| `name` | varchar(50) | NOT NULL | |
| `category` | varchar(20) | NOT NULL, CHECK | `TERRAIN_PROP` / `PLANT` / `STRUCTURE` / `ANIMAL` |
| `price` | integer | NOT NULL, CHECK > 0 | 소품 3 / 중형 5 / 대형·동물 10 |
| `unlock_type` | varchar(30) | NOT NULL, CHECK | `ALWAYS` / `STORY_COMPLETE` / `STARDUST_CUMULATIVE` |
| `unlock_story_id` | uuid | FK→stories | `STORY_COMPLETE`일 때 필수 |
| `unlock_stardust_total` | integer | CHECK > 0 | `STARDUST_CUMULATIVE`일 때 필수 |
| `model_url` | text | | 3D 모델(저폴리곤, Kenney CC0 기반) |
| `thumbnail_url` | text | | 상점 목록·실루엣용 |
| `display_order` | smallint | NOT NULL DEFAULT 0 | |
| `status` | varchar(10) | NOT NULL DEFAULT `ACTIVE`, CHECK | `ACTIVE` / `HIDDEN` |
| `created_at` | timestamptz | NOT NULL | |

```sql
check (unlock_type <> 'STORY_COMPLETE'      or unlock_story_id is not null)
check (unlock_type <> 'STARDUST_CUMULATIVE' or unlock_stardust_total is not null)
```

**해금 상태는 저장하지 않고 서버가 매번 계산한다.** `ALWAYS`=항상 / `STORY_COMPLETE`=해당 이야기 `COMPLETED` 세션 존재 / `STARDUST_CUMULATIVE`=`total_earned ≥ 임계값`. 전부 다른 테이블에서 유도되므로 별도 해금 테이블은 동기화 부채만 만든다.

**`status`가 필요한 이유** — `child_items`가 FK로 물고 있어 한 명이라도 산 아이템은 행 삭제가 불가능하다. 내리려면 상태로 감추는 수밖에 없다.

**MVP 16종** — 지형 소품 6(3원, ALWAYS) / 식물 4(5원, ALWAYS) / 구조물 3(울타리·표지판 5원 ALWAYS, 집 10원 누적 15) / 동물 3(10원: 강아지는 방귀 이야기 완주, 토끼·거북이는 누적 30·50).

### 6.5 `child_items`

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| `id` | uuid | PK | |
| `child_id` | uuid | FK→children, NOT NULL | |
| `item_id` | uuid | FK→items, NOT NULL | |
| `acquired_at` | timestamptz | NOT NULL | |

- 같은 아이템 중복 구매를 허용한다(꽃 여러 개 배치) — `(child_id, item_id)` UNIQUE를 두지 않는 이유다.
- **구매한 아이템이 사라지는 경로를 스키마 수준에서 만들지 않는다.** 삭제 API가 없다.
- **보관함 = `child_items` 중 `planet_items`에 없는 것** — 파생이라 따로 저장하지 않는다.

### 6.6 `planets`

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| `id` | uuid | PK | |
| `child_id` | uuid | FK→children, UNIQUE, NOT NULL | 아이당 1개 |
| `name` | varchar(30) | NOT NULL DEFAULT `내 행성` | |
| `tutorial_completed` | boolean | NOT NULL DEFAULT false | 첫 진입 배치 안내 완료 |
| `created_at` | timestamptz | NOT NULL | |

**판 크기·모양 컬럼은 두지 않는다.** 클라이언트 카탈로그가 단일 소스이고, 서버가 격자 크기를 따로 들고 있으면 두 값이 어긋날 때 배치가 조용히 거부된다. 카메라·되돌리기·격자선 표시도 전부 클라이언트 책임이라 서버 상태가 없다.

### 6.7 `planet_items`

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| `id` | uuid | PK | |
| `planet_id` | uuid | FK→planets, NOT NULL | |
| `child_item_id` | uuid | FK→child_items, UNIQUE, NOT NULL | 한 보유 아이템은 한 곳에만 |
| `placed_q` | smallint | NOT NULL | 축좌표 |
| `placed_r` | smallint | NOT NULL | 축좌표 |
| `placed_at` | timestamptz | NOT NULL | |

UNIQUE(`planet_id`, `placed_q`, `placed_r`) — 한 칸에 하나. 위반은 409(`CELL_OCCUPIED`)로 변환한다.

**좌표에 하한 CHECK가 없는 이유** — 프론트와 같은 축좌표(q, r)를 쓰고, 축좌표는 원점 기준이라 **음수가 유효하다.** `>= 0` 검사를 두면 절반의 판이 막힌다.

**범위 검증을 DB에서 못 하는 이유** — PostgreSQL의 테이블 CHECK는 다른 테이블을 참조할 수 없다. 판 크기를 아는 주체가 클라이언트 카탈로그이기도 하다.

**치우기는 삭제가 아니다.** 이 행만 지우고 `child_items`는 남는다 = 보관함 복귀. 배치·이동·회수는 개별 행 조작이고, 되돌리기는 클라이언트가 역조작 API를 호출한다.

---

## 7. 관계도

```
parents ─1:N─ refresh_tokens
parents ─1:N─ children ─1:N─ child_consents
                 │
                 ├─1:1─ stardust_wallets ─1:N─ stardust_transactions ─?─ story_sessions / story_scenes / items
                 ├─1:1─ planets ─1:N─ planet_items ─1:1─ child_items ─N:1─ items ─?─ stories
                 ├─1:N─ child_story_play_counts ─N:1─ stories
                 ├─1:N─ wordbook ─?─ story_scenes
                 └─1:N─ story_sessions ─N:1─ stories
                            │                   │
                            │                   ├─1:N─ topics (story_topics)
                            │                   ├─1:N─ characters ─1:N─ story_scenes
                            │                   └─1:N─ story_scenes ─1:N─ scene_audio
                            ├─1:N─ messages ─1:0..1─ utterance_analyses
                            ├─1:N─ mission_results
                            ├─1:0..1─ post_activity_results
                            └─1:0..1─ reports
```

---

## 8. 트랜잭션·정합성 규칙

| 작업 | 규칙 |
|---|---|
| **아이 생성** | `children` + `planets` + `stardust_wallets`를 한 트랜잭션에서. `ChildCreatedEvent` 동기 리스너 |
| **발화 턴 처리** | `messages`(아이) → `utterance_analyses` → `story_sessions` 갱신 → `messages`(캐릭터) 단일 트랜잭션. 세션 낙관적 락(`version`) |
| **별가루 지급** | 세션 `COMPLETED` 전이와 같은 트랜잭션에서 지갑 증가 + 이력 기록. 멱등은 부분 유니크 인덱스가 보장 |
| **반복 완주** | `child_story_play_counts` upsert의 반환값으로 지급액 결정 — 조회·판정·증가가 한 문장 |
| **아이템 구매** | 해금 검증 → 잔액 검증(CHECK ≥ 0으로 이중 방어) → 차감 + 이력(−price) + `child_items` 생성 단일 트랜잭션 |
| **배치** | UNIQUE(칸)·UNIQUE(보유 아이템) 위반을 409로 변환 — 동시 조작에도 겹침 불가 |

---

## 9. 설계 원칙 — 저장하지 않는 것

파생 가능한 값을 저장하면 원본과 어긋날 때 어느 쪽이 맞는지 알 수 없다. 다음은 의도적으로 저장하지 않는다.

| 값 | 계산 방법 |
|---|---|
| `missing_elements` | `required_elements − accumulated_elements` |
| `current_phase` | `status` + 현재 장면 유형 |
| 아이템 해금 상태 | `unlock_type`별로 세션·지갑에서 유도 |
| 보관함 | `child_items` − `planet_items` |
| 리포트 대표 발화 | `messages` + `utterance_analyses` |
| 2×2 아이템의 비앵커 점유 칸 | 카탈로그 발판 정의로 앱이 계산 |

---

## 10. v3 → v4 변경 요약

| # | 변경 | 이유 |
|---|---|---|
| 1 | `islands`→`planets`, `island_items`→`planet_items` | 도메인 용어를 화면·코드·API에서 "행성"으로 통일 |
| 2 | `grid_x/grid_y` → `placed_q/placed_r`, `>= 0` CHECK 제거 | 프론트와 같은 축좌표. 음수가 유효 |
| 3 | `planets.grid_width/height` 제거 | 판 크기는 클라이언트 카탈로그가 단일 소스 |
| 4 | `characters` 신설 + `story_scenes.character_id`·`scene_stance`·`proper_nouns` | TTS 화자 고정·GUIDED 표현 방식·STT 힌트를 담을 자리가 없었다 |
| 5 | `scene_audio` 신설 | 사전 생성 TTS를 담을 테이블이 아예 없었다 |
| 6 | `messages.stt_confidence`·`stt_low_confidence`·`stt_retry_count` | 대표 발화 필터링의 근거가 없었다 |
| 7 | `utterance_analyses.model_id`·`dropped_evidence` | 프롬프트/모델 추적은 소급이 안 된다 |
| 8 | `story_sessions.safety_*` 3종 | 위험 신호를 감지해도 기록이 안 남았다 |
| 9 | `story_sessions.version` | 턴 처리 중 연타로 상태가 덮어써졌다 |
| 10 | `stardust_transactions.scene_id` + 멱등 인덱스 분리 | 기존 단일 인덱스가 장면 보너스 2건째를 막았다 |
| 11 | `child_story_play_counts` 신설 | 완주 횟수 COUNT는 원자적이지 않다 |
| 12 | `post_activity_results.card_order_seed` | 재진입 시 카드 순서가 바뀌어 채점 재현 불가 |
| 13 | `items.status` | FK 때문에 삭제가 불가능한데 내릴 방법이 없었다 |
| 14 | `messages.character_emotion` CHECK 제거 | 표정 키는 캐릭터마다 다르다 |

---

## 11. 미해결 항목

| 항목 | 현재 상태 | 정해지면 바뀌는 것 |
|---|---|---|
| **2×2 발판 겹침** | 앵커 칸만 UNIQUE로 막힌다. 나머지 칸은 앱이 계산해야 하는데 미구현 | 큰 아이템이 겹쳐 놓인다 |
| **`character_persona` 중복** | `characters.personality` + `scene_stance`와 내용이 겹친다 | 캐릭터 LLM 프롬프트를 새 구조로 옮기면 제거 |
| **STT 신뢰도 기준값** | 미정이라 아무것도 걸러내지 않는다 | 대표 발화 선정, 재입력 안내 |
| **대표 발화 선정 방식** | 규칙 기반 파생으로 가정 | LLM 판단이면 `reports`에 저장해야 한다(§5.2) |
| **토끼·거북이 해금** | 후속 이야기가 없어 누적 30·50으로 임시 설정 | 이야기 추가 시 `STORY_COMPLETE`로 전환 |
| **`difficulty`** | 자유 문자열(시드는 한글 `보통`) | 다른 코드값처럼 대문자 스네이크 + CHECK로 통일 |
| **`scene_audio` 메타데이터** | 시드는 `engine`/`voice`/`duration_ms`/`sentence_timings`가 플레이스홀더 | TTS 엔진 확정 후 manifest 실측값으로 교체 |
| **아이 이름 자리표시자** | 이름을 뺀 공용 음성으로 가정(`child_id`는 null) | 아이별 렌더로 가면 `scene_audio.child_id` 사용 |
| **TTS 엔진** | 미확정 (`characters.tts_voice`가 null) | 사전 생성과 실시간의 목소리 일치 여부 |

---

## 12. 검증 방법

스키마·시드·엔티티 매핑은 실제 PostgreSQL에 적재해 확인한다.

```bash
docker exec goodquestion-postgres psql -U postgres -c "create database gq_check;"
docker exec -i goodquestion-postgres psql -U postgres -d gq_check -v ON_ERROR_STOP=1 < src/main/resources/db/schema.sql
docker exec -i goodquestion-postgres psql -U postgres -d gq_check -v ON_ERROR_STOP=1 < src/main/resources/db/seed.sql
```

엔티티 매핑은 `ddl-auto=validate`가 앱 기동 시 전수 검사한다.

```bash
DB_URL="jdbc:postgresql://localhost:5432/gq_check" DB_USERNAME=postgres DB_PASSWORD=... ./gradlew test
```

정합성 확인 질의 — 아래 둘 다 **0행이어야 정상**이다.

```sql
-- 잔액이 이력 합계와 맞는가
select w.child_id, w.balance, coalesce(sum(t.amount), 0) ledger
from stardust_wallets w left join stardust_transactions t on t.wallet_id = w.id
group by w.id having w.balance <> coalesce(sum(t.amount), 0);

-- 누적 획득이 지급 합계와 맞는가
select w.child_id, w.total_earned, coalesce(sum(t.amount) filter (where t.amount > 0), 0) earned
from stardust_wallets w left join stardust_transactions t on t.wallet_id = w.id
group by w.id having w.total_earned <> coalesce(sum(t.amount) filter (where t.amount > 0), 0);
```

마이그레이션은 v3 스키마를 세운 뒤 `migration-to-v4.sql`을 적용한 결과가 `schema.sql` 신규 생성 결과와 컬럼 단위로 일치하는지 대조한다.

```sql
select table_name||'.'||column_name||' '||data_type||' null='||is_nullable
from information_schema.columns where table_schema='public' order by 1;
```
