# API 및 DTO 명세

> 원본은 `src/main/java/.../dto/` 의 record들이다. **불일치가 생기면 코드가 맞다.**
> 스키마 대응은 [데이터베이스_설계.md](데이터베이스_설계.md)를 참고한다.

---

## 1. 공통 규약

### 1.1 인증

- `/api/auth/**` 와 `/actuator/health` 만 인증 없이 접근한다. 나머지는 전부 Bearer 토큰이 필요하다.
- **보호자 식별자는 요청에 담지 않는다.** `@CurrentParentId`가 JWT에서 꺼내 주입한다. 아래 표의 요청 필드에 `parentId`가 없는 이유다.
- 아이·세션 리소스는 컨트롤러 진입 시 **소유권을 검증**한다. 남의 아이면 403.

### 1.2 오류 응답

모든 오류는 형태가 같다.

```json
{ "code": "CONSENT_REQUIRED", "message": "유효한 아동 동의가 필요합니다." }
```

| 상황 | 상태 | `code` |
|---|---|---|
| 검증 실패(`@Valid`) | 400 | `INVALID_REQUEST` — message는 `필드명: 사유`. `INVALID_IDEMPOTENCY_KEY`(키 64자 초과) |
| 토큰 없음·위조·**만료** | 401 | `UNAUTHORIZED` |
| 남의 리소스 | 403 | `FORBIDDEN` |
| 없는 리소스 | 404 | `NOT_FOUND` |
| 상태 충돌 | 409 | 아래 목록 |
| 값이 규칙에 안 맞음 | 422 | `STT_EMPTY_TEXT`, `GRID_OUT_OF_RANGE` |
| 미구현 스텁 | **501** | `NOT_IMPLEMENTED` |
| 그 외 | 500 | `INTERNAL_ERROR` |

409 코드: `CONSENT_REQUIRED` `SESSION_NOT_IN_PROGRESS` `SCENE_NOT_STORY` `SCENE_NOT_DIALOGUE` `REPORT_NOT_READY` `DUPLICATE_WORD` `DUPLICATE_EMAIL` `CELL_OCCUPIED` `ITEM_ALREADY_PLACED` `ITEM_LOCKED` `STARDUST_INSUFFICIENT` `MAX_TURNS_EXCEEDED` `MISSION_NOT_EXPOSED` `MISSION_ALREADY_SUBMITTED` `RETELLING_BEFORE_ORDER` `CONCURRENT_TURN` `REQUEST_IN_PROGRESS`

**멱등키(2026-08 확정)** — 발화 제출과 아이템 구매는 `Idempotency-Key` 헤더(선택, UUID 권장, 64자 이하)를 받는다. 클라이언트가 작업마다 새로 만들고 **재시도 사이에만 유지**한다. 같은 키의 재전송은 완료된 요청이면 저장된 응답을 그대로 재생하고, 처리 중이면 409 `REQUEST_IN_PROGRESS`를 돌려준다. 키가 없으면 기존 동작 그대로다. 기록은 24시간 보관 후 청소된다.

**501을 쓰는 이유** — 컨트롤러 골격만 있고 로직이 없는 엔드포인트가 200에 빈 본문을 돌려주면 프론트가 구현된 것으로 오해한다. 명시적으로 알린다.

**401과 403은 반드시 갈라 처리한다.** 리프레시 토큰이 없어 만료 복구 경로가 재로그인 하나뿐이므로, 클라이언트는 **401을 받으면 로그인 화면으로** 보내야 하고 403은 그냥 오류로 표시하면 된다. 스프링 시큐리티 기본값은 둘 다 403 + 빈 본문이라 `RestAuthenticationEntryPoint`·`RestAccessDeniedHandler`로 갈라 두었고, 두 응답 모두 위의 `{code, message}` 형태를 지킨다.

### 1.3 구현 상태 표기

| 표기 | 뜻 |
|---|---|
| ✅ | 호출하면 실제 값이 온다 |
| ⚠️ | 일부 경로만 동작 (설명 참고) |
| ⛔ | 호출하면 **501** — DTO 계약만 확정된 상태 |

---

## 2. 엔드포인트 총괄

### 2.1 인증 — `/api/auth` (인증 불필요)

| 메서드 | 경로 | 설명 | 요청 | 응답 | 상태 |
|---|---|---|---|---|---|
| POST | `/signup` | 이메일과 비밀번호로 계정을 만들고 토큰까지 바로 발급한다 | `SignUpRequest` | 201 `AuthResponse` | ✅ |
| POST | `/login` | 이메일과 비밀번호를 확인하고 토큰을 발급한다 | `LoginRequest` | 200 `AuthResponse` | ✅ |
| POST | `/social/{provider}` | 인가 코드를 서버가 제공자 토큰으로 교환해 가입 또는 로그인 처리한다 | `SocialLoginRequest` | 200 `SocialAuthResponse` | ⚠️ `kakao`, `google`만. 그 외 501 |
| POST | `/refresh` | 리프레시 토큰으로 액세스 토큰을 다시 받는다 | `TokenRefreshRequest` | 200 `TokenResponse` | ✅ |
| POST | `/logout` | 리프레시 토큰을 무효화한다 | `LogoutRequest` | 204 (본문 없음) | ✅ |

### 2.2 보호자 — `/api/parents`

| 메서드 | 경로 | 설명 | 요청 | 응답 | 상태 |
|---|---|---|---|---|---|
| GET | `/me` | 로그인한 보호자의 프로필을 조회한다 | — | `ParentResponse` | ✅ |
| PATCH | `/me` | 보호자 이름과 비밀번호를 수정한다 | `ParentUpdateRequest` | `ParentResponse` | ⚠️ 이름만. `newPassword`를 보내면 501 |

### 2.3 아이 — `/api/children`

| 메서드 | 경로 | 설명 | 요청 | 응답 | 상태 |
|---|---|---|---|---|---|
| POST | `` | 아이를 등록한다. 행성과 지갑이 함께 생긴다 | `ChildCreateRequest` | 201 `ChildResponse` | ✅ |
| GET | `` | 내가 등록한 아이 목록을 조회한다 | — | `List<ChildResponse>` | ✅ |
| GET | `/{childId}` | 아이 한 명을 조회한다 | — | `ChildResponse` | ✅ |
| PATCH | `/{childId}` | 아이 이름과 출생연도를 수정한다 | `ChildUpdateRequest` | `ChildResponse` | ✅ |
| DELETE | `/{childId}` | 아이를 삭제한다 | — | 204 | ✅ |

아이를 만들면 행성·별가루 지갑이 같은 트랜잭션에서 함께 생긴다(응답에는 담지 않는다).

### 2.4 아동 동의 — `/api/children/{childId}/consents`

| 메서드 | 경로 | 설명 | 요청 | 응답 | 상태 |
|---|---|---|---|---|---|
| POST | `` | 법정대리인 동의를 등록한다. 세션 시작의 전제 조건이다 | `ConsentCreateRequest` | 201 `ConsentResponse` | ✅ |
| GET | `` | 현재 동의 상태를 조회한다 | — | `ConsentStatusResponse` | ✅ |
| POST | `/withdraw` | 유효한 동의를 철회한다 | — | `ConsentResponse` | ✅ |

### 2.5 홈

| 메서드 | 경로 | 설명 | 요청 | 응답 | 상태 |
|---|---|---|---|---|---|
| GET | `/api/children/{childId}/home` | 이어하기 카드와 추천 이야기, 행성 위젯을 한 화면 분량으로 조립해 내려준다 | — | `HomeResponse` | ✅ |

### 2.6 콘텐츠

| 메서드 | 경로 | 설명 | 요청 | 응답 | 상태 |
|---|---|---|---|---|---|
| GET | `/api/stories` | 이야기 목록을 조회한다 | `?topic=` (선택) | `StoryListResponse` | ✅ |
| GET | `/api/stories/{storyId}` | 이야기 한 편의 상세 정보를 조회한다 | — | `StoryDetailResponse` | ✅ |
| GET | `/api/stories/{storyId}/scenes` | 이야기의 장면 전체를 순서대로 조회한다 | — | `List<SceneContentResponse>` | ✅ |
| GET | `/api/topics` | 이야기를 거르는 데 쓰는 주제 목록을 조회한다 | — | `List<TopicResponse>` | ✅ |

### 2.7 세션

| 메서드 | 경로 | 설명 | 요청 | 응답 | 상태 |
|---|---|---|---|---|---|
| POST | `/api/children/{childId}/sessions` | 이야기를 골라 세션을 시작한다. 첫 장면을 함께 돌려준다 | `SessionStartRequest` | 201 `SessionStartResponse` | ✅ |
| GET | `/api/sessions/{sessionId}` | 세션 상태와 진행 상황을 조회한다 | — | `SessionResponse` | ✅ |
| GET | `/api/sessions/{sessionId}/resume` | 이어하기. 장면과 대화 내역, 마지막 대사를 한 번에 돌려준다 | — | `SessionResumeResponse` | ✅ |
| GET | `/api/sessions/{sessionId}/messages` | 대화 내역을 조회한다 | `?sceneId=` (선택) | `List<MessageResponse>` | ✅ |
| POST | `/api/sessions/{sessionId}/scenes/current/story-complete` | STORY 장면 재생이 끝났음을 알리고 다음 장면으로 넘긴다 | — | `SceneAdvanceResponse` | ✅ |
| POST | `/api/sessions/{sessionId}/stop` | 진행 중인 세션을 중단한다 | — | 200 (본문 없음) | ✅ |
| POST | `/api/sessions/{sessionId}/scenes/current/opening` | 현재 장면의 고정 첫 대사를 재생한다 | — | `SceneOpeningResponse` | ✅ |
| GET | `/api/sessions/{sessionId}/scenes/current` | 현재 장면을 다시 조회한다. 새로고침 복구용 | — | `CurrentSceneResponse` | ✅ |

