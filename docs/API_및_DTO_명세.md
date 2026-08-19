# API 및 DTO 명세

> 원본은 `src/main/java/.../dto/` 의 record들이다. **불일치가 생기면 코드가 맞다.**
> 스키마 대응은 [데이터베이스_설계.md](데이터베이스_설계.md)를 참고한다.

---

## 1. 공통 규약

### 1.1 인증

- `/api/auth/**`, `/actuator/health`, 그리고 이야기 정적 에셋 `/stories/**`(장면/미션 이미지)만 인증 없이 접근한다. 나머지는 전부 Bearer 토큰이 필요하다.
- **보호자 식별자는 요청에 담지 않는다.** `@CurrentParentId`가 JWT에서 꺼내 주입한다. 아래 표의 요청 필드에 `parentId`가 없는 이유다.
- 아이·세션 리소스는 컨트롤러 진입 시 **소유권을 검증**한다. 남의 아이면 403.

### 1.2 오류 응답

모든 오류는 형태가 같다.

```json
{ "code": "CONSENT_REQUIRED", "message": "유효한 아동 동의가 필요합니다." }
```

| 상황 | 상태 | `code` |
|---|---|---|
| 검증 실패(`@Valid`) | 400 | `INVALID_REQUEST` — message는 `필드명: 사유`. `INVALID_IDEMPOTENCY_KEY`(키 64자 초과), `INVALID_PASSWORD_RESET_TOKEN`(재설정 링크 무효/만료) |
| 토큰 없음·위조·**만료** | 401 | `UNAUTHORIZED`. 인증 시도 실패는 세분 코드: `INVALID_CREDENTIALS`(로그인), `INVALID_REFRESH_TOKEN`(재발급), `KAKAO_AUTH_FAILED`/`GOOGLE_AUTH_FAILED`(소셜) |
| 남의 리소스 | 403 | `FORBIDDEN` |
| 없는 리소스 | 404 | `NOT_FOUND` |
| 상태 충돌 | 409 | 아래 목록 |
| 값이 규칙에 안 맞음 | 422 | `STT_EMPTY_TEXT`, `GRID_OUT_OF_RANGE`, `INVALID_WORD` (2026-08-16 추가) |
| 로그인 시도 초과 잠금 | 423 | `ACCOUNT_LOCKED` |
| 업로드 한도 초과 | 413 | `AUDIO_TOO_LARGE` (2026-08-16 추가. 기존 500) |
| AI 벤더 오류 | **502** | `AI_UPSTREAM_ERROR` — 벤더가 4xx/5xx를 돌려줌(429 제외) |
| AI 쿼터 소진·연결 실패 | **503** | `AI_RATE_LIMITED`(벤더 429), `AI_UNAVAILABLE`(연결 실패/타임아웃). 재시도 안내 대상 |
| 메일 발송 실패 | 503 | `EMAIL_DELIVERY_FAILED` |
| 미구현 스텁 | **501** | `NOT_IMPLEMENTED` |
| 그 외 | 500 | `INTERNAL_ERROR` |

409 코드: `CONSENT_REQUIRED` `SESSION_NOT_IN_PROGRESS` `SCENE_NOT_STORY` `SCENE_NOT_DIALOGUE` `REPORT_NOT_READY` `DUPLICATE_WORD` `DUPLICATE_EMAIL` `CELL_OCCUPIED` `ITEM_ALREADY_PLACED` `ITEM_LOCKED` `STARDUST_INSUFFICIENT` `MAX_TURNS_EXCEEDED` `MISSION_NOT_EXPOSED` `RETELLING_BEFORE_ORDER` `CONCURRENT_TURN` `REQUEST_IN_PROGRESS`

**멱등키(2026-08 확정)** — 발화 제출과 아이템 구매는 `Idempotency-Key` 헤더(선택, UUID 권장, 64자 이하)를 받는다. 클라이언트가 작업마다 새로 만들고 **재시도 사이에만 유지**한다. 같은 키의 재전송은 완료된 요청이면 저장된 응답을 그대로 재생하고, 처리 중이면 409 `REQUEST_IN_PROGRESS`를 돌려준다. 키가 없으면 기존 동작 그대로다. 기록은 24시간 보관 후 청소된다.

**501을 쓰는 이유** — 컨트롤러 골격만 있고 로직이 없는 엔드포인트가 200에 빈 본문을 돌려주면 프론트가 구현된 것으로 오해한다. 명시적으로 알린다.

**401과 403은 반드시 갈라 처리한다.** 클라이언트는 **401을 받으면 `/refresh`로 재발급을 시도**하고, 리프레시까지 만료(`INVALID_REFRESH_TOKEN`)면 로그인 화면으로 보낸다. 403은 그냥 오류로 표시하면 된다. 스프링 시큐리티 기본값은 둘 다 403 + 빈 본문이라 `RestAuthenticationEntryPoint`·`RestAccessDeniedHandler`로 갈라 두었고, 두 응답 모두 위의 `{code, message}` 형태를 지킨다.

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
| POST | `/password-reset/request` | 비밀번호 재설정 메일을 보낸다. 계정 존재 여부와 무관하게 202 | `PasswordResetRequest` | 202 (본문 없음) | ✅ |
| POST | `/password-reset/confirm` | 메일의 토큰으로 새 비밀번호를 확정한다. 토큰은 1회용 | `PasswordResetConfirmRequest` | 204 (본문 없음) | ✅ |
| POST | `/find-email` | 이름/아이 정보 대조로 이메일을 찾는다. 매치가 없어도 200과 빈 배열 | `FindEmailRequest` | 200 `FindEmailResponse` | ✅ |

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

