# 굿퀘스천 백엔드 개발 가이드

> **목적**: 팀원이 이 문서 하나로 프로젝트를 띄우고, 구조의 설계 의도를 이해하고,
> 자기 담당 API 개발에 착수할 수 있게 한다.
>
> **세부 명세는 이 문서에 담지 않는다.** 엔드포인트와 DTO 필드는
> [API-DTO.md](API-DTO.md), 테이블과 컬럼은 [DB설계.md](DB설계.md)가 원본이다.

---

## 0. 문서 지도

무엇을 찾을 때 어디를 보는지부터 정리한다.

| 알고 싶은 것 | 볼 문서 |
| --- | --- |
| 엔드포인트 경로, 요청/응답 필드, 오류 코드, 구현 상태 | [API-DTO.md](API-DTO.md) |
| 테이블, 컬럼, 제약, 인덱스, 트랜잭션 규칙 | [DB설계.md](DB설계.md) |
| 서버 배포, 환경변수, Railway 설정 | [deploy-railway.md](deploy-railway.md) |
| 로컬 셋업, 패키지 구조, 개발 절차 | 이 문서 |

**충돌하면 무엇이 맞는가**

1. 코드가 최우선이다. DTO record와 엔티티가 실제 계약이다.
2. 스키마는 [`V1__init_schema.sql`](../src/main/resources/db/migration/V1__init_schema.sql)이 원본이고 DB설계.md는 그 근거 설명이다.
3. 문서끼리 다르면 API-DTO.md와 DB설계.md가 이 문서보다 최신이다.

---

## 1. 담당 파트

| 담당자 | 영역 | 담당 패키지 |
| --- | --- | --- |
| 김민태 | AI/대화 파이프라인 | `story.dialogue`, `ai` |
| 이영준 | 사용자/인증 | `user`, `home` |
| 조현우 | 콘텐츠/세션 | `story.content`, `story.session` |
| 이서우 | 후속 활동/학습 | `learning` |

---

## 2. 로컬 개발 환경 셋업

### 요구 사항