첫 대사 재생은 멱등이다. 세션 시작과 장면 전환이 이미 저장해 뒀으면 그 메시지를 그대로 돌려주고 `alreadyOpened=true`로 알린다.

### 2.8 대화(턴) — `/api/sessions/{sessionId}`

| 메서드 | 경로 | 설명 | 요청 | 응답 | 상태 |
|---|---|---|---|---|---|
| POST | `/utterances` | 아이 발화를 제출한다. 분석과 진행 판단, 캐릭터 응답까지 한 번에 처리하는 핵심 API. `Idempotency-Key` 헤더(선택)로 재전송 중복 방지 | `UtteranceRequest` | `UtteranceResponse` | ✅ |
| GET | `/turn-state` | 현재 장면의 턴 누적 상태를 조회한다 | — | `TurnStateResponse` | ✅ |

**턴 처리 중 발생하는 충돌**

| 상황 | 코드 | 의미 |
|---|---|---|
| 현재 장면이 대화 장면이 아니다 | 409 `SCENE_NOT_DIALOGUE` | STORY 장면에서는 발화를 받지 않는다 |
| 이미 최대 턴에 닿은 장면이다 | 409 `MAX_TURNS_EXCEEDED` | 정상 흐름에서는 나오지 않는다. 앞선 응답을 놓친 클라이언트의 재전송 |
| 같은 세션에 턴이 겹쳤다 | 409 `CONCURRENT_TURN` | 아이가 연타했다. 앞선 요청이 끝난 뒤 다시 보낸다 |

한 턴 처리는 수 초가 걸리므로 **전송 중에는 발화 버튼을 잠가야 한다.** 겹친 요청은 409로 돌아온다.

`CONCURRENT_TURN`은 **다시 보내도 되는 실패**다. 겹친 요청 중 진 쪽이 아이 발화만 남기고 끝날 수 있으므로, 재전송 전에 `GET /turn-state`나 이어하기로 현재 상태를 다시 읽는 것이 안전하다.

**타임아웃 후 재전송은 `Idempotency-Key`가 정석이다.** 같은 키로 다시 보내면 서버가 처리를 이미 마쳤어도 저장된 응답이 재생되어 중복 턴이 되지 않는다. 키를 쓰지 않는 클라이언트만 turn-state 판정이 필요하다.

### 2.9 미션 — `/api/sessions/{sessionId}/missions`

| 메서드 | 경로 | 설명 | 요청 | 응답 | 상태 |
|---|---|---|---|---|---|
| GET | `/current` | 지금 노출된 미션을 조회한다. 노출 전이면 null이고 404가 아니다 | — | `CurrentMissionResponse` | ✅ |
| POST | `/{missionId}/result` | 미션 수행 결과를 제출한다 | `MissionResultRequest` | 201 `MissionResultResponse` | ⛔ |

### 2.10 말하기 후 활동 — `/api/sessions/{sessionId}/post-activity`

| 메서드 | 경로 | 설명 | 요청 | 응답 | 상태 |
|---|---|---|---|---|---|
| POST | `/start` | 카드 순서 맞추기를 시작한다. 카드를 무작위 순서로 받는다 | — | `PostActivityStartResponse` | ✅ |
| GET | `` | 후속 활동 진행 상태를 조회한다. 새로고침 복구용 | — | `PostActivityStatusResponse` | ✅ |
| POST | `/order` | 카드 순서를 제출한다. 정답 판정은 서버가 한다 | `CardSubmitRequest` | `CardSubmitResponse` | ✅ |
| POST | `/retelling` | 다시 이야기하기를 제출한다. 세션 완료와 별가루 지급이 함께 일어난다 | `RetellingRequest` | `RetellingResponse` | ✅ |

### 2.11 리포트

| 메서드 | 경로 | 설명 | 요청 | 응답 | 상태 |
|---|---|---|---|---|---|
| GET | `/api/children/{childId}/reports` | 아이의 리포트 목록을 조회한다 | — | `List<ReportListResponse>` | ✅ |
| GET | `/api/sessions/{sessionId}/report` | 세션 한 건의 리포트를 조회한다. 아직 생성 전이면 409 | — | `ReportDetailResponse` | ✅ |
| POST | `/api/sessions/{sessionId}/report` | 세션의 대화와 분석을 집계해 리포트를 생성한다 | — | 201 `ReportDetailResponse` | ⛔ |

조회 2건은 저장된 리포트를 읽는다. 생성은 LLM이 필요해 아직 열리지 않았고, 지금은 시드 데모
데이터의 리포트만 조회된다.

**대표 발화는 조회할 때마다 만든다.** `utterance_analyses`의 근거에서 구성하며 요소당 가장
이른 턴 하나만 남긴다. `sttLowConfidence=true`인 발화는 후보에서 빠진다 - 저장된 원문이 아이가
실제로 한 말과 다를 수 있는데, 리포트는 보호자에게 "아이가 이렇게 말했다"고 보여주는 자리다.

### 2.12 단어장

| 메서드 | 경로 | 설명 | 요청 | 응답 | 상태 |
|---|---|---|---|---|---|
| POST | `/api/children/{childId}/words` | 모르는 단어를 저장한다. 아이 눈높이의 뜻은 LLM이 만든다 | `WordCreateRequest` | 201 `WordResponse` | ⚠️ `meaning`을 함께 보내면 동작. 생략하면 501 |
| GET | `/api/children/{childId}/words` | 단어 목록을 조회한다 | `?entryType=` (선택) | `List<WordResponse>` | ✅ |
| PATCH | `/api/children/{childId}/words/{wordId}/favorite` | 즐겨찾기를 켜고 끈다 | — | `WordResponse` | ✅ |
| DELETE | `/api/words/{wordId}` | 저장한 단어를 삭제한다 | — | 204 | ⛔ |

아이가 이야기를 듣다 모르는 단어를 누르는 경로에서는 뜻이 올 수 없어 LLM을 타므로, 벤더 선정
전까지 그 경로만 501이다. 클라이언트가 뜻을 담아 보내면 지금도 저장된다.

즐겨찾기와 삭제는 단어가 그 아이의 것인지까지 확인한다. 아이가 둘인 보호자가 `childId`에 다른
아이를 넣어 형제의 단어를 건드리지 못하게 하기 위해서이고, 없는 자원과 같이 404로 알린다.

### 2.13 보상 — 상점·보관함

| 메서드 | 경로 | 설명 | 요청 | 응답 | 상태 |
|---|---|---|---|---|---|
| GET | `/api/children/{childId}/shop/items` | 상점 목록을 조회한다. 해금과 구매 가능 여부를 서버가 계산해 함께 준다 | — | `List<ShopItemResponse>` | ✅ |
| POST | `/api/children/{childId}/items` | 아이템을 구매한다. 별가루를 차감한다. `Idempotency-Key` 헤더(선택)로 재전송 이중 차감 방지 | `ItemPurchaseRequest` | 201 `ItemPurchaseResponse` | ✅ |
| GET | `/api/children/{childId}/items` | 보유 아이템(보관함)을 조회한다 | `?placed=` (선택) | `List<ChildItemResponse>` | ✅ |

### 2.14 보상 — 별가루

| 메서드 | 경로 | 설명 | 요청 | 응답 | 상태 |
|---|---|---|---|---|---|
| GET | `/api/children/{childId}/stardust` | 별가루 잔액과 아직 연출하지 않은 획득 내역을 조회한다 | — | `StardustWalletResponse` | ✅ |
| POST | `/api/children/{childId}/stardust/acknowledge` | 획득 연출을 재생했다고 표시한다 | — | `StardustAcknowledgeResponse` | ✅ |

### 2.15 보상 — 행성·배치

| 메서드 | 경로 | 설명 | 요청 | 응답 | 상태 |
|---|---|---|---|---|---|
| GET | `/api/children/{childId}/planet` | 내 행성과 놓인 아이템, 다음 해금 목표를 조회한다 | — | `PlanetResponse` | ✅ |
| PATCH | `/api/children/{childId}/planet` | 행성 이름을 바꾼다 | `PlanetRenameRequest` | `PlanetRenameResponse` | ✅ |
| POST | `/api/children/{childId}/planet/tutorial-complete` | 행성 튜토리얼을 마쳤다고 표시한다 | — | `TutorialCompleteResponse` | ✅ |
| POST | `/api/children/{childId}/planet/placements` | 아이템을 행성 격자에 놓는다 | `PlacementCreateRequest` | 201 `PlacementResponse` | ✅ |
| PATCH | `/api/planet/placements/{placementId}` | 놓인 아이템을 다른 칸으로 옮긴다 | `PlacementMoveRequest` | `PlacementResponse` | ✅ |
| DELETE | `/api/planet/placements/{placementId}` | 놓인 아이템을 치운다 | — | 204 | ✅ |