**수행 결과 제출은 별도 API가 아니다.** 아이가 미션에 대해 말한 발화를 발화 제출
(`POST /utterances`)에 `missionId`를 실어 보내면 서버가 완료 표시와 분석을 함께
처리한다(이야기_전개_가이드.md 3.5). 한때 계약만 있던 `POST /{missionId}/result`는
2026-08-15에 제거했다 - 구현도 호출처도 없었다.

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
| POST | `/api/sessions/{sessionId}/report` | 세션의 대화와 분석을 집계해 리포트를 생성한다 | — | 201 `ReportDetailResponse` | ✅ |

조회 2건은 저장된 리포트를 읽는다. 생성은 `ReportService.generateNow`가 세션의 대화와
분석을 모아 LLM으로 요약한다.

**대표 발화는 조회할 때마다 만든다.** `utterance_analyses`의 근거에서 구성하며 요소당 가장
이른 턴 하나만 남긴다. `sttLowConfidence=true`인 발화는 후보에서 빠진다 - 저장된 원문이 아이가
실제로 한 말과 다를 수 있는데, 리포트는 보호자에게 "아이가 이렇게 말했다"고 보여주는 자리다.

### 2.12 단어장

| 메서드 | 경로 | 설명 | 요청 | 응답 | 상태 |
|---|---|---|---|---|---|
| POST | `/api/children/{childId}/words` | 모르는 단어를 저장한다. 아이 눈높이의 뜻은 LLM이 만든다 | `WordCreateRequest` | 201 `WordResponse` | ✅ |
| GET | `/api/children/{childId}/words` | 단어 목록을 조회한다 | `?entryType=` (선택) | `List<WordResponse>` | ✅ |
| PATCH | `/api/children/{childId}/words/{wordId}/favorite` | 즐겨찾기를 켜고 끈다 | — | `WordResponse` | ✅ |
| DELETE | `/api/children/{childId}/words/{wordId}` | 저장한 단어를 삭제한다 (2026-08 경로 확정) | — | 204 | ✅ |
| POST | `/api/children/{childId}/words/{wordId}/sentence-practice` | 예문 따라 말하기를 제출한다. 일치율 90% 이상이면 별가루 2개 | `SentencePracticeRequest` | `SentencePracticeResponse` | ✅ |

아이가 이야기를 듣다 모르는 단어를 누르는 경로에서는 뜻이 올 수 없어 LLM을 타므로, 벤더 선정
전까지 그 경로만 501이다. 클라이언트가 뜻을 담아 보내면 지금도 저장된다.

즐겨찾기와 삭제는 단어가 그 아이의 것인지까지 확인한다. 아이가 둘인 보호자가 `childId`에 다른
아이를 넣어 형제의 단어를 건드리지 못하게 하기 위해서이고, 없는 자원과 같이 404로 알린다.

예문 따라 말하기(2026-08-16)는 학습 -> 보상(별가루) -> 놀이(행성 꾸미기) -> 학습 사이클의
단어장 쪽 두 번째 학습이다. 예문 3종(이야기/일상/심화) 중 하나를 골라 그대로 따라 말하면,
클라이언트가 `/api/stt`로 인식한 텍스트를 보내고 서버가 문자 일치율을 채점한다.
음성 인식 실패(빈 텍스트)는 `/api/stt`가 422 `STT_EMPTY_TEXT`로 알리므로 이 API까지 오지
않는다. 3.11의 `SentencePracticeResponse` 참고.

### 고객센터/공지/이용안내/알림 (2026-08-16 추가, #84 + 문의 수정/삭제)

| 메서드 | 경로 | 설명 | 상태 |
|---|---|---|---|
| GET | `/api/notices` | 공개 공지 목록 (고정 먼저, 최신순) | ✅ |
| GET | `/api/notices/{id}` | 공지 상세 (조회수 증가) | ✅ |
| GET | `/api/guides` | 이용안내 목록 (`?category=` 선택) | ✅ |
| GET | `/api/guides/{id}` | 이용안내 상세 | ✅ |
| POST | `/api/inquiries` | 문의 작성 (`CreateInquiryRequest`) | ✅ |
| GET | `/api/inquiries` | 내 문의 목록 (답변 여부 포함) | ✅ |
| GET | `/api/inquiries/{id}` | 문의 상세 (답변 포함. 남의 것은 404) | ✅ |
| PATCH | `/api/inquiries/{id}` | 문의 수정 - **답변 전(PENDING)만**. 이후 409 `INQUIRY_ALREADY_ANSWERED` | ✅ |
| DELETE | `/api/inquiries/{id}` | 문의 삭제 - 답변 전만 (204) | ✅ |
| GET | `/api/notifications` | 알림 목록 | ✅ |
| GET | `/api/notifications/unread-count` | 안 읽은 알림 수 | ✅ |
| PATCH | `/api/notifications/{id}/read` | 읽음 처리 | ✅ |
| POST | `/api/notifications/read-all` | 전체 읽음 | ✅ |
| POST | `/api/notifications/devices` | FCM 기기 토큰 등록 | ✅ |

공지/이용안내의 생성·수정·삭제와 문의 답변은 **관리자 콘솔**(admin-goodquestion-backend,
같은 DB의 같은 테이블)이 담당한다. 답변이 등록되면 사용자 알림이 함께 생성된다.
답변이 달린 문의의 내용이 바뀌면 답변이 무엇에 대한 것인지 어긋나므로, 사용자 수정/삭제는
PENDING 상태에서만 허용한다.

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

### 2.17 후속 자유 대화 (2026-08-18 추가)

이야기를 완주한 뒤 그 이야기의 인물과 이어서 하는 대화다. **학습이 아니라 관계**라서
요소 판정·유도·별가루·리포트가 전부 없고, 세션(`/api/sessions`)과는 표도 경로도 갈라져 있다.

