# 굿퀘스천 ERD

> **원본은 [`V1__init_schema.sql`](../src/main/resources/db/migration/V1__init_schema.sql)이다.**
> 이 문서는 그 스키마를 관계 중심으로 다시 그린 것이다. 컬럼 하나하나의 제약과
> 그렇게 정한 근거는 [데이터베이스_설계.md](데이터베이스_설계.md)를 본다.
>
> 스키마를 바꾸면(`V2`, `V3` 추가) 이 문서와 [erd-view.html](erd-view.html)을 함께 고친다.
>
> **그림이 코드로 보인다면** 에디터에 mermaid 렌더러가 없는 것이다. GitHub에서는 그냥
> 보이고, 로컬에서는 [erd-view.html](erd-view.html)을 브라우저로 열면 된다.
> IntelliJ는 Mermaid 플러그인, VS Code는 Markdown Preview Mermaid Support 확장이 필요하다.

---

## 0. 읽는 법

그림이 두 종류다. **전체 지도**는 도메인 묶음과 소유 방향만 보는 흐름도이고,
**도메인별 그림**은 컬럼과 카디널리티까지 담은 ER 다이어그램이다. 아래 표기는
도메인별 그림에 적용된다.

**관계 표기**

| 기호 | 뜻 |
| --- | --- |
| `\|\|--o{` | 1 대 N (왼쪽 1건에 오른쪽 0건 이상) |
| `\|\|--o\|` | 1 대 0~1 (있으면 정확히 1건) |
| `\|\|--\|\|` | 1 대 1 (양쪽 모두 반드시 1건) |
| `\|o--o{` | 0~1 대 N (FK가 nullable) |

**컬럼 표기**

`PK` 기본키, `FK` 외래키, `UK` 유니크 제약. 지면 관계상 관계와 식별에 관여하는
컬럼 위주로 싣고 나머지는 생략했다. 배열 타입(`text[]`)은 `text_array`로 적었다.

**도메인 구분** - 패키지 구조와 같다.

| 도메인 | 테이블 수 | 담당 패키지 |
| --- | --- | --- |
| 사용자와 인증 | 4 | `user` |
| 콘텐츠 | 6 | `story.content` |
| 런타임 | 4 | `story.session`, `story.dialogue`, `story.mission` |
| 학습 | 3 | `learning.postactivity`, `learning.report`, `learning.wordbook` |
| 보상 | 7 | `learning.reward` |

---

## 1. 전체 지도

24개 테이블이 어느 도메인에 속하고 무엇을 소유하는지만 본다. 컬럼과 카디널리티는
아래 도메인별 그림에 있다.

```mermaid
flowchart TB
  subgraph U["사용자"]
    direction LR
    parents[parents] --> children[children]
    parents --> refresh_tokens[refresh_tokens]
    children --> child_consents[child_consents]
  end

  subgraph C["콘텐츠"]
    direction LR
    topics[topics] --> story_topics[story_topics]
    stories[stories] --> story_topics
    stories --> characters[characters]
    stories --> story_scenes[story_scenes]
    characters -.-> story_scenes
    story_scenes --> scene_audio[scene_audio]
  end

  subgraph R["런타임"]
    direction LR
    story_sessions[story_sessions] --> messages[messages]
    story_sessions --> mission_results[mission_results]
    messages --> utterance_analyses[utterance_analyses]
  end

  subgraph L["학습"]
    direction LR
    post_activity_results[post_activity_results]
    reports[reports]
    wordbook[wordbook]
  end

  subgraph W["보상"]
    direction LR
    stardust_wallets[stardust_wallets] --> stardust_transactions[stardust_transactions]
    items[items] --> child_items[child_items]
    planets[planets] --> planet_items[planet_items]
    child_items --> planet_items
    child_story_play_counts[child_story_play_counts]
  end

  children -->|child_id| story_sessions
  stories -->|story_id| story_sessions
  story_scenes -->|scene_id| messages
  story_scenes -->|scene_id| mission_results
  story_sessions -->|session_id| post_activity_results
  story_sessions -->|session_id| reports
  children -->|child_id| wordbook
  children -->|child_id| stardust_wallets
  children -->|child_id| planets
  children -->|child_id| child_items
  children -->|child_id| child_story_play_counts
  stories -->|story_id| child_story_play_counts
```