배치 3종은 **행 단위 조작**이다. 스냅샷 통짜 저장이 아니라 놓기·옮기기·치우기를 각각 호출한다. 되돌리기 전용 API는 없고 클라이언트가 직전 조작의 역조작을 부른다.

### 2.16 음성

| 메서드 | 경로 | 설명 | 요청 | 응답 | 상태 |
|---|---|---|---|---|---|
| POST | `/api/stt` | 아이 음성을 텍스트로 변환한다. 원본 음성은 저장하지 않는다 | `multipart/form-data`, 파트명 `audio` | `TranscriptionResponse` | ✅ |
| POST | `/api/tts` | 대사 텍스트를 캐릭터 음성으로 합성한다 | `SynthesisRequest` | `SynthesisResponse` | ✅ |

두 건 모두 OpenAI 실측 구성으로 동작한다(벤더 비교용, 미결-01). `audioUrl`은 스토리지 선정 전까지 data URL(base64 mp3)로 내려간다. 자세한 내용은 7절 갱신분 참고.

> **멀티파트 한도** — 30초 16kHz mono WAV가 약 960KB로 Spring Boot 기본 1MB에 아슬아슬하게 걸려, `max-file-size`를 10MB로 올려 두었다.

---

## 3. DTO 상세

각 DTO 아래의 **사용처**가 그 DTO를 주고받는 엔드포인트다. `X에 중첩`은 단독 응답이 아니라 다른 DTO의 필드로만 실려 나간다는 뜻이고, 그때는 최종적으로 어느 엔드포인트가 전달하는지도 함께 적었다.

여기에 없는 엔드포인트는 **요청·응답 본문이 아예 없는 둘**뿐이다 — `POST /api/sessions/{sessionId}/stop`(200, 빈 본문)과 `DELETE /api/words/{wordId}`(204).

### 3.1 공통

#### `ErrorResponse`

> **사용처** — 모든 엔드포인트의 4xx·5xx 응답 본문

| 필드 | 타입 | 설명 |
|---|---|---|
| `code` | String | `ErrorCode` 이름 또는 `INVALID_REQUEST` |
| `message` | String | 사람이 읽는 설명 |

---

### 3.2 인증·계정

#### `SignUpRequest`

> **사용처** — `POST /api/auth/signup` 요청

| 필드 | 타입 | 검증 |
|---|---|---|
| `email` | String | `@NotBlank @Email @Size(max=255)` |
| `password` | String | `@NotBlank @Size(min=8, max=64)` |
| `name` | String | `@NotBlank @Size(max=50)` |

#### `LoginRequest`

> **사용처** — `POST /api/auth/login` 요청

| 필드 | 타입 | 검증 |
|---|---|---|
| `email` | String | `@NotBlank @Email` |
| `password` | String | `@NotBlank` |

#### `SocialLoginRequest`

> **사용처** — `POST /api/auth/social/{provider}` 요청

| 필드 | 타입 | 검증 |
|---|---|---|
| `authorizationCode` | String | `@NotBlank` |
| `redirectUri` | String | `@NotBlank` |

**서버가 인가 코드를 제공자 토큰으로 교환한다.** 클라이언트가 액세스 토큰을 직접 넘기지 않는다 — 넘기면 검증 없이 신뢰해야 한다.

#### `TokenRefreshRequest` / `LogoutRequest`

> **사용처** — `POST /api/auth/refresh` 요청 / `POST /api/auth/logout` 요청

| 필드 | 타입 | 검증 |
|---|---|---|
| `refreshToken` | String | `@NotBlank` |

#### `TokenResponse`

> **사용처** — `POST /api/auth/refresh` 응답 · `AuthResponse`·`SocialAuthResponse`에 중첩

| 필드 | 타입 | 설명 |
|---|---|---|
| `accessToken` | String | 이후 요청의 `Authorization: Bearer` 헤더에 담는다 |
| `refreshToken` | String | 회전(rotate)된 새 리프레시 토큰. 기존 토큰은 이 값 발급과 동시에 무효화된다 |
| `accessTokenExpiresIn` | long | 액세스 토큰 유효 기간(초). 기본 7일 |

#### `AuthResponse` / `SocialAuthResponse`

> **사용처** — `POST /api/auth/signup`·`/login` 응답 / `POST /api/auth/social/{provider}` 응답

| 필드 | 타입 | 설명 |
|---|---|---|
| `tokens` | `TokenResponse` | 발급된 토큰 묶음. 별도 로그인 없이 바로 쓴다 |
| `parent` | `ParentResponse` | 가입 또는 로그인한 보호자 프로필 |
| `isNewUser` | boolean | **`SocialAuthResponse`에만** — 최초 가입이면 true |

#### `ParentResponse`

> **사용처** — `GET /api/parents/me`·`PATCH /api/parents/me` 응답 · `AuthResponse`·`SocialAuthResponse`에 중첩

| 필드 | 타입 | 설명 |
|---|---|---|
| `id` | UUID | 보호자 식별자 |
| `email` | String | 소셜 전용 계정은 null |
| `name` | String | 보호자 이름 |
| `provider` | `AuthProvider` | **소셜일 때만 값.** 이메일 계정은 null |

DB의 `provider`는 `LOCAL`/`KAKAO` 둘 다 NOT NULL이지만, 응답에서는 `LOCAL`을 null로 바꿔 내린다. 클라이언트는 "값이 있으면 소셜"로만 판단하면 된다.

#### `ParentUpdateRequest`

> **사용처** — `PATCH /api/parents/me` 요청

| 필드 | 타입 | 검증 | 설명 |
|---|---|---|---|
| `name` | String | `@Size(max=50)` | 전달한 필드만 반영 |
| `currentPassword` | String | | `newPassword`와 함께 보낸다 |
| `newPassword` | String | `@Size(min=8)` | 보내면 현재 501 |

---

### 3.3 아이·동의

#### `ChildCreateRequest` / `ChildUpdateRequest`

> **사용처** — `POST /api/children` 요청 / `PATCH /api/children/{childId}` 요청

| 필드 | 타입 | 검증 |
|---|---|---|
| `name` | String | Create `@NotBlank @Size(max=50)` / Update `@Size(max=50)` |
| `birthYear` | Short | Create `@NotNull @Min(2000) @Max(2100)` / Update 동일하되 선택 |

#### `ChildResponse`

> **사용처** — `POST /api/children` · `GET /api/children` · `GET /api/children/{childId}` · `PATCH /api/children/{childId}` 응답

| 필드 | 타입 | 설명 |
|---|---|---|
| `id` | UUID | 아이 식별자 |
| `name` | String | 아이 이름. 고정 대사의 `ㅇㅇ` 자리에 치환된다 |
| `birthYear` | short | 출생연도 |
| `age` | int | **저장하지 않는다** — 현재연도 − 출생연도 |
| `consentStatus` | `ConsentStatus` | `VALID`/`NONE`/`WITHDRAWN` — 목록의 동의 뱃지용 파생값 |

#### `ConsentCreateRequest`

> **사용처** — `POST /api/children/{childId}/consents` 요청

| 필드 | 타입 | 검증 |
|---|---|---|
| `consentVersion` | String | `@NotBlank` (예: `mvp_v1`) |
| `verificationMethod` | `VerificationMethod` | `@NotNull` — `AUTHENTICATED_PARENT` / `INSTITUTION_PAPER` / `MOBILE_VERIFICATION` |

#### `ConsentResponse`

> **사용처** — `POST /api/children/{childId}/consents` · `POST /api/children/{childId}/consents/withdraw` 응답 · `ConsentStatusResponse`에 중첩

| 필드 | 타입 | 설명 |
|---|---|---|
| `id` | UUID | 동의 식별자 |
| `consentVersion` | String | 동의한 약관 버전 (예: `mvp_v1`) |
| `consentedAt` | OffsetDateTime | 동의한 시각 |
| `withdrawnAt` | OffsetDateTime | 철회 시각. null이면 유효 |

#### `ConsentStatusResponse`

> **사용처** — `GET /api/children/{childId}/consents` 응답

| 필드 | 타입 | 설명 |
|---|---|---|
| `current` | `ConsentResponse` | **null이면 새 세션을 시작할 수 없다** |
| `history` | `List<ConsentResponse>` | 철회분 포함 전체 이력 |

---

### 3.4 홈

#### `HomeResponse`

> **사용처** — `GET /api/children/{childId}/home` 응답

| 필드 | 타입 | 설명 |
|---|---|---|
| `inProgressSession` | `SessionSummaryResponse` | 없으면 null |
| `recommendedStories` | `List<StoryCardResponse>` | 현재는 PUBLISHED 최신 3개 |
| `planetWidget` | `PlanetWidget` | 홈에 띄우는 행성 요약. 별가루 잔액과 배치 수 |

**`PlanetWidget`**: `stardustBalance` int, `placedCount` int, `hasUnacknowledged` boolean