| 메서드 | 경로 | 설명 | 요청 | 응답 | 상태 |
|---|---|---|---|---|---|
| GET | `/api/children/{childId}/stories/{storyId}/free-talk/characters` | 완주한 이야기의 대화 가능 인물 | — | `List<FreeTalkCharacterResponse>` | ✅ |
| POST | `/api/children/{childId}/free-talk` | 대화를 열고 첫 인사를 받는다 (201) | `FreeTalkStartRequest` | `FreeTalkStartResponse` | ✅ |
| POST | `/api/free-talk/{freeTalkId}/messages` | 아이의 말을 제출한다. `Idempotency-Key` 헤더(선택) | `FreeTalkMessageRequest` | `FreeTalkTurnResponse` | ✅ |
| POST | `/api/free-talk/{freeTalkId}/end` | 아이가 먼저 그만둔다. 캐릭터가 작별 인사를 남긴다 | — | `FreeTalkEndResponse` | ✅ |
| POST | `/api/free-talk/{freeTalkId}/leave` | 인사 없이 그냥 나간다. 대화만 닫는다 (204) | — | — | ✅ |

음성 인식은 기존 `POST /api/stt`를 그대로 쓴다. **아이 음성 원본은 저장하지 않는다.**

**충돌·거절**

| 상황 | 코드 | 의미 |
|---|---|---|
| 완주하지 않은 이야기 | 404 `STORY_NOT_COMPLETED` | 있음을 알리지 않는다 — 403으로 답하면 아직 안 읽은 이야기의 인물 구성이 드러난다 |
| 이미 끝난 대화에 발화 | 409 `FREE_TALK_ENDED` | 10턴을 채웠거나 아이가 먼저 그만둔 대화다 |
| 같은 대화에 턴이 겹쳤다 | 409 `CONCURRENT_TURN` | 학습 대화와 같은 판정. 앞선 요청이 끝난 뒤 다시 보낸다 |
| 남의 대화 | 403 `FORBIDDEN` | 대화를 만든 보호자만 이어갈 수 있다 |

**턴 상한** — 아이가 10번 말하면 캐릭터가 스스로 인사하고 닫는다(`ended: true`).
남은 턴은 화면에 표시하지 않는다. `maxTurns`는 클라이언트가 길이를 가늠하라고 주는 값이지
카운터를 그리라는 뜻이 아니다.

`POST /end`는 **이미 닫힌 대화에도 200**으로 응답하며 남아 있는 마지막 대사를 그대로
돌려준다. 끝난 대화에 "그만하기"가 한 번 더 들어오는 것은 흔한 일이고, 그때마다 대사를
새로 만들면 요금만 두 배가 된다.

**끝내는 길이 둘이다 — `/end`와 `/leave`.** 화면의 "마무리하기"가 `/end`, "나가기"가
`/leave`다. 갈라 둔 이유는 하나뿐이다 — `/end`는 작별 대사를 LLM으로 만들고 TTS로 읽어
주므로, 나가려는 아이가 낭독이 끝날 때까지 붙잡힌다. `/leave`는 **LLM도 TTS도 부르지
않고** `ended_at`만 채운다. 대사가 새로 생기지 않으므로 남는 마지막 대사는 나가기 직전의
그 대사다.

`POST /leave`는 **응답 본문이 없고 204**다. 이미 닫힌 대화에 다시 들어와도 **204**이며
`FREE_TALK_ENDED`(409)를 돌려주지 않는다 — 아이는 이 응답을 기다리지 않고 이미 화면을
떠난 뒤라 실패를 보여 줄 곳이 없고, 나가기가 거절로 막히면 그것 자체가 결함이다.
클라이언트도 응답을 기다리지 말고 바로 홈으로 나가면 된다.

---

## 3. DTO 상세

각 DTO 아래의 **사용처**가 그 DTO를 주고받는 엔드포인트다. `X에 중첩`은 단독 응답이 아니라 다른 DTO의 필드로만 실려 나간다는 뜻이고, 그때는 최종적으로 어느 엔드포인트가 전달하는지도 함께 적었다.

여기에 없는 엔드포인트는 **요청·응답 본문이 아예 없는 셋**뿐이다 — `POST /api/sessions/{sessionId}/stop`(200, 빈 본문), `DELETE /api/children/{childId}/words/{wordId}`(204), `POST /api/free-talk/{freeTalkId}/leave`(204).

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
| `accessTokenExpiresIn` | long | 액세스 토큰 유효 기간(초). 기본 30분(1800). 만료 시 리프레시 토큰(기본 14일, 1회 사용 회전)으로 재발급한다 |

#### `PasswordResetRequest` / `PasswordResetConfirmRequest`

> **사용처** — `POST /api/auth/password-reset/request` / `POST /api/auth/password-reset/confirm` 요청

- `PasswordResetRequest`: `email`(`@NotBlank @Email`, 255자 이하)
- `PasswordResetConfirmRequest`: `token`(`@NotBlank`, 메일 링크의 1회용 토큰) · `newPassword`(`@NotBlank`, 8~64자)

계정 존재 여부를 응답으로 구분하지 않는다 — 요청은 항상 202, 무효/만료 토큰은 400 `INVALID_PASSWORD_RESET_TOKEN`, 메일 발송 실패만 503.

#### `FindEmailRequest` / `FindEmailResponse`

> **사용처** — `POST /api/auth/find-email` 요청 / 응답

- `FindEmailRequest`: `parentName`(`@NotBlank`) · `childName`(선택) · `childBirthYear`(선택)
- `FindEmailResponse`: `emails`(`List<String>`, 마스킹된 이메일 목록)

