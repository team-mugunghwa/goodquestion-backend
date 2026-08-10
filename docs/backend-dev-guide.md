# 담당 파트

| 담당자 | 영역 | 담당 도메인 |
| --- | --- | --- |
| 김민태 | AI·대화 파이프라인 | dialog, infra/stt, infra/tts, infra/llm |
| 이영준 | 사용자·인증 | user, home |
| 조현우 | 콘텐츠·세션 | story, session |
| 이서우 | 후속 활동·학습 | learning, infra/llm |

# 굿퀘스천 백엔드 개발 가이드

> **목적**: 팀원이 이 문서 하나로 디렉터리 구조의 설계 의도를 이해하고, 담당 API의 개발에 바로 착수할 수 있도록 한다.
>
> **Base path**: `/api/v1` · **인증**: 모든 요청에 `Authorization: Bearer {Supabase JWT}` 필요 · JWT의 `sub` = `parents.id`

---

## 0. 로컬 개발 환경 셋업

### .env 파일 생성

`.env.example`을 `.env`로 복사한 뒤 실제 값을 채운다. `.env`는 `.gitignore`에 포함되어 있어 커밋되지 않는다.

```bash
cp .env.example .env
```

```
DB_URL=jdbc:postgresql://localhost:5432/goodquestion
DB_USERNAME=
DB_PASSWORD=
# 자체 발급 Access 토큰 서명 키 (HS256, 32바이트 이상) — openssl rand -base64 32
JWT_SECRET=
# Access 토큰 만료(ms). 미설정 시 7일
JWT_EXPIRATION_MS=
```

### DB 스키마·시드 데이터

**SQL을 직접 실행하지 않는다.** Flyway가 앱 기동 시 `resources/db/migration`의 파일을
버전 순서대로 적용하고, 적용 이력을 `flyway_schema_history`에 남긴다. 빈 DB만 만들어 두면 된다.

| 파일 | 역할 |
| --- | --- |
| `resources/db/migration/V1__init_schema.sql` | 테이블·인덱스·제약 조건 생성 (24개 테이블) — **수정 금지** |
| `resources/db/migration/R__seed_content.sql` | MVP 콘텐츠 (`ON CONFLICT DO UPDATE` upsert) — **편집 자유** |
| `resources/db/migration/R__seed_demo_data.sql` | 데모 계정·진행 기록 (`DO NOTHING` insert-if-missing) — **편집 자유** |

**스키마 변경은 새 파일로.** V1은 이미 적용된 파일이라 수정하면 체크섬 위반으로 앱이 안 뜬다.
스키마를 바꾸려면 `V2__*.sql`을 새로 추가한다. 시드 변경은 R__ 파일을 직접 편집한다 —
Flyway가 체크섬 변경을 감지하고 다음 기동에서 자동 재실행한다.

로컬 DB를 갈아엎으려면 스키마만 비운다 — 다음 기동에서 Flyway가 처음부터 다시 만든다.

```bash
docker exec goodquestion-postgres psql -U postgres -d goodquestion -c 'drop schema public cascade; create schema public;'
```

---

## 1. 전체 디렉터리 구조와 설계 원칙

```
com.goodquestion
├── global          횡단 관심사 (인증·에러·설정). 비즈니스 로직 없음
├── common          도메인 간 공유 enum (ThinkingElement, ResponseMode)
├── user            계정·아이·동의. 서비스 진입의 전제 조건을 관리
├── home            메인 화면 조합. 자체 엔티티 없이 session + story를 합쳐 반환
├── story           정적 콘텐츠 (운영자가 등록, 아이는 읽기만). 변경 드묾
├── session         런타임 기록 (매 턴 쓰기 발생). 대화 상태·메시지 보관
├── dialog          대화 파이프라인 핵심. 발화 분석→진행 판단→캐릭터 응답→미션
├── learning        이야기 완료 후 활동. 후속 활동·리포트·단어장
└── infra           외부 API 어댑터 (STT·LLM·TTS). 엔티티 없음
```

**왜 이렇게 나눴는가**