`hasUnacknowledged`가 true면 행성 진입 전에 연출 예고 점을 표시한다.

---

### 3.5 콘텐츠

#### `StoryCardResponse` — 목록과 홈 추천이 공유

> **사용처** — `StoryListResponse`·`StoryDetailResponse`·`HomeResponse`에 중첩 → 최종 전달: `GET /api/stories` · `GET /api/stories/{storyId}` · `GET /api/children/{childId}/home`

`id` · `title` · `summary` · `difficulty` · `estimatedMinutes`(Short) · `imageUrl` · `topics`(`List<String>`)

#### `StoryListResponse`

> **사용처** — `GET /api/stories` 응답

| 필드 | 타입 | 설명 |
|---|---|---|
| `stories` | `List<StoryCardResponse>` | `?topic=` 필터 적용 결과 |
| `topics` | `List<String>` | **필터와 무관하게 항상 전체** — 필터 칩을 그리는 용도 |

페이징은 없다. MVP 콘텐츠 수가 한 화면에 들어간다.

#### `StoryDetailResponse`

> **사용처** — `GET /api/stories/{storyId}` 응답

| 필드 | 타입 |
|---|---|
| `story` | `StoryCardResponse` (중첩) |
| `sceneCount` | int |
| `childRole` | String |
| `intro` | String |

#### `SceneContentResponse` — 세션 시작·이어하기·장면 전환·현재 장면이 공유

> **사용처** — `GET /api/stories/{storyId}/scenes` 응답 · `SessionStartResponse`·`SessionResumeResponse`·`SceneAdvanceResponse`·`CurrentSceneResponse`에 중첩

| 필드 | 타입 | 설명 |
|---|---|---|
| `sceneId` | UUID | 장면 식별자 |
| `sceneOrder` | short | 이야기 안에서의 장면 순서 |
| `sceneType` | `SceneType` | `STORY` / `DIALOGUE` |
| `narrationSentences` | `List<String>` | STORY만. DIALOGUE는 빈 배열 |
| `imageUrl` | String | 장면 배경 이미지 |
| `characterName` | String | DIALOGUE만 |
| `maxTurns` | Short | DIALOGUE만 — 남은 턴 UI |

**내레이션 분리는 서버가 한다.** 줄바꿈 기준으로 자른다 — 마침표로 자르면 `1.5km` 같은 표현이 깨진다.

**서버 내부 설정은 담지 않는다.** `element_criteria`·`remaining_worries`·`mission_config`·`scene_stance`·`proper_nouns`는 전부 LLM·STT 입력이라 클라이언트가 알 필요가 없다.

#### `TopicResponse`

> **사용처** — `GET /api/topics` 응답

`id`(UUID) · `name`(String)

---

### 3.6 세션

#### `SessionStartRequest`

> **사용처** — `POST /api/children/{childId}/sessions` 요청

`storyId`(UUID)

#### `SessionStartResponse`

> **사용처** — `POST /api/children/{childId}/sessions` 응답

`sessionId` · `status`(`SessionStatus`) · `currentScene`(`SceneContentResponse`) · `phase`(`PlayPhase`)

도입 장면을 즉시 렌더할 수 있게 콘텐츠 전체를 함께 준다.

#### `SessionResponse`

> **사용처** — `GET /api/sessions/{sessionId}` 응답 · `SessionResumeResponse`에 중첩

| 필드 | 타입 | 설명 |
|---|---|---|
| `sessionId` `childId` `storyId` | UUID | 세션과 그 세션이 물고 있는 아이·이야기 식별자 |
| `status` | `SessionStatus` | `IN_PROGRESS`/`POST_ACTIVITY`/`COMPLETED`/`STOPPED` |
| `currentScene` | `SceneRef` | `{sceneId, sceneOrder, sceneType}` — 식별 정보만 |
| `phase` | `PlayPhase` | `STORY`/`DIALOGUE`/`POST_ACTIVITY`/`ENDED` |
| `progress` | `ProgressResponse` | 현재 장면의 누적 진행 상태 |
| `sceneGoalMet` | boolean | 현재 장면의 목표 요소를 다 채웠는지 |
| `lastActivityAt` | OffsetDateTime | 마지막으로 상태가 바뀐 시각. 이어하기 카드 정렬에 쓴다 |

**`phase`는 저장값이 아니다.** `status` + 현재 장면 유형에서 파생한다. 프론트가 화면을 고르는 단일 근거라 서버가 계산해 내린다.

#### `ProgressResponse`

> **사용처** — `SessionResponse`·`UtteranceResponse`·`TurnStateResponse`에 중첩 → 최종 전달: `GET /api/sessions/{sessionId}` · `POST /api/sessions/{sessionId}/utterances` · `GET /api/sessions/{sessionId}/turn-state`

| 필드 | 타입 | 설명 |
|---|---|---|
| `mode` | `ResponseMode` | `NORMAL`/`GUIDED`/`CLOSING` |
| `accumulatedElements` | `List<ThinkingElement>` | 현재 장면 누적 |
| `missingElements` | `List<ThinkingElement>` | **저장하지 않고 계산** (목표 − 누적) |
| `turnCount` `maxTurns` | int | 현재 장면에서 아이가 말한 횟수와 그 장면의 최대 대화 범위 |
| `guidanceTarget` | `ThinkingElement` | GUIDED일 때만 |

#### `SessionResumeResponse`

> **사용처** — `GET /api/sessions/{sessionId}/resume` 응답

`session`(`SessionResponse`) · `currentScene`(`SceneContentResponse`) · `messages`(`List<MessageResponse>`) · `lastCharacterMessage`(`CharacterMessageResponse`) · `exposedMission`(`MissionResponse`)

`messages`는 세션 전체 내역이고 `lastCharacterMessage`는 마지막 캐릭터 발화다. `exposedMission`은 노출 판정이 `story.mission`에 있어 지금은 항상 null이다. 미션 구현 후 채워지며 응답 스키마는 바뀌지 않는다.

#### `SessionSummaryResponse` — 홈 이어하기 카드

> **사용처** — `HomeResponse`에 중첩 → 최종 전달: `GET /api/children/{childId}/home`

`sessionId` · `storyId` · `storyTitle` · `storyImageUrl` · `status` · `currentSceneOrder`(short) · `totalScenes`(int) · `lastActivityAt`

#### `MessageResponse`

> **사용처** — `GET /api/sessions/{sessionId}/messages` 응답 · `SessionResumeResponse`·`UtteranceResponse`에 중첩

| 필드 | 타입 | 설명 |
|---|---|---|
| `messageId` | UUID | 메시지 식별자 |
| `speakerType` | `SpeakerType` | `CHILD`/`CHARACTER`/`SYSTEM` |
| `turnOrder` | int | 세션 안에서 유일. 장면이 바뀌어도 이어진다 |
| `text` | String | 이름 치환이 끝난 상태 |
| `sttLowConfidence` | boolean | **아이 발화만 의미 있다** |
| `characterEmotion` | `CharacterEmotion` | 캐릭터 발화만 |
| `createdAt` | OffsetDateTime | 발화가 기록된 시각 |

`sttConfidence` 원값은 내부 지표라 내리지 않는다. 화면은 "미덥지 않았다"는 사실만 알면 다시 말하기를 안내할 수 있다.

#### `CharacterMessageResponse`

> **사용처** — `SceneOpeningResponse`·`SceneAdvanceResponse`·`SessionResumeResponse`·`UtteranceResponse`에 중첩 → 최종 전달: `POST /api/sessions/{sessionId}/scenes/current/opening` · `POST /api/sessions/{sessionId}/scenes/current/story-complete` · `GET /api/sessions/{sessionId}/resume` · `POST /api/sessions/{sessionId}/utterances`

`messageId` · `text` · `audioUrl`

`audioUrl`이 null이면 클라이언트가 `/api/tts`를 호출한다. 고정 대사는 사전 렌더 음성(`scene_audio`)을 내려줄 수 있다.

#### `SceneOpeningResponse`

> **사용처** — `POST /api/sessions/{sessionId}/scenes/current/opening` 응답

`message`(`CharacterMessageResponse`) · `alreadyOpened`(boolean)

**멱등이다.** 재호출하면 새 메시지를 만들지 않고 `alreadyOpened=true`로 알린다.

#### `SceneAdvanceResponse`

> **사용처** — `POST /api/sessions/{sessionId}/scenes/current/story-complete` 응답

`phase`(`PlayPhase`) · `currentScene`(`SceneContentResponse`) · `openingMessage`(`CharacterMessageResponse`)

다음 장면이 DIALOGUE면 고정 첫 대사를 함께 저장·반환한다. 마지막 장면이 STORY로 끝났다면 `phase=POST_ACTIVITY`이고 `currentScene`은 null이다.

#### `SceneTransitionResponse`

> **사용처** — `UtteranceResponse`에 중첩 — 장면 종료 턴에만 → 최종 전달: `POST /api/sessions/{sessionId}/utterances`

`next`(`SceneTransitionTarget`) · `nextSceneId` · `nextSceneOrder`(Integer) · `nextSceneType` · `closingReason`(`SceneEndReason`) · `resultImageUrl`(String, nullable)