**선을 정리한 기준**

FK 35개를 다 그리면 선이 엉켜 읽을 수 없다. 두 가지로 줄였다.

1. 테이블을 도메인으로 묶었다. **상자 안 관계는 빠짐없이 그렸다.**
2. 도메인을 넘는 참조는 **not null만** 그리고 화살표에 FK 컬럼 이름을 달았다.
   화살표는 소유 방향이며 카디널리티는 표시하지 않는다.

이 기준으로 생략되는 것은 아래 7개이고, 전부 nullable 크로스 도메인 참조다.
없어도 소유 구조가 그대로 읽히고, 도메인별 그림에는 모두 그려져 있다.

| 참조 | FK 컬럼 | 무엇인가 |
| --- | --- | --- |
| `story_sessions` -> `story_scenes` | `current_scene_id` | 세션이 지금 있는 장면 |
| `wordbook` -> `story_scenes` | `source_scene_id` | 단어가 나온 장면 |
| `items` -> `stories` | `unlock_story_id` | 완주로 해금되는 이야기 |
| `stardust_transactions` -> `story_sessions` | `session_id` | 지급 근거 세션 |
| `stardust_transactions` -> `story_scenes` | `scene_id` | 장면 보너스의 장면 |
| `stardust_transactions` -> `items` | `item_id` | 구매 대상 아이템 |
| `scene_audio` -> `children` | `child_id` | 아이별로 렌더한 음성 |

점선으로 그린 `characters -> story_scenes`도 nullable이지만 도메인 안이라 남겼다.

---

## 2. 사용자와 인증

```mermaid
erDiagram
    parents ||--o{ children : "보호자"
    parents ||--o{ refresh_tokens : "발급"
    children ||--o{ child_consents : "동의"

    parents {
        uuid id PK
        varchar email UK "provider=LOCAL"
        varchar password_hash
        varchar provider "LOCAL, KAKAO"
        varchar provider_id UK "provider와 복합"
        varchar name
        timestamptz created_at
    }

    children {
        uuid id PK
        uuid parent_id FK
        varchar name
        smallint birth_year
        timestamptz created_at
    }

    child_consents {
        uuid id PK
        uuid child_id FK
        varchar consent_version
        varchar verification_method "AUTHENTICATED_PARENT 외 2종"
        timestamptz consented_at
        timestamptz withdrawn_at "철회 시각. null이면 유효"
    }

    refresh_tokens {
        uuid id PK
        uuid parent_id FK
        varchar token_hash UK "원문 미저장"
        timestamptz expires_at
        timestamptz revoked_at
        timestamptz created_at
    }
```

`refresh_tokens`는 테이블만 있고 **어떤 서비스도 참조하지 않는다.** 현재는 Access
토큰 단일 전략이다. 근거는 [API_및_DTO_명세.md 8절](API_및_DTO_명세.md)에 있다.

동의는 행을 지우지 않고 `withdrawn_at`을 채워 철회한다. 이력이 남아야 하기 때문이다.

---

## 3. 콘텐츠

정적 콘텐츠다. 런타임 상태를 알지 못하며, 수정은 복사 등록으로 처리한다(데이터-02).