| 원칙 | 설명 |
| --- | --- |
| **변경 주기로 분리** | story(정적 콘텐츠)와 session(런타임 기록)은 읽기/쓰기 패턴이 달라 캐싱·RLS 정책이 다름 |
| **책임 경계로 분리** | dialog는 5단계 파이프라인을 조율하는 핵심인데, 분석(analysis)·판단(progression)·응답(character)·미션(mission)을 각각 독립 컴포넌트로 두어 단위 테스트 가능 |
| **인프라는 어댑터로** | STT·LLM·TTS는 엔티티 없는 외부 연동이라 도메인이 아니라 infra 계층. 벤더 교체 시 구현체만 교체 |
| **Auth는 도메인이 아님** | Supabase Auth가 인증을 담당하므로 서버엔 Auth 엔티티가 없고, JWT 검증 필터만 global/security에 위치 |
| **조합 전용 패키지** | home은 session + story를 조합만 하고 자체 엔티티가 없어 별도 패키지 (어느 한쪽에 넣으면 경계가 흐려짐) |

---

## 2. 디렉터리별 API 일람

### global — 횡단 관심사

> 직접 API 없음. 모든 컨트롤러가 의존하는 공통 인프라.

| 파일 | 역할 | 개발 작업 |
| --- | --- | --- |
| `security/SupabaseJwtFilter` | 매 요청의 JWT 검증 → SecurityContext에 parentId 저장 | Supabase JWT Secret 환경변수 연결, 만료·위조 시 401 처리 확인 |
| `security/CurrentParentId` + `Resolver` | 컨트롤러 파라미터에 `@CurrentParentId UUID parentId` 주입 | 구현 완료 — 사용법만 숙지 |
| `error/ErrorCode` | 비즈니스 예외 코드 13종 정의 | 새 에러 추가 시 여기에 등록 |
| `error/GlobalExceptionHandler` | `BusinessException` → HTTP 응답 변환 | Validation·Unknown 에러 포맷 확인 |
| `config/SecurityConfig` | Spring Security 설정 (STATELESS, JWT 필터 등록) | 운영 시 permitAll 경로 최소화 |
| `config/AsyncConfig` | `@Async` 활성화 (리포트 비동기 생성) | 스레드풀 크기·큐 용량 튜닝 필요 |

---

### user/parent — 보호자 프로필

> **왜 필요한가**: Supabase Auth가 가입/로그인을 처리하지만, 서버가 관리하는 보호자 이름(닉네임)은 별도 저장이 필요하다. `parents.id = auth.users.id`로 동일하게 사용.

| 메서드 | 경로 | 목적 | 주요 작업 | 상태 |
| --- | --- | --- | --- | --- |
| POST | `/parents/me` | 최초 로그인 후 프로필 등록 | 이미 존재하면 그대로 반환(멱등). `Parent.builder().id(parentId)` — @GeneratedValue 아님 | 구현됨 |
| GET | `/parents/me` | 내 프로필 조회 | parentId로 조회, 없으면 404 | 구현됨 |

---

### user/child — 아이 관리

> **왜 필요한가**: 보호자 1명이 여러 아이를 등록할 수 있고, 모든 아이 관련 리소스(세션·단어장·리포트)는 "내 아이인지" 소유권 검증이 필수. `ChildService.getOwnedChild()`가 이 검증의 단일 진입점.

| 메서드 | 경로 | 목적 | 주요 작업 | 상태 |
| --- | --- | --- | --- | --- |
| POST | `/children` | 아이 등록 | name + birthYear 저장, 연도 기준 age 계산(만 나이 아님), hasActiveConsent=false 반환 | 구현됨 |
| GET | `/children` | 내 아이 목록 | parentId로 필터, 각 아이의 동의 상태 포함 | 구현됨 |
| GET | `/children/{childId}` | 아이 상세 | 소유권 검증 후 반환 | 구현됨 |
| PATCH | `/children/{childId}` | 아이 정보 수정 | name, birthYear 부분 수정 (null 필드는 무시) | 구현됨 |
| DELETE | `/children/{childId}` | 아이 삭제 | 아동 개인정보 삭제권 대응. FK cascade로 하위 데이터(동의·세션·메시지·분석·단어장·리포트) 일괄 삭제. 복구 불가 확인 필요 | 구현됨 |

---

### user/consent — 아동 동의

> **왜 필요한가**: 아동 데이터 처리에는 보호자 동의가 법적으로 필요하다. "유효 동의 없는 아이는 새 세션을 시작할 수 없다"는 비즈니스 규칙의 근거. 삭제가 아니라 `withdrawn_at` 기록 방식(이력 보존).