`resultImageUrl`(2026-08 확정) — 끝난 장면의 결과 연출 이미지. 값이 있으면 마지막 대사 재생 뒤, 다음 장면을 그리기 전에 연출로 끼워 넣는다. 대화3의 "배가 떨어지는" 연출이 해당하며, 종료 사유나 미션 노출 여부와 무관하게 장면이 닫히면 항상 내려간다. 값의 원천은 `mission_config.result_image_url`이다.

#### `CurrentSceneResponse` / `TurnStateResponse`

> **사용처** — `GET /api/sessions/{sessionId}/scenes/current` 응답 / `GET /api/sessions/{sessionId}/turn-state` 응답

`{currentScene, phase}` / `{progress, phase}`

---

### 3.7 대화(턴)

#### `UtteranceRequest`

> **사용처** — `POST /api/sessions/{sessionId}/utterances` 요청

| 필드 | 타입 | 검증 | 설명 |
|---|---|---|---|
| `text` | String | `@NotBlank` | 확정 발화 텍스트 |
| `sttRawText` | String | | STT 최초 변환 텍스트 |
| `sttConfidence` | BigDecimal | `@DecimalMin(0.0) @DecimalMax(1.0)` | 선택 |
| `sttRetryCount` | Short | `@PositiveOrZero` | 선택, 기본 0 |
| `missionId` | String | | 이 발화가 미션 수행 결과일 때 |

헤더 `Idempotency-Key`(선택) — 발화마다 새 UUID를 만들고 재시도 사이에만 유지한다(1장 멱등키 참고).

**`sttLowConfidence`는 요청에 없다.** 기준값(0.5, 2026-08 확정) 판정은 저장 시 서버가 한다 — 클라이언트마다 기준이 갈리면 리포트 필터링이 흔들린다. `sttConfidence`는 `/api/stt` 응답의 `confidence`를 그대로 되올린다.

#### `UtteranceResponse` — 단일 스키마, null 여부로 분기

> **사용처** — `POST /api/sessions/{sessionId}/utterances` 응답

| 필드 | 타입 | 언제 값이 있나 |
|---|---|---|
| `childMessage` | `MessageResponse` | 항상 |
| `analysis` | `AnalysisResponse` | 항상 |
| `progress` | `ProgressResponse` | 항상 |
| `characterMessage` | `CharacterMessageResponse` | 항상 (종료 시엔 고정 마지막 대사) |
| `closingReaction` | `CharacterMessageResponse` | **최대 턴 종료 턴** - 아이의 마지막 발화에 대한 짧은 반응. `characterMessage`보다 먼저 재생한다 |
| `mission` | `MissionResponse` | **미션 노출 턴** |
| `sceneTransition` | `SceneTransitionResponse` | **장면 종료 턴** (`progress.mode=CLOSING`) |
| `safety` | `SafetyResponse` | **위험 신호로 대사 생성이 중단된 턴** |

**분기 판단**
- 대화 계속 → `mission`·`sceneTransition`·`safety` 모두 null
- 미션 노출 → `mission` 있음
- 장면 종료 → `sceneTransition` 있음
- 안전 개입 → `safety` 있음. 이때 `characterMessage`는 생성 대사가 아니라 **안전 문구**다

#### `AnalysisResponse`

> **사용처** — `UtteranceResponse`에 중첩 → 최종 전달: `POST /api/sessions/{sessionId}/utterances`

`childIntent`(13종) · `mainPoint` · `detectedElements`(`List<DetectedElement>`) · `utteranceValidity`(5종)

**캐릭터 표정·태도 변화의 트리거를 겸한다.** 프론트가 자체 판단하지 않고 이 값으로만 연출한다.

`analysisVersion`·`modelId`·`droppedEvidence`는 내부 추적용이라 내리지 않는다.

**`DetectedElement`**: `type`(`ThinkingElement`) · `evidence`(String — 아이 발화 원문의 근거 문구)

#### `SafetyResponse`

> **사용처** — `UtteranceResponse`에 중첩 — 안전 개입 턴에만 → 최종 전달: `POST /api/sessions/{sessionId}/utterances`

| 필드 | 타입 | 설명 |
|---|---|---|
| `categories` | `List<String>` | 감지 범주만. **아이 발화 원문은 담지 않는다** |
| `recoveryAvailable` | boolean | true면 오탐 복귀 버튼 노출 |

---

### 3.8 미션

#### `MissionResponse`

> **사용처** — `CurrentMissionResponse`·`UtteranceResponse`·`SessionResumeResponse`에 중첩 → 최종 전달: `GET /api/sessions/{sessionId}/missions/current` · `POST /api/sessions/{sessionId}/utterances` · `GET /api/sessions/{sessionId}/resume`

| 필드 | 타입 | 설명 |
|---|---|---|
| `missionId` | String | 장면 `mission_config`에 정의된 미션 키. 결과 제출 경로에 그대로 쓴다 |
| `missionType` | `MissionType` | `PROBLEM_SOLVING` / `PERSPECTIVE_SHIFT` |
| `title` `description` | String | 미션 오버레이에 띄우는 제목과 안내 문구 |
| `payload` | `Payload` | `{questions, cards}` — **유형에 따라 한쪽만 값이 있다** |

- `Question`: `key`(`tool`/`reason`/`request`/`expectedResult`로 고정) · `label`
- `Card`: `key` · `label` · `imageUrl` · `template`

#### `CurrentMissionResponse`

> **사용처** — `GET /api/sessions/{sessionId}/missions/current` 응답

`mission`(`MissionResponse`) — **미노출 상태면 null이고 404가 아니다.** 노출 여부는 정상 상태이지 오류가 아니다.

#### `MissionResultRequest`

> **사용처** — `POST /api/sessions/{sessionId}/missions/{missionId}/result` 요청

| 필드 | 타입 | 설명 |
|---|---|---|
| `answers` | `Map<String,String>` | 미션1 — Question의 key별 답 |
| `cards` | `List<CardAnswer>` | 미션2 — `{key, strengthText}` |

#### `MissionResultResponse`

> **사용처** — `POST /api/sessions/{sessionId}/missions/{missionId}/result` 응답

`missionId` · `accepted`(boolean) — 결과는 다음 턴 캐릭터 대사에 반영된다.

---

### 3.9 말하기 후 활동

#### `PostActivityStartResponse`

> **사용처** — `POST /api/sessions/{sessionId}/post-activity/start` 응답 · 중첩 `Card`는 `PostActivityStatusResponse`도 재사용

`cards`(`List<Card>`) · `attemptCount`(short)
- `Card`: `cardId` · `text`

**정답 순서는 담지 않는다.** 판정은 서버만 한다.
카드 순서는 `card_order_seed`로 고정되어 재호출해도 같다.

#### `PostActivityStatusResponse`

> **사용처** — `GET /api/sessions/{sessionId}/post-activity` 응답

| 필드 | 타입 | 설명 |
|---|---|---|
| `status` | String | 후속 활동 진행 단계 |
| `cards` | `List<PostActivityStartResponse.Card>` | 시작 응답과 **같은 순서** (시드로 고정) |
| `attemptCount` | short | 카드 순서를 제출한 횟수. 오답마다 늘어난다 |
| `isOrderCorrect` | Boolean | 아직 제출 전이면 null |
| `retellingKeywords` | `List<String>` | 카드 순서를 맞춘 뒤에만 값이 있다 |
| `retellingText` | String | 아이가 제출한 다시 이야기하기 원문. 제출 전이면 null |
| `completedAt` | OffsetDateTime | 후속 활동을 마친 시각. 진행 중이면 null |

**새로고침 복구용이다.** 앱을 껐다 켜도 이 응답 하나로 후속 활동 화면을 그대로 되살린다 — 카드 순서가 시드로 고정돼 있어 같은 화면이 나온다.

#### `CardSubmitRequest` / `CardSubmitResponse`

> **사용처** — `POST /api/sessions/{sessionId}/post-activity/order` 요청 / 응답

| | 필드 | 설명 |
|---|---|---|
| 요청 | `submittedOrder` `List<String>` `@NotEmpty` | cardId 순서 |
| 응답 | `correct` boolean, `retellingKeywords` `List<String>` | **오답이면 `retellingKeywords`가 null** (재시도) |

#### `RetellingRequest`

> **사용처** — `POST /api/sessions/{sessionId}/post-activity/retelling` 요청

`text`(`@NotBlank`) · `sttRawText`

#### `RetellingResponse` — 재구성 발화 제출 = 세션 완료 + 별가루 지급

> **사용처** — `POST /api/sessions/{sessionId}/post-activity/retelling` 응답

| 필드 | 타입 | 설명 |
|---|---|---|
| `sessionStatus` | String | 처리 후 세션 상태. 정상 완료면 `COMPLETED` |
| `completedAt` | OffsetDateTime | 세션을 완료 처리한 시각 |
| `stardust` | `Stardust` | `{earned, breakdown, balance}` |
| `unlockedItems` | `List<UnlockedItem>` | `{itemId, name, thumbnailUrl}` — 이번 완주로 열린 것 |

지급 결과를 이 응답에 담아야 하므로 **세션 완료 처리는 같은 트랜잭션에서 동기로 끝낸다.**