```mermaid
erDiagram
    stories ||--o{ story_topics : "분류"
    topics ||--o{ story_topics : "분류"
    stories ||--o{ characters : "등장"
    stories ||--o{ story_scenes : "장면"
    characters |o--o{ story_scenes : "화자"
    story_scenes ||--o{ scene_audio : "사전 음성"

    stories {
        uuid id PK
        varchar title
        text summary
        varchar child_role "아이가 맡는 역할"
        text intro
        varchar difficulty
        smallint estimated_minutes
        jsonb post_activity_config "후속 활동 카드 정의"
        varchar status "DRAFT, PUBLISHED, ARCHIVED"
        timestamptz created_at
    }

    topics {
        uuid id PK
        varchar name UK
        smallint display_order
    }

    story_topics {
        uuid story_id PK "FK"
        uuid topic_id PK "FK"
    }

    characters {
        uuid id PK
        uuid story_id FK
        varchar character_key UK "story_id와 복합. 표정 파일명의 키"
        varchar name
        text personality "캐릭터 LLM 페르소나"
        text guidance_style
        varchar tts_voice
        text tts_style
        varchar tts_gender "MALE, FEMALE"
        text_array expression_keys
    }

    story_scenes {
        uuid id PK
        uuid story_id FK
        smallint scene_order UK "story_id와 복합"
        varchar scene_type "STORY, DIALOGUE"
        text scene_description
        uuid character_id FK "nullable"
        varchar character_name
        text character_opening "고정 첫 대사"
        text character_closing "고정 마지막 대사"
        text scene_goal
        text_array required_elements
        jsonb element_criteria "요소 인정 기준"
        jsonb remaining_worries "요소별 남은 걱정"
        jsonb mission_config "미션 없는 장면은 null"
        smallint preferred_turns
        smallint max_turns
    }

    scene_audio {
        uuid id PK
        uuid scene_id FK
        varchar slot "NARRATION, OPENING, CLOSING"
        uuid child_id FK "null이면 공용 음성"
        text storage_path
        char text_hash "렌더 원본의 SHA-256"
        varchar engine
        varchar voice
        integer duration_ms
        jsonb sentence_timings "문장별 실측 시작·끝"
    }
```

`story_scenes`가 `character_name`과 `character_id`를 함께 가진다. 이름은 화면 표시용이고
페르소나/보이스/표정은 FK로 찾는다. 장면마다 화자 페르소나가 따로 있으면 같은 캐릭터가
장면별로 다른 목소리로 합성되는 것을 막을 수 없어 `characters`를 분리했다.

`scene_audio.child_id`가 nullable인 이유는, 아이 이름이 들어가는 대사만 아이별로
렌더하고 나머지는 공용 음성을 쓰기 때문이다.

---

## 4. 런타임

세션이 진행되면서 쌓이는 상태다. 여기부터는 아이별 데이터다.

```mermaid
erDiagram
    children ||--o{ story_sessions : "진행"
    stories ||--o{ story_sessions : "재생"
    story_scenes |o--o{ story_sessions : "현재 장면"
    story_sessions ||--o{ messages : "대화"
    story_scenes ||--o{ messages : "발생 장면"
    messages ||--o| utterance_analyses : "분석"
    story_sessions ||--o{ mission_results : "미션"
    story_scenes ||--o{ mission_results : "발생 장면"

    story_sessions {
        uuid id PK
        uuid child_id FK
        uuid story_id FK
        uuid current_scene_id FK "nullable"
        smallint current_child_turn_count
        text_array accumulated_elements "누적 말하기 요소"
        text_array last_detected_elements
        varchar last_response_mode "NORMAL, GUIDED, CLOSING"
        boolean scene_goal_met
        varchar scene_end_reason "GOAL_MET, MAX_TURNS"
        boolean guided_used_in_scene "장면 보너스 판정"
        boolean mission_exposed
        boolean mission_completed
        boolean safety_flagged "위험 신호 감지 이력"
        text_array safety_categories "범주만. 원문 미저장"
        varchar status "IN_PROGRESS, POST_ACTIVITY, COMPLETED, STOPPED"
        bigint version "낙관적 락"
        timestamptz last_activity_at
    }

    messages {
        uuid id PK
        uuid session_id FK
        uuid scene_id FK
        varchar speaker_type "CHILD, CHARACTER, SYSTEM"
        integer turn_order UK "session_id와 복합"
        text text
        text stt_raw_text "아이 발화만. 원본 음성 미저장"
        numeric stt_confidence "0~1"
        boolean stt_low_confidence
        smallint stt_retry_count
        varchar character_emotion "캐릭터 발화만"
    }

    utterance_analyses {
        uuid id PK
        uuid message_id FK "UK. 아이 메시지 1건당 1건"
        varchar child_intent
        text main_point
        jsonb detected_elements
        varchar utterance_validity "VALID, SHORT, UNCLEAR, OFF_TOPIC, PLAYFUL"
        varchar analysis_version
        varchar model_id "프롬프트 동일·모델만 교체를 구분"
        jsonb dropped_evidence "후처리에서 폐기된 근거"
    }

    mission_results {
        uuid id PK
        uuid session_id FK
        uuid scene_id FK
        varchar mission_id UK "session_id와 복합"
        varchar mission_type "PROBLEM_SOLVING, PERSPECTIVE_SHIFT"
        jsonb result
    }
```