| 메서드 | 경로 | 목적 | 주요 작업 | 상태 |
| --- | --- | --- | --- | --- |
| POST | `/children/{childId}/consents` | 동의 등록 | consentVersion + verificationMethod 저장. 소유권 검증 선행 필요 | TODO: ChildService 연결 |
| PATCH | `/children/{childId}/consents/{consentId}/withdraw` | 동의 철회 | withdrawn_at 기록. 이후 해당 아이의 새 세션 시작 차단 | TODO: consent↔child 소유 검증 |

---

### home — 메인 화면

> **왜 필요한가**: 메인 화면은 "이어하기"(session)와 "추천 이야기"(story)를 한 번에 보여줘야 하는데, 이 두 도메인을 합치는 역할. 자체 엔티티 없이 조합만 수행.

| 메서드 | 경로 | 목적 | 주요 작업 | 상태 |
| --- | --- | --- | --- | --- |
| GET | `/children/{childId}/home` | 메인 화면 데이터 | ① IN_PROGRESS 세션 중 last_activity_at 최신 1건(이어하기) ② PUBLISHED 이야기 최신순 2~3개(추천 — MVP는 추천 로직 미구현) ③ 이미지 URL 포함 | TODO |

---

### story/topic — 토픽

> **왜 필요한가**: 이야기 목록 화면의 "주제별 필터링" UI에 토픽 목록을 제공. 토픽 이름 변경·순서 관리를 한 곳에서 할 수 있도록 별도 테이블로 분리함 (배열 컬럼 방식의 오타·정합성 문제 해소).

| 메서드 | 경로 | 목적 | 주요 작업 | 상태 |
| --- | --- | --- | --- | --- |
| GET | `/topics` | 필터 UI용 전체 토픽 목록 | display_order 오름차순 정렬. 목록이 적어 페이징 불필요 | 구현됨 |

---

### story/story — 이야기

> **왜 필요한가**: 이야기 목록·상세는 서비스의 메인 탐색 화면. PUBLISHED 상태만 노출하며, 토픽 필터링은 story_topics 조인으로 처리.

| 메서드 | 경로 | 목적 | 주요 작업 | 상태 |
| --- | --- | --- | --- | --- |
| GET | `/stories?topicId=&page=&size=` | 이야기 목록 | PUBLISHED 필터 + 토픽 조인(topicId 있을 때). 대표 이미지·제목·예상 시간·주제 반환. 토픽 이름은 StoryTopicRepository.findAllByStoryIds 배치 조회(N+1 방지) | TODO: 토픽 이름 배치 조회 |
| GET | `/stories/{storyId}` | 이야기 상세 | 도입·상황·아이 역할 표시. sceneCount는 SceneRepository.countByStoryId. PUBLISHED 검증 | TODO |

---

### story/scene — 장면 콘텐츠

> **왜 필요한가**: 클라이언트가 도입·전개(STORY) 내레이션을 재생하려면 장면 텍스트와 이미지가 필요. 이야기 시작 전 프리페치해서 오프라인 재생 대비. **element_criteria·remaining_worries·미션 노출 조건 등 서버 내부 설정은 의도적으로 제외** — 아이가 "정답 기준"이나 "미션이 언제 나올지"를 미리 알 수 없게.

| 메서드 | 경로 | 목적 | 주요 작업 | 상태 |
| --- | --- | --- | --- | --- |
| GET | `/stories/{storyId}/scenes` | 장면 콘텐츠 프리페치 | scene_order 오름차순. sceneType(STORY/DIALOGUE)·sceneDescription·imageUrl·characterName·hasMission 반환. PUBLISHED 검증 | TODO: PUBLISHED 검증 |

---

### session/session — 세션 생명주기

> **왜 필요한가**: 이야기 시작부터 종료까지의 전체 상태를 관리. 이어하기 복원, 진행 판단의 누적 상태(accumulated_elements, turns_without_new_element 등) 보관, 미션 상태 추적.
>
> **핵심 규칙**: 유효 동의 없으면 시작 불가(409 CONSENT_REQUIRED). STORY 장면 진행은 scene-completions, DIALOGUE 장면 진행은 /utterances 파이프라인의 CLOSING으로 — 이 구분이 중요.