---

### 3.10 리포트

#### `ReportListResponse`

> **사용처** — `GET /api/children/{childId}/reports` 응답

`id` · `sessionId` · `storyTitle` · `createdAt`

#### `ReportDetailResponse`

> **사용처** — `GET /api/sessions/{sessionId}/report` · `POST /api/sessions/{sessionId}/report` 응답

| 필드 | 타입 | 설명 |
|---|---|---|
| `id` `sessionId` | UUID | 리포트 식별자와 대상 세션 |
| `storyTitle` | String | 어떤 이야기를 한 회차인지 보여주는 제목 |
| `summary` | String | 보호자에게 보여줄 전체 요약 문장 |
| `strengths` | `List<ReportItem>` | `{element, comment}` — 이번 회차에 잘 보여준 요소 |
| `nextFocus` | `List<ReportItem>` | `{element, comment}` — 다음에 연습하면 좋을 요소 |
| `representativeUtterances` | `List<RepresentativeUtterance>` | `{text, element}` — 근거가 된 아이 발화 |
| `createdAt` | OffsetDateTime | 리포트가 생성된 시각 |

**`ReportItem`을 그대로 내린다.** 요소 코드만 내리면 화면에 "REASON"만 뜨고 왜 잘했는지가 사라진다 — 보호자에게 보여줄 문장이 `comment`에 있다.

**대표 발화는 저장값이 아니다.** 조회 시 `messages` + `utterance_analyses`의 근거에서 구성하고, `sttLowConfidence=true`인 발화는 후보에서 제외한다.

**요소당 한 건만 담는다.** 같은 요소가 여러 턴에서 확인되면 가장 이른 턴을 쓴다. 전부 담으면 비슷한 문장이 반복돼 무엇을 잘했는지가 오히려 흐려진다. 근거 문구가 비어 있으면 발화 전체로 대신한다.

---

### 3.11 단어장

#### `WordCreateRequest`

> **사용처** — `POST /api/children/{childId}/words` 요청

| 필드 | 타입 | 검증 | 설명 |
|---|---|---|---|
| `word` | String | `@NotBlank @Size(max=50)` | 저장할 단어 |
| `entryType` | `WordEntryType` | `@NotNull` | `UNKNOWN` / `FAVORITE` |
| `sourceSceneId` | UUID | | 단어가 나온 장면. 뜻 생성의 문맥으로 쓴다 |
| `meaning` | String | | **없으면 서버가 LLM으로 생성** |
| `exampleSentence` | String | | 이야기 속 예문. 없으면 뜻과 함께 서버가 생성 |

같은 아이가 같은 단어를 또 저장하면 409 `DUPLICATE_WORD`.

#### `WordResponse`

> **사용처** — `POST /api/children/{childId}/words` · `GET /api/children/{childId}/words` · `PATCH /api/children/{childId}/words/{wordId}/favorite` 응답

`id` · `word` · `meaning` · `exampleSentence` · `entryType` · `sourceSceneId` · `createdAt`

---

### 3.12 보상

#### `ShopItemResponse`

> **사용처** — `GET /api/children/{childId}/shop/items` 응답

| 필드 | 타입 | 설명 |
|---|---|---|
| `itemId` | UUID | 아이템 식별자. 구매 요청에 그대로 쓴다 |
| `name` `category` | String, `ItemCategory` | 아이템 이름과 분류 |
| `price` | int | 별가루 가격. 화면에는 아이콘 개수로 표시한다 |
| `modelUrl` `thumbnailUrl` | String | 3D 모델과 목록 썸네일 |
| `unlocked` | boolean | 해금 조건을 채웠는지. 서버가 아이 상태로 매번 계산한다 |
| `silhouette` | boolean | 잠긴 아이템을 실루엣으로 표시할지 |
| `unlockGuide` | `UnlockGuide` | `{storyTitle, storyImageUrl}` — 이야기 완주 해금만 |
| `purchasable` | boolean | 해금 + 잔액 충분 |
| `shortfall` | int | 모자란 별가루 |

**해금·구매 가능·부족 수량은 전부 서버가 계산해 내린다.** 프론트 판정 금지 — 가격 규칙이 두 곳에 있으면 어긋난다.

`status=HIDDEN`인 아이템은 목록에서 빠진다(응답 필드로 노출하지 않는다).

#### `ItemPurchaseRequest` / `ItemPurchaseResponse`

> **사용처** — `POST /api/children/{childId}/items` 요청 / 응답

`itemId`(`@NotNull`) → `{item: ChildItemResponse, balance: int}`

#### `ChildItemResponse`

> **사용처** — `GET /api/children/{childId}/items` 응답 · `ItemPurchaseResponse`에 중첩

`childItemId` · `itemId` · `name` · `category` · `thumbnailUrl` · `modelUrl` · `acquiredAt` · `placed`(boolean)

**`placed=false`면 보관함에 있다.** 보관함은 `child_items` − `planet_items`로 계산하는 파생값이다.

#### `StardustWalletResponse`

> **사용처** — `GET /api/children/{childId}/stardust` 응답

`balance` · `totalEarned` · `unacknowledged`(`List<StardustTransactionResponse>`)

`unacknowledged`가 비어 있지 않으면 행성 진입 시 별가루가 떨어지는 연출을 재생한다.

#### `StardustTransactionResponse`

> **사용처** — `StardustWalletResponse`·`RetellingResponse`에 중첩 → 최종 전달: `GET /api/children/{childId}/stardust` · `POST /api/sessions/{sessionId}/post-activity/retelling`

`transactionId` · `amount`(지급 +, 사용 −) · `reason` · `createdAt`

`reason`: `STORY_COMPLETED` / `SCENE_BONUS` / `ITEM_PURCHASE` / `ADMIN_ADJUST`
한 세션에서 `SCENE_BONUS`가 **최대 2건** 나올 수 있다.

`sessionId`·`sceneId`는 서버 멱등 판정용이라 내리지 않는다.

#### `StardustAcknowledgeResponse`

> **사용처** — `POST /api/children/{childId}/stardust/acknowledge` 응답

`acknowledgedCount`(int)

#### `PlanetResponse`

> **사용처** — `GET /api/children/{childId}/planet` 응답

| 필드 | 타입 | 설명 |
|---|---|---|
| `planetId` | UUID | 행성 식별자. 아이 1명당 1개 |
| `name` | String | 아이가 붙인 행성 이름 |
| `tutorialCompleted` | boolean | 배치 튜토리얼을 봤는지. false면 첫 진입 안내를 띄운다 |
| `placedItems` | `List<PlacementResponse>` | 지금 행성에 놓여 있는 아이템과 좌표 |
| `progress` | `Progress` | `{placedCount, nextUnlock}` |

- `NextUnlock`: `itemName` · `thumbnailUrl` · `conditionText` — 모두 해금되면 null

**판 크기·모양은 응답에 없다.** 클라이언트 카탈로그가 단일 소스다.

#### `PlacementCreateRequest` / `PlacementMoveRequest` / `PlacementResponse`

> **사용처** — `POST /api/children/{childId}/planet/placements` 요청 / `PATCH /api/planet/placements/{placementId}` 요청 / 두 엔드포인트의 응답 · `PlanetResponse`에 중첩

| | 필드 |
|---|---|
| Create | `childItemId`(`@NotNull`) · `placedQ`(`@NotNull`) · `placedR`(`@NotNull`) |
| Move | `placedQ` · `placedR` |
| Response | `placementId` · `childItemId` · `itemId` · `modelUrl` · `placedQ` · `placedR` |

**좌표는 축좌표(q, r)이고 음수가 유효하다.** 원점 기준이라 `@PositiveOrZero`를 붙이면 절반의 판이 막힌다.

같은 칸에 놓으면 409 `CELL_OCCUPIED`, 이미 배치된 아이템이면 409 `ITEM_ALREADY_PLACED`.

#### `PlanetRenameRequest` / `PlanetRenameResponse` / `TutorialCompleteResponse`

> **사용처** — `PATCH /api/children/{childId}/planet` 요청 / 응답 · `POST /api/children/{childId}/planet/tutorial-complete` 응답

`name`(`@NotBlank @Size(1..30)`) → `{planetId, name}` / `{tutorialCompleted}`

---

### 3.13 음성

#### `TranscriptionResponse`

> **사용처** — `POST /api/stt` 응답

- `text`(String) — 인식 결과가 비면 422 `STT_EMPTY_TEXT`
- `confidence`(BigDecimal, 0~1, nullable) — exp(토큰 logprob 평균). 클라이언트는 이 값을
  발화 제출의 `sttConfidence`에 그대로 되올린다. 벤더가 logprob을 못 주면 null
- `lowConfidence`(boolean) — 기준값(0.5) 미만 여부. 판정은 서버가 한다. true면 제출 전에
  "잘 못 알아들었을 수 있어요" 다시 말하기 안내를 띄운다 (비차단 - 아이가 그대로 제출해도 된다)

#### `SynthesisRequest` / `SynthesisResponse`

> **사용처** — `POST /api/tts` 요청 / 응답