매치가 없어도 200과 빈 배열이다 — 존재 여부를 에러로 구분하지 않는다.

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
| `recommendedStories` | `List<StoryCardResponse>` | 현재는 PUBLISHED 상위 3개(`display_order` 오름차순, 같으면 `created_at` 내림차순) |
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
| `stories` | `List<StoryCardResponse>` | `?topic=` 필터 적용 결과. `display_order` 오름차순, 같으면 `created_at` 내림차순 |
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
| `narrationAudioUrl` | String | STORY만. 내레이션 사전 렌더 음성. null이면 음성 없이 진행하거나 `/api/tts`로 합성한다 (2026-08-16 추가) |
| `narrationTimings` | `List<{index, start, end}>` | 문장별 실측 시작/끝(초). `narrationAudioUrl`이 있을 때만 값이 있다. index는 `narrationSentences` 순서와 같다 (2026-08-16 추가) |
| `imageUrl` | String | 장면 배경 이미지 |
| `videoUrl` | String | 장면 배경 영상. null이거나 재생 실패면 `imageUrl`로 폴백한다. 반복 여부는 `sceneType`이 정한다 - STORY는 낭독 종료 시 멈추고 DIALOGUE는 반복 (2026-08-16 추가) |
| `characterName` | String | DIALOGUE만 |
| `maxTurns` | Short | DIALOGUE만 — 남은 턴 UI |

**내레이션 분리는 서버가 한다.** 줄바꿈 기준으로 자른다 — 마침표로 자르면 `1.5km` 같은 표현이 깨진다.

**자막 넘김은 `narrationTimings`를 쓴다.** 이게 없으면 글자수 비례 추정밖에 없는데,
문장 길이와 실제 낭독 길이가 어긋나 자막이 소리보다 먼저/늦게 넘어간다.

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
| `newElements` | `List<ThinkingElement>` | 이번 발화에서 **새로** 인정된 요소. 표정/연출 트리거용 (2026-08-16 추가) |
| `turnCount` `maxTurns` | int | 현재 장면에서 아이가 말한 횟수와 그 장면의 최대 대화 범위 |
| `guidanceTarget` | `ThinkingElement` | GUIDED일 때만 |

#### `SessionResumeResponse`

> **사용처** — `GET /api/sessions/{sessionId}/resume` 응답

`session`(`SessionResponse`) · `currentScene`(`SceneContentResponse`) · `messages`(`List<MessageResponse>`) · `lastCharacterMessage`(`CharacterMessageResponse`) · `exposedMission`(`MissionResponse`)

`messages`는 세션 전체 내역이고 `lastCharacterMessage`는 마지막 캐릭터 발화다. `exposedMission`에는 노출 중이던 미션이 실제로 담긴다 - 미노출이면 null이고, 완료 여부는 구분하지 않는다.

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

`audioUrl`이 null이면 클라이언트가 `/api/tts`를 호출한다. **고정 대사(첫/마지막 대사)는
사전 렌더 음성이 실제로 내려간다(2026-08-16부터).** 서버가 지금 내보내는 문장의
SHA-256을 `scene_audio.text_hash`와 대조해 맞을 때만 채우므로, LLM이 만든 대사와
아이 이름이 치환된 문장은 자동으로 null이 되어 기존 합성 경로를 탄다. 클라이언트
로직은 그대로다 - `audioUrl ?? 합성` 분기면 충분하다.

#### `SceneOpeningResponse`

> **사용처** — `POST /api/sessions/{sessionId}/scenes/current/opening` 응답

`message`(`CharacterMessageResponse`) · `alreadyOpened`(boolean)

**멱등이다.** 재호출하면 새 메시지를 만들지 않고 `alreadyOpened=true`로 알린다.

#### `SceneAdvanceResponse`

> **사용처** — `POST /api/sessions/{sessionId}/scenes/current/story-complete` 응답

`phase`(`PlayPhase`) · `currentScene`(`SceneContentResponse`) · `openingMessage`(`CharacterMessageResponse`)

다음 장면이 DIALOGUE면 고정 첫 대사를 함께 저장·반환한다. 마지막 장면이 STORY로 끝났다면 `phase=POST_ACTIVITY`이고 `currentScene`은 null이다. 후속 활동 config가 없는 이야기는 건너뛰고 즉시 완료되어 `phase=ENDED`다(2026-08 확정, 완주 별가루 포함).

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
| `sttRawText` | String | | STT 최초 변환 텍스트. `/api/stt` 응답의 `rawText`(벤더 원문)를 되올린다 |
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
| `meaning` | String | | **없으면 이야기 어휘 사전 -> LLM 순서로 서버가 채운다** |
| `exampleSentence` | String | | 이야기 예문(아이가 단어를 만난 문장). 있으면 사전/생성 예문보다 우선 |

**예문은 항상 3종으로 저장된다**(2026-08-16, V14): 1) 이야기 예문(`exampleSentence`)
2) 일상 예문(`exampleSentenceDaily`) 3) 심화 예문(`exampleSentenceAdvanced`, 일상보다
한 단계 어려움). 요청은 이야기 예문 하나만 보낼 수 있고, 나머지는 어휘 사전 또는
LLM 생성으로 서버가 채운다. 뜻을 직접 보낸 저장도 예문을 채우기 위해 생성이 돌 수
있다(이때 모델 판정으로 저장을 거절하지는 않는다).

**서버는 `word`를 표제어로 정규화해 저장한다** (형태소 분석 Nori, 2026-08).
"기왓장이"를 보내면 "기왓장"으로 저장되고 응답의 `word`도 표제어다. 조사를
뗄 수 없는 낱말(비명사, 미등재어)은 보낸 그대로 저장된다.

같은 아이가 같은 **표제어**를 또 저장하면 409 `DUPLICATE_WORD` - "기왓장이"를
담은 뒤 "기왓장을"을 보내도 409다. 이때 뜻 생성은 일어나지 않는다.