| 메서드 | 경로 | 목적 | 주요 작업 | 상태 |
| --- | --- | --- | --- | --- |
| POST | `/children/{childId}/sessions` | 이야기 시작 | ① 동의 검증 ② PUBLISHED 이야기 검증 ③ 첫 장면 조회 → 세션 생성 ④ STORY면 이동만, DIALOGUE면 character_opening을 messages에 저장 후 반환 | TODO |
| GET | `/sessions/{sessionId}` | 세션 상태 조회 | 이어하기 복원용. 현재 장면(imageUrl 포함)·턴 수·status 반환 | TODO |
| GET | `/sessions/{sessionId}/messages?sceneId=` | 대화 내역 조회 | turn_order 오름차순. 캐릭터 메시지에 characterEmotion 포함. sceneId 필터 선택 | 구현됨 |
| POST | `/sessions/{sessionId}/scene-completions` | STORY 장면 재생 완료 | ① STORY 장면인지 검증(아니면 409) ② 다음 장면 조회 → moveToScene ③ 다음이 DIALOGUE면 opening 저장·반환 ④ 마지막 장면이면 toPostActivity | TODO |
| PATCH | `/sessions/{sessionId}/stop` | 사용자 중단 | status → STOPPED. 이후 발화 제출 차단 | 구현됨 |

---

### dialog — 대화 파이프라인 (핵심)

> **왜 필요한가**: 이 서비스의 핵심 기능. "아이가 보내기를 누르면" 5단계가 서버에서 한 번에 처리되고, 캐릭터 응답과 진행 상태를 반환한다. 미션도 이 파이프라인 안에서 노출·수행된다.
>
> **설계 원칙**: 분석(analysis)·진행(progression)·미션(mission)은 **LLM을 사용하지 않는 순수 규칙**으로, 캐릭터 응답(character)만 LLM을 사용한다. 이 분리 덕분에 분석·진행·미션은 LLM 없이 단위 테스트할 수 있다.

| 메서드 | 경로 | 목적 | 주요 작업 | 상태 |
| --- | --- | --- | --- | --- |
| POST | `/sessions/{sessionId}/utterances` | 아이 발화 제출 + 미션 수행 | **아래 파이프라인 상세 참고**. 미션 수행 발화는 `missionId` 필드 포함 — 별도 API 없이 동일 파이프라인 재사용 (미션 checkPoints가 target_elements와 대응) | TODO |

**파이프라인 상세 (DialogOrchestrator)**

```
① child 메시지 저장 (text, stt_raw_text)
   └ missionId 있으면 session.completeMission()
② 발화 분석 LLM 호출 (UtteranceAnalysisService)
   └ 입력: sceneContext(=description+conflict), goal, previousCharacterMessage,
     childUtterance, targetElements(=required_elements), elementCriteria
③ 서버 후처리 (AnalysisPostProcessor) — LLM 미사용
   └ evidence 원문 포함 검증, 중복 정리, 스키마 외 요소 제거, 약한 SOLUTION 보정
④ 진행 규칙 엔진 (ProgressionEngine) — LLM 미사용
   └ 누적 상태 갱신 + NORMAL/GUIDED/CLOSING 결정
   └ 미션 필수 장면: 요소 충족 + missionCompleted 모두 필요 (종료 조건)
⑤ 미션 노출 판단 (MissionPolicy) — LLM 미사용
   └ scene.hasMission() && !missionExposed && 노출 조건 충족
   → 응답 missionTrigger 구성 (mission_config에서 클라이언트용 정보만 추출)
⑥-a NORMAL/GUIDED → 캐릭터 LLM 호출 (CharacterResponseService)
   └ GUIDED면 scene.getRemainingWorry(guidanceTarget) 포함
   └ 미션 노출 턴에는 캐릭터가 미션을 자연스럽게 이어주도록 프롬프트 반영
   └ 응답에 CharacterEmotion 포함
⑥-b CLOSING → SceneClosingHandler
   └ LLM 짧은 반응 + 고정 마지막 대사(character_closing) 재생
   └ 다음 장면 이동(STORY면 이동만, DIALOGUE면 opening 저장) 또는 후속 활동 전환
⑦ UtteranceResponse 조립 (missingElements = required - accumulated)
```

**하위 컴포넌트별 위치와 역할**