| | 필드 | 설명 |
|---|---|---|
| 요청 | `text`(`@NotBlank`) · `characterName` | 이름이 있으면 캐릭터 보이스, 없으면 내레이션 보이스 |
| 응답 | `audioUrl` · `expiresAt` | **바이트를 직접 내리지 않는다** — URL이라야 다시 듣기·캐싱이 된다 |

---

## 4. 여러 응답이 공유하는 DTO

정확한 경로는 §3의 각 DTO **사용처**에 있다. 이 표는 "어떤 것이 공유되는지"만 한눈에 보기 위한 것이다.

| DTO | 쓰이는 곳 |
|---|---|
| `StoryCardResponse` | 이야기 목록, 이야기 상세, 홈 추천 |
| `SceneContentResponse` | 세션 시작, 이어하기, 장면 전환, 현재 장면 |
| `ProgressResponse` | 세션 조회, 턴 처리, 턴 상태 |
| `MessageResponse` | 대화 기록 조회, 이어하기, 턴 처리 |
| `CharacterMessageResponse` | 턴 처리, 첫 대사 재생, 장면 이동, 이어하기 |
| `ParentResponse` | 회원가입, 로그인, 소셜 로그인, 내 정보 |
| `StardustTransactionResponse` | 지갑 조회, 후속 활동 완료 |
| `ChildItemResponse` | 보관함 조회, 구매 결과 |
| `PlacementResponse` | 행성 조회, 놓기, 옮기기 |
| `SessionSummaryResponse` | 홈 이어하기 |

한 화면이 아니라 **한 개념이 하나의 DTO를 갖는다.** 화면마다 DTO를 만들면 같은 개념이 여러 형태로 갈라진다.

---

## 5. 응답 설계 원칙

**1. 판정은 전부 서버가 한다.** 해금 여부·구매 가능·부족 수량·정답 여부·진행 단계·부족 요소를 클라이언트가 계산하지 않는다. 규칙이 두 곳에 있으면 반드시 어긋난다.

**2. 파생값은 저장하지 않고 응답에서 계산한다.** `age`, `missingElements`, `phase`, `placed`, 보관함, 대표 발화.

**3. 서버 내부 설정은 내리지 않는다.** LLM·STT 입력(`element_criteria`, `remaining_worries`, `scene_stance`, `proper_nouns`), 추적용 메타(`analysisVersion`, `modelId`, `droppedEvidence`), 멱등 키(`card_order_seed`, 거래의 `sessionId`/`sceneId`).

**4. 분기는 필드 null로 표현한다.** 응답 스키마를 유형별로 나누지 않는다 — `UtteranceResponse` 하나로 대화 계속·미션 노출·장면 종료·안전 개입을 모두 표현한다.

**5. 정답은 내리지 않는다.** 카드 정답 순서, 미션 모범 답안.

**6. 아이 발화 원문은 안전 응답에 담지 않는다.** `SafetyResponse.categories`는 범주만 담는다.

---

## 6. 미해결 항목

| # | 항목 | 현재 | 조치 |
|---|---|---|---|
| 1 | **`TokenResponse.refreshToken`이 항상 null** | 저장소(`refresh_tokens` 테이블·`RefreshToken` 엔티티)는 준비됐고 발급·회전·무효화 로직만 없다 | 그때까지 **Access 토큰 단일 전략으로 완결 동작한다**(→ §8). 도입해도 응답 스키마는 그대로라 클라이언트 변경이 없다 |
| 2 | **멀티파트 1MB 한도** | `application.yml`에 `spring.servlet.multipart` 설정 없음 → Boot 기본 1MB | 30초 WAV ≈ 960KB라 아슬아슬하다. 10MB로 올린다 |
| 4 | **`SafetyResponse` 감지 로직** | 계약 자리만 확정. 항상 null | AI 파이프라인 연동 시 |
| 5 | **`CharacterEmotion` 고정 6종** | 응답 enum이 고정인데 DB는 CHECK를 풀었다 | 캐릭터별 `expression_keys`로 옮기면 문자열 키 + fallback으로 바꾼다 |
| 6 | **`DELETE /api/words/{wordId}`** | 경로에 `childId`가 없어 소유권 검증 경로가 애매. `WordbookService.delete`는 소유 검증까지 구현돼 있고 컨트롤러만 501이다 | 경로를 `/api/children/{childId}/words/{wordId}`로 맞추면 컨트롤러만 바꾸면 된다 |
| 8 | **아이템 발판(footprint)** | 카탈로그 정의가 없어 모든 아이템을 1칸으로 보고 배치 검증을 한다 | 2x2 아이템의 비앵커 칸이 겹칠 수 있다. 카탈로그가 나오면 `PlanetService`의 빈 칸 검사에 점유 칸 계산을 더한다 |
| 7 | **`SynthesisRequest.characterName`이 이름 문자열** | `characters` 테이블이 생겼으니 키로 지정하는 편이 안전 | `characterKey` 또는 `sceneId`+`slot`으로 전환 검토 |

---

## 7. 구현 현황 요약

| 영역 | 엔드포인트 | ✅ | ⚠️ | ⛔ |
|---|---|---|---|---|
| 인증 | 5 | 2 | 1 | 2 |
| 보호자 | 2 | 1 | 1 | 0 |
| 아이·동의 | 8 | 8 | 0 | 0 |
| 홈 | 1 | 1 | 0 | 0 |
| 콘텐츠 | 4 | 4 | 0 | 0 |
| 세션·장면 | 8 | 8 | 0 | 0 |
| 대화·미션 | 4 | 3 | 0 | 1 |
| 후속 활동 | 4 | 4 | 0 | 0 |
| 리포트 | 3 | 2 | 0 | 1 |
| 단어장 | 4 | 2 | 1 | 1 |
| 보상 | 11 | 11 | 0 | 0 |
| 음성 | 2 | 2 | 0 | 0 |
| **합계** | **56** | **48** | **3** | **5** |

⛔ 5건은 **DTO 계약이 확정된 상태**다. 프론트는 이 문서의 스키마대로 붙여 두면 서비스 구현 후 계약 변경 없이 동작한다.

⚠️ 3건의 내용은 이렇다. 소셜 로그인은 카카오와 구글만, 내 정보 수정은 이름만, 단어 저장은 `meaning`을 함께 보내면 동작.

**2026-08-13 갱신분 5** (집계 변동 없음 - ⚠️ 범위만 넓어짐)