- JDK 25 (`build.gradle`의 toolchain이 25로 고정)
- PostgreSQL 17
- Docker (로컬 PostgreSQL 용, 테스트도 기본적으로 Docker로 DB를 띄운다).
  직접 설치한 PostgreSQL을 쓴다면 [Docker 없이 로컬 PostgreSQL로 돌리기](#docker-없이-로컬-postgresql로-돌리기)를 본다

### .env 파일 생성

`.env.example`을 복사해 실제 값을 채운다. `.env`는 `.gitignore`에 있어 커밋되지 않는다.

```bash
cp .env.example .env
```

```
DB_URL=jdbc:postgresql://localhost:5432/goodquestion
DB_USERNAME=postgres
DB_PASSWORD=
# Access 토큰 서명 키 (HS256, 32바이트 이상) - openssl rand -base64 32 로 생성
JWT_SECRET=
JWT_EXPIRATION_MS=604800000
# 카카오 로그인을 쓸 때만 필요. 비우면 소셜 로그인만 실패한다
KAKAO_CLIENT_ID=
KAKAO_CLIENT_SECRET=
# 외부 API 연동 전에는 아무 값이나 넣어 두면 기동된다
STT_API_KEY=
TTS_API_KEY=
LLM_API_KEY=
```

### DB 준비

**SQL을 직접 실행하지 않는다.** 빈 DB만 만들어 두면 Flyway가 앱 기동 시 스키마와 시드를
전부 만든다.

```bash
docker run -d --name goodquestion-postgres \
  -e POSTGRES_PASSWORD=<비밀번호> -e POSTGRES_DB=goodquestion \
  -p 5432:5432 postgres:17
```

### 실행과 확인

```bash
./gradlew bootRun
```

기동 로그에 아래가 순서대로 뜨면 정상이다.

```
Migrating schema "public" to version "1 - init schema"
Migrating schema "public" with repeatable migration "1 seed content"
Migrating schema "public" with repeatable migration "2 seed demo data"
Started GoodquestionBackendApplication
```

```bash
curl http://localhost:8080/actuator/health
```

### 테스트 실행

Docker만 떠 있으면 된다. 환경변수도 `.env`도 필요 없다.

```bash
./gradlew test
```

테스트는 Testcontainers로 `postgres:17` 컨테이너를 직접 띄우고 거기에 붙는다.
Flyway가 그 빈 DB에 스키마와 시드를 전부 만들기 때문에 로컬 개발 DB는 건드리지 않는다.
컨테이너는 임의 포트에 뜨고 테스트가 끝나면 사라진다. IntelliJ에서도 그냥 실행하면 된다.

| 파일 | 역할 |
| --- | --- |
| `support/PostgresContainerConfig.java` | 컨테이너 정의. `@ServiceConnection`이 접속 정보를 주입한다 |
| `support/IntegrationTest.java` | 컨텍스트가 필요한 테스트에 붙이는 애노테이션 |
| `src/test/resources/application-test.yml` | DB 외 설정(`jwt.secret` 등)의 테스트 기본값 |

스프링 컨텍스트가 필요한 테스트에는 `@SpringBootTest` 대신 `@IntegrationTest`를 붙인다.
세 가지 설정을 한 애노테이션으로 묶어 두었으므로 조합이 갈려 컨텍스트 캐시가 쪼개지지 않는다.
컨테이너는 테스트 클래스마다가 아니라 전체 실행에서 한 번만 뜬다.

`ArchitectureTest`는 컨텍스트도 DB도 쓰지 않으므로 Docker 없이도 돈다.

```bash
./gradlew test --tests '*ArchitectureTest'
```

`main()`은 여전히 `.env`를 읽는다. 위 방식은 테스트에만 적용된다.

### Docker 없이 로컬 PostgreSQL로 돌리기

Docker를 쓰지 않고 직접 설치한 PostgreSQL이 있다면 `-PlocalDb`를 붙인다.
접속 정보는 `bootRun`과 같은 `.env`의 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`를 그대로 쓴다.

```bash
./gradlew test -PlocalDb
```

Testcontainers는 뜨지 않고 그 DB에 바로 붙는다. `.env`가 없거나 `DB_URL`이 비면
빌드가 이유를 말하고 멈춘다.

대신 알고 써야 할 것이 있다.

- 그 DB에 Flyway가 실제로 돈다. 테스트가 쓴 데이터는 `@Transactional`이라 롤백되지만
  마이그레이션과 `R__` 시드는 커밋된다. `bootRun` 한 번 한 것과 같은 상태가 된다.
- 그래서 `R__1_seed_content.sql`을 손으로 고쳐 실험 중이라면 그 내용이 덮인다.
  개발 DB와 섞기 싫으면 테스트용 DB를 따로 만들고 `.env`의 `DB_URL`을 그쪽으로 돌린다.
- 앞선 실행이 남긴 데이터가 다음 실행에 보이므로 컨테이너 방식만큼 깨끗하지 않다.
  테스트가 실패했을 때 DB 상태부터 의심해야 한다.

기본값은 Testcontainers다. 특별한 이유가 없으면 `-PlocalDb` 없이 쓰는 쪽을 권한다.
CI도 기본 경로로 돈다.

---

## 3. DB 마이그레이션 절차

Flyway가 `resources/db/migration`의 파일을 기동 시 자동 적용하고 이력을
`flyway_schema_history`에 남긴다.

| 파일 | 역할 | 편집 |
| --- | --- | --- |
| `V1__init_schema.sql` | 테이블/인덱스/제약 (24개 테이블) | **수정 금지** |
| `R__1_seed_content.sql` | 콘텐츠 시드 (`ON CONFLICT DO UPDATE`) | 자유 |
| `R__2_seed_demo_data.sql` | 데모 계정/진행 기록 (`ON CONFLICT DO NOTHING`) | 자유 |

`R__`는 `V__`가 모두 끝난 뒤 알파벳순으로 실행되므로 `1_seed_content -> 2_seed_demo_data` 순서다.
데모 데이터가 콘텐츠를 FK 참조하므로 이 순서가 필수다.

### 스키마 변경 (컬럼 추가, 테이블 신설)

이미 적용된 `V*` 파일은 수정 금지다. 체크섬이 어긋나면 앱이 뜨지 않는다.

1. 다음 번호로 `V{n}__{설명}.sql`을 새로 만든다 (현재 V1까지 있으므로 다음은 V2)
2. 엔티티 필드와 매핑을 함께 수정한다. `ddl-auto=validate`가 불일치를 잡는다
3. 앱을 실행해 Flyway 적용과 엔티티 검증을 확인한다
4. 시드에 새 컬럼 값이 필요하면 `R__1_seed_content.sql`도 수정한다
5. 커밋

컬럼 상세와 제약 근거는 [DB설계.md](DB설계.md)를 본다.

### 데이터 변경 (콘텐츠 편집, 데모 추가)

`R__` 파일을 직접 편집하면 다음 기동에서 자동 재실행된다.

- 이야기/장면/캐릭터/아이템 -> `R__1_seed_content.sql` (upsert라 편집이 반영된다)
- 데모 계정/진행 기록 -> `R__2_seed_demo_data.sql` (do-nothing이라 사용자 상태를 덮지 않는다)

행 삭제는 자동 반영되지 않는다. 파일에서 지운 뒤 수동 `DELETE`가 필요하다.

### 로컬 DB 초기화

```bash
docker exec goodquestion-postgres psql -U postgres -d goodquestion -c 'drop schema public cascade; create schema public;'
```

다음 기동에서 Flyway가 처음부터 다시 만든다. 마이그레이션 파일을 고쳐 실험할 때 쓴다.

---

## 4. 패키지 구조와 설계 원칙

```
com.mugunghwa.goodquestion
+--- global      공유 커널. 인증/오류/설정/공용 enum. 도메인을 모른다
+--- user        보호자/아이/동의. 의존 사슬의 최하단
+--- story       콘텐츠와 런타임. content/session/dialogue/mission
+--- learning    이야기 완료 후. postactivity/report/wordbook/reward
+--- ai          LLM/STT/TTS 어댑터. 무상태, 엔티티 없음
+--- home        조립 계층. 자체 엔티티 없이 여러 도메인을 합쳐 반환
```

**의존 방향**

```
home  ->  learning  ->  story  ->  user
              |           |
              +-----------+---->  ai (단방향)

global <- 모두가 의존, global은 아무것도 모름
```

**왜 이렇게 나눴는가**

| 원칙 | 설명 |
| --- | --- |
| ai는 무상태 어댑터 | 도메인 엔티티를 모른다. `global.vocab` enum과 자체 DTO로만 소통해서 벤더 교체 시 구현체만 바꾼다 |
| 판정은 서버가 한다 | 진행 판단/종료/정답 계산은 `story.dialogue.engine`의 순수 규칙이고 LLM을 부르지 않는다. LLM 없이 단위 테스트할 수 있다 |
| 콘텐츠와 런타임 분리 | `story.content`는 읽기 전용 정적 콘텐츠라 런타임 상태(`session`/`dialogue`)를 알지 못한다 |
| 역방향은 이벤트로 뒤집는다 | `user.child`가 아이를 만들면 `ChildCreatedEvent`를 발행하고 `learning.reward.RewardProvisioningListener`가 지갑과 행성을 만든다. `user -> learning` 역참조가 생기지 않는다 |
| home은 조립 전용 | 여러 도메인 조회를 합치기만 하고 자체 엔티티가 없다. 어느 한쪽에 넣으면 경계가 흐려진다 |

### 구조 규칙은 빌드가 강제한다

위 규칙은 문서가 아니라 [`ArchitectureTest`](../src/test/java/com/mugunghwa/goodquestion/ArchitectureTest.java)에
ArchUnit 규칙으로 박혀 있다. 경계를 넘는 import를 추가하면 테스트가 깨진다.

```bash
./gradlew test --tests '*ArchitectureTest'
```

새 패키지를 만들거나 의존 방향을 바꿔야 한다면 이 테스트를 먼저 고치고 이유를
`because(...)`에 남긴다.

---

## 5. 도메인별 진입점

세부 엔드포인트와 필드는 [API-DTO.md](API-DTO.md)에 있다. 여기서는 어느 클래스부터
읽으면 되는지만 짚는다.

| 패키지 | 진입점 | 맡는 것 | API-DTO 참고 절 |
| --- | --- | --- | --- |
| `global.security` | `JwtAuthFilter`, `JwtProvider`, `@CurrentParentId` | 토큰 검증과 보호자 주입 | 1.1 인증 |
| `global.error` | `ErrorCode`, `GlobalExceptionHandler` | 오류 코드 24종과 응답 변환 | 1.2 오류 응답 |
| `user.auth` | `AuthController`, `AuthService` | 가입/로그인/카카오 | 2.1, 3.2 |
| `user.child`, `user.consent` | `ChildController`, `ConsentController` | 아이 관리와 동의 게이트 | 2.3, 2.4, 3.3 |
| `home` | `HomeController`, `HomeService` | 이어하기 + 추천 + 행성 위젯 조립 | 2.5, 3.4 |
| `story.content` | `StoryController`, `SceneController`, `TopicController` | 이야기/장면/주제 조회 | 2.6, 3.5 |
| `story.session` | `SessionController`, `SessionService` | 세션 생명주기와 장면 전환 | 2.7, 3.6 |
| `story.dialogue` | `TurnController`, `TurnOrchestrator` | 발화 턴 파이프라인 | 2.8, 3.7 |
| `story.dialogue.engine` | `ProgressionEngine`, `AnalysisPostProcessor`, `GuidanceTargetSelector` | 진행 판단 순수 규칙 | 3.7 |
| `story.mission` | `MissionController`, `MissionPolicy` | 미션 노출과 결과 | 2.9, 3.8 |
| `learning.postactivity` | `PostActivityController` | 카드 순서와 재구성 | 2.10, 3.9 |
| `learning.report` | `ReportController` | 보호자 리포트 | 2.11, 3.10 |
| `learning.wordbook` | `WordbookController` | 단어장 | 2.12, 3.11 |
| `learning.reward` | `ShopController`, `StardustController`, `PlanetController`, `PlacementController` | 상점/별가루/행성 배치 | 2.13 ~ 2.15, 3.12 |
| `ai.speech` | `SpeechController`, `SpeechService` | STT/TTS 진입점 | 2.16, 3.13 |
| `ai.stt`, `ai.tts`, `ai.analysis`, `ai.character`, `ai.report`, `ai.word` | `*Client` 인터페이스와 `*PromptBuilder` | 외부 모델 호출과 프롬프트 조립 | 3.13 |

### 발화 턴 파이프라인

이 서비스의 핵심이고 미구현 범위가 가장 넓다. `TurnOrchestrator`가 조율한다.

```
POST /api/sessions/{sessionId}/utterances
 1 세션 검증과 소유권 확인 (SessionService)
 2 아이 메시지 저장
 3 발화 분석 LLM 호출 (UtteranceAnalysisService, 트랜잭션 밖)
 4 후처리 (AnalysisPostProcessor)          LLM 미사용
 5 분석 저장, 세션 누적 상태 갱신
 6 진행 모드 결정 (ProgressionEngine)       LLM 미사용
 7 미션 노출 판단 (MissionPolicy)           LLM 미사용
 8a NORMAL/GUIDED -> 캐릭터 대사 생성 (CharacterResponseService)
 8b CLOSING       -> 마무리 대사와 장면 이동 (SceneClosingHandler)
 9 캐릭터 메시지 저장 후 UtteranceResponse 조립
```

4/6/7 단계는 LLM을 부르지 않는 순수 규칙이다. 이 분리 덕에 LLM 없이 단위 테스트할 수 있고,
`ArchitectureTest`가 `story.dialogue.engine`의 `ai` 참조를 금지해 강제한다.

---

## 6. 새 API를 추가할 때

1. [API-DTO.md](API-DTO.md)에 요청/응답 계약이 있는지 먼저 본다. 있으면 그대로 따른다
2. DTO는 `record`로 만들고 해당 도메인의 `dto` 하위 패키지에 둔다
3. 컨트롤러는 보호자 식별자를 파라미터로 받지 않는다. `@CurrentParentId UUID parentId`로 주입받는다
4. 아이나 세션 리소스는 진입 시 소유권을 검증한다. 남의 것이면 403
5. 새 오류 상황이면 `ErrorCode`에 등록하고 `BusinessException`으로 던진다
6. 미구현 상태로 두려면 `UnsupportedOperationException`을 던진다. `GlobalExceptionHandler`가 501로 바꿔 프론트가 구현 여부를 오해하지 않는다
7. 계약이 바뀌면 [API-DTO.md](API-DTO.md)를 함께 고친다

**응답 설계 원칙 6가지**(판정은 서버가, 파생값은 저장하지 않고 계산, 서버 내부 설정은
내리지 않음, 분기는 null로, 정답은 내리지 않음, 발화 원문은 안전 응답에 담지 않음)는
[API-DTO.md 5절](API-DTO.md)에 근거와 함께 정리돼 있다.

---

## 7. 구현 현황

엔드포인트 56개 중 25개가 동작하고, 2개는 일부 경로만 동작하며, 29개가 501 스텁이다.
스텁도 **DTO 계약은 확정**돼 있어 프론트는 미리 붙여 둘 수 있다.
영역별 집계와 개별 상태는 [API-DTO.md 7절](API-DTO.md)에 있다.

대략적인 그림은 이렇다.

- **동작**: 인증(가입/로그인/카카오), 아이와 동의 전체, 콘텐츠 조회, 홈, 세션 시작과 장면 전환, 단어 목록과 즐겨찾기
- **일부만 동작**: 소셜 로그인은 카카오만(다른 공급자는 501), 내 정보 수정은 이름만(비밀번호 변경은 501)
- **501 스텁**: 대화 턴 파이프라인 전체, 미션, 후속 활동, 리포트, 보상(상점/별가루/행성) 전체, 토큰 재발급과 로그아웃, 이어하기, 단어 저장과 삭제
- **경계에서 막힘**: `/api/stt`, `/api/tts`는 컨트롤러와 서비스까지 구현됐고 벤더 클라이언트만 비어 있다

### 권장 개발 순서

| 순서 | 대상 | 이유 |
| --- | --- | --- |
| 1 | `ProgressionEngine`, `MissionPolicy`, `AnalysisPostProcessor` | LLM 없는 순수 로직이라 단위 테스트부터 쓰기 좋다 |
| 2 | `TurnOrchestrator` | 위가 준비되면 LLM을 mock으로 두고 통합 테스트가 가능하다 |
| 3 | `ai` 클라이언트 구현체 | 벤더 연동과 프롬프트 빌더 완성 |
| 4 | `learning.postactivity` -> `report` | 세션 완료 이후 흐름 |
| 5 | `learning.reward` | 컨트롤러만 있고 서비스가 통째로 없다. 엔티티와 스키마는 준비돼 있다 |

---

## 8. 배포

Railway에 Docker 이미지로 배포한다. `develop` 브랜치에 push하면 자동 재배포된다.
프로젝트 생성, 환경변수 매핑, 트러블슈팅은 [deploy-railway.md](deploy-railway.md)에 있다.

---

## 9. 팀 확정 필요 사항

각 문서의 미해결 항목을 합친 것이다. 근거는 [API-DTO.md 6절](API-DTO.md),
[DB설계.md 11절](DB설계.md)에 있다.

| 항목 | 현재 | 결정 필요 |
| --- | --- | --- |
| 리프레시 토큰 | 테이블과 엔티티는 있고 로직이 없다. Access 단일 전략으로 완결 동작 | 도입 시점. 응답 스키마는 그대로라 클라이언트 변경은 없다 |
| 멀티파트 1MB 한도 | `application.yml`에 설정이 없어 Boot 기본 1MB | 30초 WAV가 약 960KB라 아슬아슬하다. 10MB로 올릴지 |
| STT 신뢰도 기준값 | 미정이라 `sttLowConfidence`가 항상 false | 기준값 확정 |
| `SafetyResponse` 감지 | 계약 자리만 있고 항상 null | AI 파이프라인 연동 시 구현 |
| `CharacterEmotion` 6종 | 응답 enum은 고정인데 DB는 CHECK를 풀었다 | 캐릭터별 표정 키로 옮길지 |
| `DELETE /api/words/{wordId}` | 경로에 `childId`가 없어 소유권 검증이 애매하다 | 경로를 `/api/children/{childId}/words/{wordId}`로 맞출지 |
| STT/TTS/LLM 벤더 | 인터페이스만 있다 | 벤더 선정. 아동 한국어 STT 인식률 검증이 필수다 |