| 패키지 | 컴포넌트 | 역할 | LLM 사용 | 핵심 참고 문서 |
| --- | --- | --- | --- | --- |
| `dialog/analysis` | `UtteranceAnalysisService` | 분석 LLM 호출 → UtteranceAnalysis 저장 | O | 발화 분석 문서 4·6장 |
| `dialog/analysis` | `AnalysisPostProcessor` | evidence 원문 검증·중복 정리 (5개 검증 항목) | X | 발화 분석 문서 7장 |
| `dialog/progression` | `ProgressionEngine` | NORMAL/GUIDED/CLOSING 결정 (판단 순서 4단계) | X | 발화 분석 문서 10~11장 |
| `dialog/progression` | `GuidanceTargetSelector` | 유도 대상 요소 선택 (반복 방지·우선순위) | X | 발화 분석 문서 12장 |
| `dialog/mission` | `MissionPolicy` | 미션 노출 조건 판단 (재노출 방지) | X | 콘텐츠 문서 미션 노출 원칙 |
| `dialog/character` | `CharacterResponseService` | 캐릭터 대사 + 감정 생성 | O | 발화 분석 문서 13~14장 |
| `dialog/character` | `SceneClosingHandler` | CLOSING 시 마무리 대사 + 장면 이동 처리 | △ (짧은 반응만) | 콘텐츠 문서 공통 처리 규칙 |

---

### dialog/speech — 음성 입출력

> **왜 필요한가**: 아이는 음성으로 대화하고, 캐릭터 대사는 음성으로 재생된다. STT(변환)와 발화 확정(/utterances)을 분리한 이유: 변환 텍스트를 화면에 보여주고 → 아이가 확인 후 보내기를 누르는 두 단계 흐름.

| 메서드 | 경로 | 목적 | 주요 작업 | 상태 |
| --- | --- | --- | --- | --- |
| POST | `/speech/transcriptions` | STT (음성→텍스트) | multipart 오디오 업로드 → infra/stt 위임 → 텍스트 반환. 원본 음성 미저장. 실패·빈 텍스트 422 | TODO: 벤더 연결 |
| POST | `/speech/syntheses` | TTS (텍스트→음성) | messageId 또는 text → infra/tts 위임 → audio/mpeg. 캐릭터 음성 재생·다시 듣기·단어장 음성 듣기 공용 | TODO: 벤더 연결 |

---

### learning/activity — 말하기 후 활동

> **왜 필요한가**: 이야기 완료 후 "이야기 재구성"(카드 순서 배열 + 핵심 단어로 다시 말하기)을 통해 이해도를 확인한다. **정답 판정은 반드시 서버에서** — 프런트 판정 금지 원칙.

| 메서드 | 경로 | 목적 | 주요 작업 | 상태 |
| --- | --- | --- | --- | --- |
| GET | `/sessions/{sessionId}/post-activity` | 활동 시작 정보 | POST_ACTIVITY 상태 검증. stories.post_activity_config의 카드를 **무작위 순서**로 반환. 결과 행 없으면 생성 | TODO |
| POST | `/sessions/{sessionId}/post-activity/card-orders` | 카드 순서 제출 | 서버가 config.cards[].correct_order와 비교해 정답 계산. 정답→retellingKeywords 반환 / 오답→attempt_count 증가·재시도 | TODO |
| POST | `/sessions/{sessionId}/post-activity/retelling` | 이야기 재구성 제출 | retelling_text 저장 → completed_at → session.complete() → **리포트 비동기 생성 트리거** | TODO |

---

### learning/report — 보호자 리포트

> **왜 필요한가**: 보호자가 아이의 말하기 결과를 확인하는 화면. 세션 완료 시 서버가 messages + utterance_analyses를 종합해 LLM으로 자동 생성. 비동기 생성이라 조회 시 아직 없으면 409.

| 메서드 | 경로 | 목적 | 주요 작업 | 상태 |
| --- | --- | --- | --- | --- |
| GET | `/children/{childId}/reports?page=&size=` | 리포트 목록 | 아이 소유권 검증 → 최신순 페이징. storyTitle은 session→story 조인 | TODO |
| GET | `/sessions/{sessionId}/report` | 리포트 상세 | 없으면 409 REPORT_NOT_READY. representativeUtterance는 저장값이 아니라 evidence·messages에서 조회 시 구성 | TODO |

---

### learning/wordbook — 단어장

> **왜 필요한가**: 대화 중 모르는 단어를 저장하고 다시 볼 수 있는 기능. 뜻(meaning)과 예문(example_sentence)은 서버가 LLM으로 아이 수준에 맞게 생성. 음성 듣기는 `/speech/syntheses` 재사용.