`story_sessions.version`은 낙관적 락이다. 턴 처리가 STT/분석/대사 생성으로 수 초 걸려
연타가 들어오면 턴 카운터와 누적 요소가 덮어써지는데, 이걸 409로 바꾼다.

`messages`의 `unique (session_id, turn_order)`가 턴 중복을 막는다.

---

## 5. 학습

세션이 끝난 뒤의 산출물을 다룬다.

```mermaid
erDiagram
    story_sessions ||--o| post_activity_results : "후속 활동"
    story_sessions ||--o| reports : "리포트"
    children ||--o{ wordbook : "저장"
    story_scenes |o--o{ wordbook : "출처 장면"

    story_sessions {
        uuid id PK
        varchar status "POST_ACTIVITY 단계에서 진입"
    }

    story_scenes {
        uuid id PK
        jsonb element_criteria
    }

    children {
        uuid id PK
    }

    post_activity_results {
        uuid id PK
        uuid session_id FK "UK. 세션당 1건"
        varchar card_order_seed "카드 셔플 고정. 채점 재현용"
        text_array submitted_order
        boolean is_order_correct "서버가 판정"
        smallint attempt_count
        text retelling_text
        timestamptz completed_at
    }

    reports {
        uuid id PK
        uuid session_id FK "UK. 세션당 1건"
        text summary
        jsonb strengths "잘 보여준 말하기 요소"
        jsonb next_focus "다음에 연습할 요소"
        timestamptz created_at
    }

    wordbook {
        uuid id PK
        uuid child_id FK
        varchar word UK "child_id와 복합. 중복 저장 방지"
        text meaning "없으면 서버가 LLM으로 생성"
        text example_sentence
        varchar entry_type "UNKNOWN, FAVORITE"
        uuid source_scene_id FK "nullable"
        timestamptz created_at
    }
```

구현 시 걸리는 제약이 세 개 있다.

- `post_activity_results.card_order_seed`가 **not null**이다. 카드 순서를 시드로 고정해야
  재진입/재시도에도 같은 화면이 나오고 채점이 재현된다. `/start`에서 시드를 만들어 저장한다.
- `is_order_correct`는 **서버가 판정한다.** `stories.post_activity_config`의 `correct_order`와
  비교하며 클라이언트 판정을 받지 않는다.
- `wordbook`의 `unique (child_id, word)`가 중복 저장을 막는다. 위반 시 409 `DUPLICATE_WORD`다.

`post_activity_results`와 `reports` 모두 세션당 1건(`session_id` 유니크)이라 재호출은
생성이 아니라 조회 또는 409가 된다.

---

## 6. 보상

별가루를 벌어 아이템을 사고 행성에 배치한다. 테이블이 7개로 가장 많다.

```mermaid
erDiagram
    children ||--|| stardust_wallets : "지갑"
    children ||--|| planets : "행성"
    children ||--o{ child_items : "보유"
    children ||--o{ child_story_play_counts : "완주 횟수"
    stories ||--o{ child_story_play_counts : "완주 횟수"
    stories |o--o{ items : "완주 해금"
    stardust_wallets ||--o{ stardust_transactions : "이력"
    story_sessions |o--o{ stardust_transactions : "지급 근거"
    story_scenes |o--o{ stardust_transactions : "보너스 장면"
    items ||--o{ child_items : "구매"
    items |o--o{ stardust_transactions : "구매 대상"
    planets ||--o{ planet_items : "배치"
    child_items ||--o| planet_items : "놓임"

    children {
        uuid id PK
    }

    stardust_wallets {
        uuid id PK
        uuid child_id FK "UK. 아이당 1개"
        integer balance "사용하면 줄어든다"
        integer total_earned "누적 해금 판정 기준. 줄지 않는다"
    }

    stardust_transactions {
        uuid id PK
        uuid wallet_id FK
        integer amount "지급 +, 사용 -"
        varchar reason "STORY_COMPLETED, SCENE_BONUS, ITEM_PURCHASE, ADMIN_ADJUST"
        uuid session_id FK "지급 근거. 구매는 null"
        uuid scene_id FK "장면 보너스만"
        uuid item_id FK "구매만"
        boolean acknowledged "false면 연출 대상"
    }

    child_story_play_counts {
        uuid child_id PK "FK"
        uuid story_id PK "FK"
        smallint play_count "2회차 절반, 3회차부터 없음"
        timestamptz updated_at
    }

    items {
        uuid id PK
        varchar name
        varchar category "TERRAIN_PROP, PLANT, STRUCTURE, ANIMAL"
        integer price
        varchar unlock_type "ALWAYS, STORY_COMPLETE, STARDUST_CUMULATIVE"
        uuid unlock_story_id FK "STORY_COMPLETE일 때 필수"
        integer unlock_stardust_total "STARDUST_CUMULATIVE일 때 필수"
        varchar status "ACTIVE, HIDDEN"
        smallint display_order
    }

    child_items {
        uuid id PK
        uuid child_id FK
        uuid item_id FK
        timestamptz acquired_at
    }

    planets {
        uuid id PK
        uuid child_id FK "UK. 아이당 1개"
        varchar name "기본값 내 행성"
        boolean tutorial_completed
    }

    planet_items {
        uuid id PK
        uuid planet_id FK
        uuid child_item_id FK "UK. 하나는 한 곳에만"
        smallint placed_q UK "planet_id와 복합"
        smallint placed_r UK "planet_id와 복합"
        timestamptz placed_at
    }
```