뜻이 없을 때의 채움 순서: 1) 이야기 어휘 사전(`story_vocabulary`, 검수된 뜻,
LLM 없음) 2) 사전에 없으면 LLM 생성. 고정 대사에서 담는 단어는 대부분 1)에서
끝난다. → 프론트 `docs/단어_저장_비용_속도_설계_조사.md`

LLM 생성 단계에서 **실제 쓰이는 낱말이 아니라고 판정되면 422 `INVALID_WORD`**
로 저장을 거절한다(2026-08-16). 판정은 **애매하면 통과 쪽으로 기운다** - 실재 낱말을
막으면 아이가 궁금해한 말을 담지 못해 되돌릴 방법이 없다.
→ `docs/트러블슈팅_단어_뜻_생성_품질.md` STT 오인식이 만든 존재하지 않는 말이 단어장에
남는 것을 막는 관문으로, 동적(LLM 생성) 대사에서 단어를 담는 경로의 전제
조건이다. 요청에 뜻을 담아 보내는 경로와 어휘 사전 히트는 이 관문을 타지
않는다.

#### `WordResponse`

> **사용처** — `POST /api/children/{childId}/words` · `GET /api/children/{childId}/words` · `PATCH /api/children/{childId}/words/{wordId}/favorite` 응답

`id` · `word` · `meaning` · `exampleSentence`(이야기 예문) · `exampleSentenceDaily`(일상 예문) · `exampleSentenceAdvanced`(심화 예문) · `entryType` · `sourceSceneId` · `storyId` · `storyTitle` · `storyImageUrl` · `createdAt`

V14 이전에 저장된 단어는 일상/심화 예문이 null이다.

이야기 3필드는 단어장 화면이 단어를 **이야기별로 묶어** 보여주기 때문에 담는다. 장면 조회가
`GET /api/stories/{storyId}/scenes` 뿐이라 `sourceSceneId`만으로는 클라이언트가 이야기를 되짚을
수 없다. 이름은 이야기를 참조하는 다른 DTO(`ShopItemResponse.UnlockGuide` · `ReportListResponse`)와
맞췄다. 장면 없이 저장된 단어는 세 값이 모두 null이다.

목록은 평면 유지다 — 그룹핑은 클라이언트가 한다.

#### `SentencePracticeRequest`

> **사용처** — `POST /api/children/{childId}/words/{wordId}/sentence-practice` 요청

| 필드 | 타입 | 검증 | 설명 |
|---|---|---|---|
| `sentenceType` | `ExampleSentenceType` | `@NotNull` | `STORY` / `DAILY` / `ADVANCED` |
| `spokenText` | String | `@NotBlank @Size(max=500)` | `/api/stt`가 돌려준 인식 텍스트 |

목표 문장은 서버가 단어에서 꺼낸다 - 문장을 클라이언트가 보내면 쉬운 문장으로
바꿔치기해 보상을 딸 수 있다. 음성 자체는 보내지도 저장하지도 않는다.

#### `SentencePracticeResponse`

> **사용처** — `POST /api/children/{childId}/words/{wordId}/sentence-practice` 응답

| 필드 | 타입 | 설명 |
|---|---|---|
| `matched` | boolean | 일치율이 기준(0.90)을 넘었는지 |
| `similarity` | BigDecimal | 채점된 일치율(0.00~1.00, 소수 둘째 자리) |
| `targetSentence` | String | 채점 기준이 된 예문. 화면이 어디가 달랐는지 비교해 보여 줄 수 있게 |
| `rewarded` | boolean | 별가루가 지급됐는지 |
| `skipReason` | enum | 성공인데 지급이 없을 때만. `ALREADY_REWARDED` / `DAILY_LIMIT` |
| `stardustAmount` | int | 이번에 지급된 별가루 수. 지급이 없으면 0 |
| `stardustBalance` | int | 지급 반영 후 잔액 |

화면이 갈라야 하는 세 갈래를 그대로 담는다:

1) `matched=false` - 일치율 미달. 일치율과 목표 문장을 보여 주며 재도전을 권한다
2) `matched=true, rewarded=false` - 성공인데 지급 없음. `skipReason`으로 이유를 알린다
3) `matched=true, rewarded=true` - 성공에 지급. 별가루 연출과 잔액 갱신

채점은 문자 단위 일치율이다. 목표 예문과 발화 텍스트를 글자/숫자만 남겨 정규화한 뒤
편집 거리를 긴 쪽 길이로 나눈다 - 띄어쓰기와 문장부호는 STT 표기 차이일 뿐이라
점수에서 뺀다. 같은 발화는 항상 같은 점수를 받는 결정적 채점이라 LLM은 쓰지 않는다.

지급 규칙: 일치율 90% 이상 · 예문(단어 x 유형)당 최초 1회 · 2개 · 하루 최대 2건
(자정 기준 Asia/Seoul). 하루 4개는 완주 최대치(3+2)를 넘지 않는다. 보상이 나간
연습만 `sentence_practices`에 남는다(V15) - 상한에 걸린 날의 성공은 기록하지 않아
그 예문은 내일 다시 성공하면 보상받는다.

오류: 단어가 없거나 남의 아이 것이면 404 `NOT_FOUND`, V14 이전 단어의 빠진 예문
유형이면 409 `EXAMPLE_SENTENCE_MISSING`, 검증 실패는 400 `INVALID_REQUEST`.

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

- `text`(String) — 인식 결과가 비면 422 `STT_EMPTY_TEXT`. 저신뢰 턴(lowConfidence)에
  한해 이야기 어휘 근접 오인식 교정("방비" -> "방귀")이 끝난 값이다. 화면 표시와
  발화 제출(`utterance`) 모두 이 값을 쓴다. 한글이 전혀 없는 결과(무음에서 영어
  상투구를 뱉는 환각)도 422 `STT_EMPTY_TEXT`