| 엔드포인트 | 이전 | 현재 | 근거 |
|---|---|---|---|
| `POST /api/auth/social/{provider}` | ⚠️ kakao만 | ⚠️ kakao, google | 구글 OAuth 인가 코드 교환 추가(PR #17). 그 외 공급자는 여전히 501 |

**2026-08-13 갱신분 4** (직전 집계는 46/3/7이었다)

| 엔드포인트 | 이전 | 현재 | 근거 |
|---|---|---|---|
| `POST /api/stt` | ⛔ | ✅ | OpenAI gpt-4o-mini-transcribe. 왕복 실측 STT 0.5~1.8초 |
| `POST /api/tts` | ⛔ | ✅ | OpenAI gpt-4o-mini-tts. 실측 1.5~2.6초, 대사 한 문장 약 60KB |

**벤더 확정이 아니라 비교용 실측 구성이다(미결-01).** SttClient/TtsClient 인터페이스
뒤의 OpenAI 구현체이므로 다른 벤더로 확정되면 구현체만 갈면 된다.

- TTS `audioUrl`은 data URL(base64 mp3)이다. 스토리지 선정 전까지의 방식이며 응답 계약
  (`audioUrl`, `expiresAt`)은 그대로다. `expiresAt`은 null - data URL은 만료가 없다
- 캐릭터 3인 보이스와 말투는 서버 설정으로 매핑한다(`external.tts.voices`). 요청의
  `characterName`이 매핑에 없으면 내레이션 보이스로 합성한다
- 멀티파트 한도를 10MB로 올렸다(팀 확정 대기였던 항목. STT 실측을 위해 적용)
- 실측 관찰: 왕복(TTS 합성음을 STT로 되읽기)에서 희귀어 "방귀"가 "방비/반비"로
  오인식되는 경우가 있었다. `external.stt.vocabulary-hint`(시드 proper_nouns 합집합)로
  개선했지만 비결정적이다. **아동 실녹음 인식률 검증(미결-01)은 여전히 필요하다.**
  장면별 proper_nouns를 요청에 실어 보내는 구조는 /api/stt 계약에 장면 정보가 없어
  보류했다 - 계약 변경이 필요하면 함께 정한다

**2026-08-13 갱신분 3** (직전 집계는 45/4/7이었다)

| 엔드포인트 | 이전 | 현재 | 근거 |
|---|---|---|---|
| `POST /api/sessions/{sessionId}/utterances` | ⚠️ | ✅ | gpt-5-mini 실호출 검증 통과 |

**발화 제출이 완전 동작한다.** 대화 턴 파이프라인의 마지막 구멍이었다. 실호출 측정값:
분석 3.1초, 캐릭터 2.9초로 턴당 LLM 구간 약 6초(reasoning effort minimal). 남은 501은
미션 결과 제출, 리포트 생성, 단어 삭제, 단어 뜻 생성(단어 저장의 meaning 생략 경로),
토큰 재발급과 로그아웃, 음성 2건이다.

**2026-08-13 갱신분 2**

LLM 어댑터 2건(분석, 캐릭터)을 gpt-5-mini로 구현했다. `POST /utterances`는 코드상 전
구간이 이어졌고, 실호출 검증(LlmSmokeTest, .env에 LLM_API_KEY 필요)까지 통과했다.

근거 문서 6종(통합 명세서, 발화 분석 연동 기준, 대화 작동 규칙, 콘텐츠, 캐릭터 성격,
요구사항 목록)을 전수 대조하면서 규칙 세 가지를 문서 확정값에 맞췄다.

- 유도 판단의 남은 턴 기준을 "미충족 요소 수"에서 문서 확정값 "남은 턴 <= 2"로 정정
- 진행 임계값(2/2/2)을 운영 설정(`progression.guidance.*`)으로 분리 (진행-17)
- 약한 유도(soft-cue) 구현 (진행-13/14). NORMAL이어도 신규 요소가 잡히고 필수 요소가
  남았으면 캐릭터의 남은 걱정을 가볍게 얹는다. 장난/질문/불명확 반응이면 생략한다.
  이때 `guidanceTarget`이 soft 대상으로 기록되므로 **turn-state의 guidanceTarget은
  "강한 유도 대상"이 아니라 "직전 유도 또는 soft-cue 대상"이다**
- 반응 원칙 키 7종(reactionKey)을 서버가 계산해 캐릭터 프롬프트에 전달 (대화 작동 규칙 3.1)

**확정(2026-08)**: 콘텐츠 문서의 "최대 턴 도달 시 짧게 반응 후 마지막 대사" 흐름을
MAX_TURNS 종료에 한정해 구현했다. 응답의 `closingReaction`(별도 메시지)이 짧은 반응이고
`characterMessage`(고정 마무리 대사)보다 먼저 재생한다. 목표 달성(GOAL_MET) 종료는 문서
요구가 없고 마무리 대사가 자연스러운 연결이라 현행(마무리 대사만)을 유지한다.

**2026-08-13 갱신분** (직전 집계는 42/4/10이었다)

턴 처리 파이프라인이 붙었다.

| 엔드포인트 | 이전 | 현재 | 근거 |
|---|---|---|---|
| `POST /api/sessions/{sessionId}/utterances` | ⛔ | ⚠️ | 파이프라인 구현. 분석·캐릭터 LLM 클라이언트만 비어 있어 호출하면 501 |
| `GET /api/sessions/{sessionId}/turn-state` | ⛔ | ✅ | 구현됨 |
| `GET /api/sessions/{sessionId}/missions/current` | ⛔ | ✅ | 노출 판정은 턴 처리가 하고 여기서는 읽기만 한다 |
| `GET /api/sessions/{sessionId}/resume` | ⚠️ | ✅ | `exposedMission`이 채워졌다 |

**남은 것은 LLM 어댑터 두 개뿐이다.** 저장, 후처리, 진행 판단, 미션 노출, 장면 종료와 이동,
장면 보너스 지급까지 전 구간이 붙어 있고 `TurnOrchestratorTest`가 LLM만 대역으로 바꿔
처음부터 끝까지 검증한다. `AnalysisLlmClient`·`CharacterLlmClient`를 채우면 그대로 동작한다.

**위험 신호 감지(`UtteranceResponse.safety`)는 아직 항상 null이다.** 계약만 잡혀 있고
감지 자체가 AI 파이프라인에 딸려 있다.

턴 처리 중 외부 호출을 트랜잭션 밖으로 뺐다. 응답 계약은 그대로지만 겹친 발화의 실패 방식이
달라졌다 — 자세한 내용은 [트러블슈팅_턴_처리_커넥션_점유.md](트러블슈팅_턴_처리_커넥션_점유.md)에 있다.

**2026-08-12 갱신분 3** (직전 집계는 38/4/14였다)

후속 활동 4건을 구현했다. **이로써 세션 완주부터 보상까지 한 줄로 이어진다.** 카드 순서를
맞히고 다시 이야기하기를 내면 세션이 완료되고 별가루가 들어오며, 그 별가루로 상점에서
아이템을 사서 행성에 놓을 수 있다.

남은 10건은 대화 턴 4, 리포트 생성 1, 단어 삭제 1, 토큰 재발급과 로그아웃 2, 음성 2다.

**2026-08-12 갱신분 2** (직전 집계는 27/4/25였다)

보상 11건을 구현했다. 해금과 구매 가능 여부는 저장하지 않고 아이 상태에서 매번 계산하며,
기준은 잔액이 아니라 누적 획득량이다. 배치는 행 단위 조작이고 치우기는 보관함 복귀다.

**별가루 적립은 API로 열지 않는다.** 세션 완료 처리 안에서 서버가 넣는다. 지금은 완주
경로가 후속 활동에 있어 아직 이어지지 않았고, 장면 보너스는 대화 턴 파이프라인이 붙어야 한다.

**2026-08-12 갱신분** (직전 집계는 25/3/28이었다)

| 엔드포인트 | 이전 | 현재 | 근거 |
|---|---|---|---|
| `GET /api/children/{childId}/reports` | ⛔ | ✅ | 구현됨 |
| `GET /api/sessions/{sessionId}/report` | ⛔ | ✅ | 구현됨. 리포트가 없으면 409 |
| `POST /api/children/{childId}/words` | ⛔ | ⚠️ | `meaning`을 함께 보내면 동작. 생략하면 LLM이 필요해 501 |

리포트는 조회만 열렸다. 생성이 LLM에 걸려 있어 지금 조회되는 것은 시드 데모 데이터뿐이고,
대화 턴 파이프라인이 붙어야 실데이터가 쌓인다.

**2026-08-11 갱신분** (직전 집계는 25/2/29였다)

| 엔드포인트 | 이전 | 현재 | 근거 |
|---|---|---|---|
| `GET /api/sessions/{sessionId}/resume` | ⛔ | ⚠️ | 구현됨. `exposedMission`만 미션 미구현이라 null |
| `POST /api/sessions/{sessionId}/scenes/current/opening` | ⛔ | ✅ | 구현됨(멱등) |
| `GET /api/sessions/{sessionId}/scenes/current` | ⛔ | ✅ | 구현됨 |
| `POST /api/stt`, `POST /api/tts` | ✅ | ⛔ | 표기 오류 정정. 벤더 클라이언트가 비어 있어 호출하면 501이다 |

세션·장면 8건이 모두 열리면서 **이야기 시작부터 장면 전환, 새로고침 복구까지 대화 턴을 제외한 재생 흐름 전체가 동작한다.** 남은 구멍은 `POST /utterances` 하나이고, 그 안의 누적 상태 갱신(`StorySession.applyTurn`)은 이미 구현돼 단위 테스트까지 있다.

---

## 8. Access 토큰 단일 전략 — 동작 확인

리프레시 토큰 없이도 인증이 완결되는지 실제로 앱을 띄워 확인했다(2026-08-10).

**구조상 결합이 없다** — `RefreshToken` 엔티티와 `RefreshTokenRepository`는 존재하지만 **어떤 서비스도 참조하지 않는다.** 인증 경로는 `JwtProvider`(발급·검증) → `JwtAuthFilter`(헤더 파싱) → `SecurityContext`로 끝난다. 리프레시 미구현이 다른 기능을 막지 않는다.

**검증한 흐름**

| 단계 | 결과 |
|---|---|
| 가입 → 토큰 발급 | 201, `accessTokenExpiresIn=604800`(7일), `refreshToken=null` |
| 로그인 → 토큰 발급 | 200 |
| 토큰으로 아이 생성 | 201 — 행성·지갑이 각 1건 자동 생성됨 |
| 토큰으로 동의 등록 → 세션 시작 → 장면 진행 | 201 / 201 / 200 |
| 토큰 없음 / 위조 / **만료** | 전부 **401** `UNAUTHORIZED` |
| 남의 아이 조회 | **403** `FORBIDDEN` |
| 만료 후 **재로그인** → 같은 세션 재조회 | 200 — **진행 상태가 그대로 이어진다** |

**결론: 리프레시 없이 운영 가능하다.** 서버가 무상태라 토큰만 새로 받으면 세션·진행 기록이 그대로 살아 있다. 사용자 입장의 유일한 비용은 7일마다 재로그인이다.

**단, 확인 과정에서 결함 하나를 고쳤다.** 스프링 시큐리티 기본값은 미인증·권한없음을 **모두 403 + 빈 본문**으로 돌려줘서, 만료된 토큰이 "권한 없음"으로 보였다. 재로그인이 유일한 복구 경로인 상황에서 클라이언트가 그 시점을 알아챌 방법이 없다는 뜻이다. `RestAuthenticationEntryPoint`(401)와 `RestAccessDeniedHandler`(403)를 붙여 갈랐다.

**리프레시를 넣게 되면** — `TokenResponse` 스키마는 그대로 두고 `accessOnly(...)` 대신 두 토큰을 채우면 된다. 응답 형태가 바뀌지 않으므로 클라이언트 변경이 필요 없다. 액세스 토큰 만료를 짧게(예: 30분) 줄이는 것이 함께 따라온다.