| 메서드 | 경로 | 목적 | 주요 작업 | 상태 |
| --- | --- | --- | --- | --- |
| POST | `/children/{childId}/wordbook` | 단어 저장 | 중복 검증(409 DUPLICATE_WORD). WordMeaningLlmClient로 뜻·예문 생성 후 저장 | TODO |
| GET | `/children/{childId}/wordbook?favoriteOnly=&page=&size=` | 단어 목록 | favoriteOnly=true면 좋아하는 단어만 필터 | 구현됨 |
| PATCH | `/children/{childId}/wordbook/{wordId}/favorite` | 좋아하는 단어 토글 | is_favorite 반전. 소유 검증 필요 | TODO: 소유 검증 |
| DELETE | `/children/{childId}/wordbook/{wordId}` | 단어 삭제 | 소유 검증 후 삭제 | TODO: 소유 검증 |

---

### infra — 외부 API 어댑터

> 직접 API 없음. dialog/speech와 learning 서비스가 호출하는 외부 연동 구현체. **인터페이스로 추상화**되어 있어 벤더 교체 시 구현체만 바꾸면 된다.

| 패키지 | 인터페이스 | 구현체 | 역할 | 개발 작업 |
| --- | --- | --- | --- | --- |
| `infra/stt` | `SttClient` | `DefaultSttClient` | 음성→텍스트 변환 | 벤더 선정 후 API 호출 구현. 한국어·아동 발화 인식률 검증 필요 |
| `infra/tts` | `TtsClient` | `DefaultTtsClient` | 텍스트→음성 합성 | 캐릭터별 voice 파라미터 설계 필요 |
| `infra/llm` | `AnalysisLlmClient` | - | 발화 분석 모델 호출 | 프롬프트는 `prompt/AnalysisPromptBuilder`로 조립. 입출력 명세는 발화 분석 문서 4·6장 |
| `infra/llm` | `CharacterLlmClient` | - | 캐릭터 응답 모델 호출 | `prompt/CharacterPromptBuilder`. 캐릭터 성격(character_persona)·남은 걱정(remaining_worries) 반영 |
| `infra/llm` | `ReportLlmClient` | - | 리포트 요약 생성 | `prompt/ReportPromptBuilder`. 대화 다이제스트→summary/strengths/nextFocus |
| `infra/llm` | `WordMeaningLlmClient` | - | 단어 뜻·예문 생성 | `prompt/WordMeaningPromptBuilder`. 아이 수준 어휘 제약 포함 |

---

## 3. 전체 API 한눈에 보기 (31개)