- `rawText`(String) — 벤더가 돌려준 원문. 교정이 틀렸을 때 무엇이 실제로 인식됐는지
  추적하는 유일한 근거다. 클라이언트는 발화 제출의 `sttRawText`에 text가 아니라
  **이 값을** 되올린다 - text를 되올리면 원문이 유실된다
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

### 3.14 후속 자유 대화

#### `FreeTalkCharacterResponse`

> **사용처** — `GET /api/children/{childId}/stories/{storyId}/free-talk/characters` 응답 ·
> `FreeTalkStartResponse`에 중첩

- `characterId`(UUID) · `name`(String) — 대화를 시작할 때 되올리는 값과 화면 표시 이름
- `characterKey`(String) — 표정 이미지 파일명의 키. 클라이언트가
  `{characterKey}_{expression}.png`로 조립한다
- `thumbnailUrl`(String, nullable) — **지금은 항상 null이다.** 캐릭터 이미지가 클라이언트
  자산이라 서버가 가진 것이 없다. `characters`에 이미지 컬럼이 생기면 그때 채운다
- `lastTalkedAt`(OffsetDateTime, nullable) — 이 인물과 마지막으로 이야기한 시각. 없으면 null

#### `FreeTalkStartRequest` / `FreeTalkStartResponse`

> **사용처** — `POST /api/children/{childId}/free-talk` 요청 / 응답

| | 필드 | 설명 |
|---|---|---|
| 요청 | `storyId`(`@NotNull`) · `characterId`(`@NotNull`) | 인물이 그 이야기 소속이 아니면 404 |
| 응답 | `freeTalkId` · `character` · `opening` · `maxTurns` | `opening`은 캐릭터가 먼저 건네는 인사다 |

#### `FreeTalkMessageRequest` / `FreeTalkTurnResponse`

> **사용처** — `POST /api/free-talk/{freeTalkId}/messages` 요청 / 응답

| | 필드 | 설명 |
|---|---|---|
| 요청 | `text`(`@NotBlank`) | STT 결과 텍스트. 신뢰도·원문은 받지 않는다 — 리포트에 쓰이지 않아 남길 이유가 없다 |
| 응답 | `characterMessage` · `turnCount` · `ended` | `ended: true`면 이 대사가 마지막이다 |

#### `FreeTalkEndResponse`

> **사용처** — `POST /api/free-talk/{freeTalkId}/end` 응답

`closing`(`FreeTalkLineResponse`) — 캐릭터의 작별 대사

#### `FreeTalkLineResponse`

> **사용처** — `FreeTalkStartResponse.opening` · `FreeTalkTurnResponse.characterMessage` ·
> `FreeTalkEndResponse.closing`에 중첩

- `text`(String) — 캐릭터 대사
- `audioUrl`(String, nullable) — 서버가 미리 합성해 둔 음성. **null이면 합성에 실패한
  것이고 클라이언트가 `POST /api/tts`로 직접 만든다** — 목소리 하나 때문에 대화를
  끊지 않는다
- `emotion`(CharacterEmotion, nullable) — 표정 전환용. 6종(`NEUTRAL` `HAPPY` `SAD`
  `WORRIED` `SURPRISED` `RELIEVED`)

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
| 4 | **`SafetyResponse` 감지 로직** | 계약 자리만 확정. 항상 null | AI 파이프라인 연동 시 |
| 5 | **`CharacterEmotion` 고정 6종** | 응답 enum이 고정인데 DB는 CHECK를 풀었다 | 캐릭터별 `expression_keys`로 옮기면 문자열 키 + fallback으로 바꾼다 |
| 7 | **`SynthesisRequest.characterName`이 이름 문자열** | `characters` 테이블이 생겼으니 키로 지정하는 편이 안전 | `characterKey` 또는 `sceneId`+`slot`으로 전환 검토 |
| 8 | **아이템 발판(footprint)** | 카탈로그 정의가 없어 모든 아이템을 1칸으로 보고 배치 검증을 한다 | 2x2 아이템의 비앵커 칸이 겹칠 수 있다. 카탈로그가 나오면 `PlanetService`의 빈 칸 검사에 점유 칸 계산을 더한다 |
| 미결-01 | **STT/TTS 벤더 확정** | OpenAI 실측 구성(gpt-4o-mini)으로 동작 중. 비교용이지 최종 선정이 아니다 | 아동 실녹음 인식률 검증 후 최종 확정. 신뢰도 컷(0.5)도 그때 함께 보정 |
| 미결-02 | **네이버 소셜 로그인** | kakao/google만 지원, 그 외 501 | 도입 여부 결정 |

(결번 1, 2, 3, 6은 해소된 항목이다: 리프레시 토큰, 멀티파트 한도, STT 신뢰도 기준값, 단어 삭제 경로)

---

## 7. 구현 현황 요약

| 영역 | 엔드포인트 | ✅ | ⚠️ | ⛔ |
|---|---|---|---|---|
| 인증 | 8 | 7 | 1 | 0 |
| 보호자 | 2 | 1 | 1 | 0 |
| 아이·동의 | 8 | 8 | 0 | 0 |
| 홈 | 1 | 1 | 0 | 0 |
| 콘텐츠 | 4 | 4 | 0 | 0 |
| 세션·장면 | 8 | 8 | 0 | 0 |
| 대화·미션 | 3 | 3 | 0 | 0 |
| 후속 활동 | 4 | 4 | 0 | 0 |
| 리포트 | 3 | 3 | 0 | 0 |
| 단어장 | 4 | 4 | 0 | 0 |
| 보상 | 11 | 11 | 0 | 0 |
| 음성 | 2 | 2 | 0 | 0 |
| **합계** | **58** | **56** | **2** | **0** |