구현 시 걸리는 제약이 다섯 개 있다.

- **지갑과 행성은 아이 생성 시 자동으로 만들어진다.** `RewardProvisioningListener`가
  `ChildCreatedEvent`를 받아 같은 트랜잭션에서 각 1건씩 만든다. 보상 API는 조회/변경만 한다.
- **`total_earned`는 줄지 않는다.** 별가루를 써도 `balance`만 줄고 이 값은 유지된다.
  `STARDUST_CUMULATIVE` 해금 판정 기준이기 때문이다.
- **지급 멱등을 부분 유니크 인덱스가 보장한다.** 완주 보상은 `(session_id, reason)`,
  장면 보너스는 `(session_id, scene_id, reason)`로 나눠 건다. 하나로 묶으면 장면 보너스
  2건째가 유니크 위반으로 막힌다.
- **같은 아이템을 여러 개 살 수 있다.** `child_items`에 `(child_id, item_id)` 유니크가 없다.
  대신 `planet_items.child_item_id`가 유니크라 보유분 하나는 한 곳에만 놓인다.
- **치우기는 `planet_items` 행 삭제이고 `child_items`는 남는다.** 이게 보관함 복귀다.
  아이템 행 자체는 지워지지 않으므로 운영 중 내리기는 `items.status=HIDDEN`으로 한다.

`planet_items`의 한계 하나. 발판이 2x2인 아이템은 앵커 칸만 저장하므로 나머지 칸의 겹침을
`unique (planet_id, placed_q, placed_r)`가 막지 못한다. 클라이언트 카탈로그의 발판 정의로
서버가 점유 칸을 계산해 검증해야 한다.

---

## 7. 삭제 전파

`on delete cascade`가 걸린 곳이다. 위쪽을 지우면 아래가 함께 지워진다.

```
parents
+--- children
|    +--- child_consents
|    +--- story_sessions
|    |    +--- messages
|    |    |    +--- utterance_analyses
|    |    +--- mission_results
|    |    +--- post_activity_results
|    |    +--- reports
|    +--- wordbook
|    +--- stardust_wallets
|    |    +--- stardust_transactions
|    +--- child_items
|    |    +--- planet_items
|    +--- child_story_play_counts
|    +--- planets
|    |    +--- planet_items
|    +--- scene_audio (아이별 렌더분만)
+--- refresh_tokens

stories
+--- story_topics
+--- characters
+--- story_scenes
     +--- scene_audio
     +--- messages (restrict. 세션이 있으면 못 지운다)
```

예외 두 가지다.

- `story_sessions.story_id`와 `child_items.item_id`, `stardust_transactions.item_id`는
  cascade가 아니다. 진행 기록이 있는 콘텐츠나 누가 산 아이템은 지울 수 없다.
- `story_scenes.character_id`, `stardust_transactions.session_id`, `.scene_id`는
  `on delete set null`이다. 참조 대상이 사라져도 행은 남는다.