| # | 메서드 | 경로 | 패키지 | 한 줄 설명 | 상태 |
| --- | --- | --- | --- | --- | --- |
| 1 | POST | `/parents/me` | user/parent | 보호자 프로필 등록 (멱등) | 구현됨 |
| 2 | GET | `/parents/me` | user/parent | 내 프로필 조회 | 구현됨 |
| 3 | POST | `/children` | user/child | 아이 등록 | 구현됨 |
| 4 | GET | `/children` | user/child | 내 아이 목록 | 구현됨 |
| 5 | GET | `/children/{childId}` | user/child | 아이 상세 | 구현됨 |
| 6 | PATCH | `/children/{childId}` | user/child | 아이 정보 수정 | 구현됨 |
| 7 | DELETE | `/children/{childId}` | user/child | 아이 삭제 (개인정보 삭제권) | 구현됨 |
| 8 | POST | `/children/{childId}/consents` | user/consent | 동의 등록 | TODO |
| 9 | PATCH | `/children/{childId}/consents/{consentId}/withdraw` | user/consent | 동의 철회 | TODO |
| 10 | GET | `/children/{childId}/home` | home | 메인 화면 (이어하기 + 추천) | TODO |
| 11 | GET | `/topics` | story/topic | 필터용 토픽 목록 | 구현됨 |
| 12 | GET | `/stories` | story/story | 이야기 목록 (토픽 필터·페이징) | TODO |
| 13 | GET | `/stories/{storyId}` | story/story | 이야기 상세 | TODO |
| 14 | GET | `/stories/{storyId}/scenes` | story/scene | 장면 콘텐츠 프리페치 | TODO |
| 15 | POST | `/children/{childId}/sessions` | session | 이야기 시작 (동의 검증) | TODO |
| 16 | GET | `/sessions/{sessionId}` | session | 세션 상태 조회 (이어하기) | TODO |
| 17 | GET | `/sessions/{sessionId}/messages` | session | 대화 내역 | 구현됨 |
| 18 | POST | `/sessions/{sessionId}/scene-completions` | session | STORY 장면 재생 완료 → 다음 장면 | TODO |
| 19 | PATCH | `/sessions/{sessionId}/stop` | session | 사용자 중단 | 구현됨 |
| 20 | POST | `/sessions/{sessionId}/utterances` | dialog | **핵심**: 발화 제출 + 미션 수행 | TODO |
| 21 | POST | `/speech/transcriptions` | dialog/speech | STT (음성→텍스트) | TODO |
| 22 | POST | `/speech/syntheses` | dialog/speech | TTS (텍스트→음성) | TODO |
| 23 | GET | `/sessions/{sessionId}/post-activity` | learning/activity | 후속 활동 시작 (카드 무작위) | TODO |
| 24 | POST | `/sessions/{sessionId}/post-activity/card-orders` | learning/activity | 카드 순서 제출 (서버 채점) | TODO |
| 25 | POST | `/sessions/{sessionId}/post-activity/retelling` | learning/activity | 재구성 발화 제출 → 완료 | TODO |
| 26 | GET | `/children/{childId}/reports` | learning/report | 리포트 목록 (보호자용) | TODO |
| 27 | GET | `/sessions/{sessionId}/report` | learning/report | 리포트 상세 | TODO |
| 28 | POST | `/children/{childId}/wordbook` | learning/wordbook | 단어 저장 (LLM 뜻 생성) | TODO |
| 29 | GET | `/children/{childId}/wordbook` | learning/wordbook | 단어 목록 | 구현됨 |
| 30 | PATCH | `/children/{childId}/wordbook/{wordId}/favorite` | learning/wordbook | 좋아하는 단어 토글 | TODO |
| 31 | DELETE | `/children/{childId}/wordbook/{wordId}` | learning/wordbook | 단어 삭제 | TODO |

> **구현됨**: 컨트롤러+서비스+리포지토리 골격과 핵심 로직이 작성됨
> **TODO**: 컨트롤러·서비스 시그니처는 있으나 본문이 `throw new UnsupportedOperationException("TODO")`

---

## 4. 권장 개발 순서

| 순서 | 대상 | 이유 |
| --- | --- | --- |
| 1 | `ProgressionEngine` + `MissionPolicy` | LLM 없는 순수 로직. 단위 테스트부터 작성하기 좋음 |
| 2 | `AnalysisPostProcessor` | 마찬가지로 순수 로직 (evidence 검증 5개 항목) |
| 3 | `SessionService.start` + `scene-completions` | 세션 생명주기가 돌아야 나머지 테스트 가능 |
| 4 | `DialogOrchestrator` | 파이프라인 조립. 위 3개가 준비되면 LLM mock으로 통합 테스트 가능 |
| 5 | infra/llm 구현체 | 벤더 API 연동. 프롬프트 빌더 완성 |
| 6 | learning (activity → report → wordbook) | 이야기 완료 후 흐름. 독립적이라 병렬 가능 |
| 7 | home | session + story 조합. 두 도메인이 완성된 뒤가 자연스러움 |

---

## 5. 팀 확정 필요 사항

| 항목 | 현재 상태 | 결정 필요 |
| --- | --- | --- |
| `CharacterEmotion` 6종 | NEUTRAL/HAPPY/SAD/WORRIED/SURPRISED/RELIEVED (제안값) | 프런트 표정 리소스와 함께 확정 |
| `EXPRESSION` 요소 | 대화1의 target_elements에 있으나 사고 요소 8종에 미정의 → 시드에서 제외 | 콘텐츠팀 확인: EMOTION 중복인지 새 요소인지 |
| STT/TTS/LLM 벤더 | 인터페이스만 정의됨 | 벤더 선정 후 구현체 작성. 아동 한국어 STT 인식률 검증 필수 |
| `preferred_turns` | 문서에 없어 max−2로 제안 | 콘텐츠팀 확정 |
| 리포트 생성 시점 | 세션 완료 시 @Async 비동기 | 트래픽 적으면 최초 조회 시 동기 생성도 가능 — 팀 판단 |