⛔는 0건이다. 명세에 있는 엔드포인트는 전부 동작한다.

⚠️ 2건의 내용은 이렇다. 소셜 로그인은 카카오와 구글만, 내 정보 수정은 이름만 동작.

**2026-08-16 갱신분 3** (집계 변동 없음 - 응답 필드 추가)

- `SceneContentResponse`에 `videoUrl` 추가(V17, `story_scenes.video_url`). 방귀 뀌는
  며느리 장면 1/2/3/4/5/6/8에 영상이 있고 7/9는 null이다. 영상은 이미지를 대체하지
  않고 얹는다 - null이거나 재생 실패면 `imageUrl` 폴백. 이 DTO를 세션 시작/이어하기/
  장면 전환/현재 장면 조회/장면 목록이 공유하므로 전 응답에 실린다. 영상 파일은
  기존 이미지와 같은 정적 경로로 서빙한다(인증 불필요)

**2026-08-16 갱신분 2** (집계 변동 없음 - STT 응답 필드 추가와 판정 강화)

- `TranscriptionResponse`에 `rawText`(벤더 원문) 추가. `text`는 저신뢰 턴에 한해
  이야기 어휘 근접 오인식 교정("방비" -> "방귀", 자모 편집거리)이 끝난 값이다.
  또렷한 발화(고신뢰)는 교정하지 않는다 - 거리 상한 안에 일상어("방금", "바뀌-")가
  잡히는 오교정이 실측돼, 교정 대상을 저신뢰 턴으로 좁혔다. 프론트는 발화 제출의
  `sttRawText`에 `rawText`를 되올린다(프론트 main은 이미 rawText 파싱과 되올림을
  구현했고, 서버가 안 주면 text로 폴백한다). 3.13 참고
- 한글이 전혀 없는 결과는 422 `STT_EMPTY_TEXT`. 무음에서 모델이 영어 상투구
  ("Thank you for watching")를 뱉는 환각이 어휘 에코 판정과 저신뢰 컷을 모두
  빠져나가는 구멍을 막았다
- 교정 후 텍스트로 어휘 에코 판정을 한 번 더 돈다. 뭉개진 에코가 원문 기준 판정을
  통과한 뒤 교정으로 정확한 힌트 단어가 되는 재발 경로 차단

**2026-08-16 갱신분** (집계 변동 없음 - 응답 필드 추가와 값 채워짐)

- 고객센터/공지/이용안내/알림 사용자 API(#84)가 명세에 빠져 있어 보충했다.
  문의 수정/삭제(PATCH/DELETE, 답변 전만)를 새로 추가했다 - 409
  `INQUIRY_ALREADY_ANSWERED`. 공지/이용안내 출시 콘텐츠는 R__5 시드

- 단어 예문이 3종(이야기/일상/심화)으로 늘었다(V14). `WordResponse`에
  `exampleSentenceDaily`/`exampleSentenceAdvanced` 추가. 이야기 어휘 사전
  9단어도 3종 예문으로 확장
- 단어 저장이 표제어로 정규화된다(Nori 형태소 분석, V13). "기왓장이" ->
  "기왓장"으로 저장되고 중복(409)도 표제어 기준이다. 뜻이 없으면 이야기 어휘
  사전(`story_vocabulary`, 검수된 뜻) -> LLM 순서로 채워 고정 대사 단어는
  대부분 LLM 호출 없이 저장된다. LLM이 실제 낱말이 아니라고 판정하면 422
  `INVALID_WORD`로 거절한다(오인식 단어 차단). 3.11 참고

- 사전 렌더 장면 음성 연결(#69). `SceneContentResponse`에 `narrationAudioUrl`과
  `narrationTimings`(문장별 실측 시작/끝) 추가 - STORY 장면 내레이션이 실제 음성으로
  재생 가능해졌다. 프론트가 이 필드를 쓰기 전까지는 기존 글자수 타이머로 동작한다
- `characterMessage.audioUrl`이 고정 첫/마지막 대사에서 실제 값으로 내려간다.
  문장 SHA-256을 `scene_audio.text_hash`와 대조해 맞을 때만 채우므로 LLM 생성
  대사와 아이 이름 치환 문장은 자동으로 null(기존 합성 경로). 클라이언트 분기
  (`audioUrl ?? 합성`)는 변경 불필요
- 대화1 필수 요소: #45가 검수 의견으로 `REASON`을 제외했으나 **2026-08-15
  회의에서 REASON 포함(4종)으로 재확정되어 같은 날 복원했다.** 이야기 문서
  3절 표와 같다. `progress` 응답의 요소 목록에 REASON이 그대로 나타난다
- 대화 장면 종료: #68이 요소 충족 즉시 종료로 바꿨으나 **원 자료 요건(연동 기준
  10.3, 11절 - "필수 요소 충족 및 최소 대화량 충족"의 AND)에 따라 같은 날
  복원했다.** 요소를 다 채워도 preferred_turns(전 장면 2) 전이면 닫히지 않는다.
  충족조건 문서는 별도 작성분이라 원 자료와 어긋나는 부분이 있어, 원 자료를
  우선한다(대화1 REASON 복원과 같은 원칙)
- AI 실패 상태코드 세분화(#68): 벤더 429 -> 503 `AI_RATE_LIMITED`, 그 외 벤더
  오류 -> 502 `AI_UPSTREAM_ERROR`, 연결 실패 -> 503 `AI_UNAVAILABLE`, 업로드
  초과 -> 413 `AUDIO_TOO_LARGE`, 본문 파싱 실패 -> 400 `INVALID_REQUEST`.
  전부 500으로 나가던 것의 구분이다
- `ProgressResponse.newElements` 추가(#68): 이번 발화에서 새로 인정된 요소.
  표정 연출 트리거용

**2026-08-15 갱신분 3** (미션 결과 제출 제거로 분모 59 -> 58. ⛔ 0건)

- `POST /api/sessions/{sessionId}/missions/{missionId}/result` 계약 제거. 유일하게
  남아 있던 미구현(501)이었는데, 구현도 호출처도 없었다 - 프론트는 처음부터 발화
  제출(`POST /utterances` + `missionId`)로 미션을 냈고, 이야기_전개_가이드 3.5도
  "별도 API가 아니다, 쓰지 않는다"로 확정돼 있었다. 명세만 "구현 예정"으로 남아
  가이드와 모순되던 것을 해소했다
- 발화 경로가 검증(409 MISSION_NOT_EXPOSED, 404), 완료 표시, 분석, 캐릭터 응답을
  전부 처리하므로 기능 차이는 없다. 유일한 차이였던 질문별 구조화 저장은 소비처가
  없고(리포트 미사용, 질문별 표시 계획 없음 확인), `mission_results` 테이블과
  엔티티는 스키마 변경 리스크를 피해 휴면으로 남긴다
- `MISSION_ALREADY_SUBMITTED`(409)도 함께 제거 - 이 엔드포인트만 낼 수 있던 코드다

**2026-08-15 갱신분 2** (직전 집계는 54/3/2였다. 현황표가 실제 구현보다 뒤처져 있던 것을 정정)

| 엔드포인트 | 이전 | 현재 | 근거 |
|---|---|---|---|
| `POST /api/sessions/{sessionId}/report` | ⛔ | ✅ | `ReportService.generateNow`가 대화와 분석을 모아 LLM으로 요약한다. 구현은 2026-08-14에 들어왔는데 표에 반영되지 않았다 |
| `POST /api/children/{childId}/words` | ⚠️ | ✅ | `meaning`을 생략하면 `WordMeaningLlmClient`가 아이 눈높이의 뜻을 만든다. 단어-02 구현분이 표에 반영되지 않았다 |

남은 ⛔는 미션 결과 제출 1건이다. 코드로 확인한 결과 `MissionController.submitResult`만
`UnsupportedOperationException`을 던진다. ⚠️ 2건(소셜 로그인 공급자 범위, 비밀번호 변경)도
코드에서 확인했다 - `AuthController`의 provider 분기 default와 `ParentService`의 비밀번호
변경 경로가 각각 501이다.

**2026-08-15 갱신분** (집계 변동 없음 - API 계약도 그대로, 서버 동작만 바뀜)

- STT 어휘 힌트 에코 차단 확장: 무음이나 뭉개진 오디오에서 모델이 prompt의
  어휘 힌트를 복창하는 환각이 있는데, 기존 필터(완전 일치)를 재조합 문장이
  통과하는 것이 실측됐다. 판정을 나열 에코(힌트 단어를 지우면 아무것도 안
  남음)와 재조합 에코(힌트 어휘 등장 비율 2/3 이상)로 확장했다. 에코는 기존
  `STT_EMPTY_TEXT`(422) 경로를 탄다 - 응답 계약 변화 없음
- 벤더 프롬프트를 나열형에서 문장형으로 변경(`external.stt.prompt`, 서버 내부
  설정). 나열형이 복창 환각을 가장 잘 유발해서다. 문장형을 복창해도 재조합
  판정에 걸리는 것을 테스트로 고정했다
- 웹 녹음 상한 안내: 웹은 48kHz로 녹음하므로(샘플레이트 불일치 수정의 결과)
  109초부터 멀티파트 한도 10MB를 넘는다. 프론트가 60초에서 자동 종료한다
- 배경과 실측 전체는 `트러블슈팅_STT_어휘_힌트_에코.md` 참고

**2026-08-14 갱신분** (직전 집계는 48/3/5였다. 인증 3건 추가로 분모도 56 -> 59)

- 상태 변화: 리프레시 토큰 발급/회전/무효화 구현(refresh, logout 미구현 -> 동작),
  비밀번호 재설정 2건과 이메일 찾기 추가(전부 동작), 단어 삭제 구현(경로를
  `/api/children/{childId}/words/{wordId}`로 확정). 남은 501은 미션 결과 제출과
  리포트 생성 2건뿐이다
- 계약 추가: 발화 제출/아이템 구매의 `Idempotency-Key` 헤더(1장), `/api/stt` 응답의
  `confidence`/`lowConfidence`, 발화 응답의 `closingReaction`(최대 턴 종료)과
  `sceneTransition.resultImageUrl`(결과 연출), `sceneTransition.next`의 `COMPLETED`
  (후속 활동 무설정 이야기 즉시 완료), 인증 DTO 4종
- 확정값 반영: 미션1 질문 key safety -> reason, preferred_turns 전 장면 2,
  STT 저신뢰 기준 0.5, 아이템 가격 1/2/3과 누적 해금 3/4/5 인하
- 에러 표에 세분 코드 보강(401 계열 4종, 423, 503, 400 재설정 토큰), 8절을
  시점 기록으로 강등(현행은 액세스 30분 + 리프레시 14일)

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

근거 문서 6종(통합 명세서 - 저장소 밖 팀 공유 문서, 발화 분석 연동 기준, 대화 작동 규칙, 콘텐츠, 캐릭터 성격,
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

## 8. Access 토큰 단일 전략 — 동작 확인 (2026-08-10 시점 기록)

> **주의**: 이 절은 리프레시 토큰 도입 전의 검증 기록이다. 현재는 리프레시 발급/회전/무효화가
> 구현되어 액세스 30분 + 리프레시 14일로 운영한다(2.1절, 3.2절이 현행). 당시 검증한
> 무상태 구조(재로그인해도 진행 상태가 이어진다)는 지금도 유효하다.

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
