# 굿퀘스천 백엔드 — 세션 인수인계 (이영준 담당 파트)

> **주의(2026-08-14)**: 이 문서는 인계 시점의 스냅샷이라 현재 상태와 다른 내용이 많다
> (토큰 전략은 리프레시 도입으로, 소셜 로그인은 카카오/구글 서버 교환으로 확정됐고,
> 미완 항목 다수가 완료됨). 현재 상태는 [backend-dev-guide.md](backend-dev-guide.md)와
> [API_및_DTO_명세.md](API_및_DTO_명세.md)를 본다.

새 대화 세션에서 이 문서를 먼저 읽고 시작해 주세요. 이전 세션에서 무엇을 했고, 무엇이 남았는지 정리한 문서입니다.

---

## 0. 한 줄 요약

이영준 담당 API(`user`, `home`) 전부 구현 완료. 도중에 "Supabase Auth 미사용" 결정이 나와서 자체 인증(이메일/비밀번호 + 카카오, Access 토큰 단일)을 새로 만들어 붙였습니다. 로컬 빌드에서 `compileJava`는 성공 확인했고, `.env`의 `JWT_SECRET` 미설정으로 테스트가 실패했던 것까지 확인 후 해결법을 안내한 상태입니다. **DB 재생성, API 실사용 테스트, 팀 컨펌 2건은 진행 여부가 확인되지 않은 채로 세션이 종료되었으니 여기서부터 이어가면 됩니다.**

## 1. 담당 범위

- 담당자: 이영준
- 담당 도메인: `user`(parent, child, consent, auth) + `home`
- 원본 가이드: `docs/backend-dev-guide.md` (Google Docs 동기화본). **주의**: 이 문서는 Supabase Auth 사용을 전제로 작성돼 있어, 아래 3번 항목(인증 전환)과 내용이 어긋납니다. 인증 관련해서는 이 문서보다 아래 3번을 우선하세요.

## 2. 이번 세션에서 완료한 작업

### 2-1. 원래 TODO였던 API 3건 구현
| API | 파일 |
|---|---|
| `POST /children/{childId}/consents` | `user/consent/ConsentController.java`, `ConsentService.java` |
| `PATCH /children/{childId}/consents/{consentId}/withdraw` | 〃 |
| `GET /children/{childId}/home` | `home/HomeService.java` |

### 2-2. Supabase Auth 제거 → 자체 인증 신설
- 삭제: `global/security/SupabaseJwtFilter.java`, `SupabaseJwtVerifier.java`
- 신설: `user/auth/` 도메인 전체 (`AuthController`, `AuthService`, `AuthProvider`, `dto/*`, `kakao/KakaoClient`·`DefaultKakaoClient`·`KakaoProfile`), `global/security/JwtProvider.java`, `JwtAuthFilter.java`
- 수정: `Parent.java`(email/passwordHash/provider/providerId 추가), `ParentRepository.java`, `ParentService.java`, `SecurityConfig.java`, `ErrorCode.java`, `schema.sql`, `application.yml`, `.env`/`.env.example`

## 3. 인증 관련 확정된 결정 사항 (가이드 문서에 미반영)

- Supabase Auth **미사용**. 서버가 직접 회원가입·로그인·토큰 발급 처리
- 지원 범위: **이메일/비밀번호 + 카카오 로그인**. 구글·네이버는 미구현 (창업 운영팀 요건이 "카카오·구글·네이버 중 1개 이상"이라 카카오만으로 요건 충족 — 필요 시 `KakaoClient` 패턴을 복제해서 확장 가능)
- 토큰 전략: **Access 토큰만** (Refresh 없음), 만료 7일(`JWT_EXPIRATION_MS` 기본값)
- 카카오 로그인 방식: **클라이언트(모바일 카카오 SDK)가 액세스 토큰을 서버로 전달 → 서버가 카카오 `v2/user/me` API로 프로필만 조회**. 서버가 카카오와 직접 리다이렉트·인가코드를 주고받는 방식이 아님 (STATELESS API 구조라 이 방식이 더 적합하다고 판단)
- `POST /parents/me`는 원래 "최초 로그인 후 프로필 등록"이었는데, 이제 가입은 `/auth/signup`에서 끝나므로 **"이름 수정"으로 의미가 바뀜** (팀 컨펌 필요 — 아래 5번 참고)

## 4. 현재 상태 / 마지막으로 확인된 것

- `./gradlew build` 실행 결과: `compileJava` 성공, `test`(`contextLoads`) 실패
- 실패 원인: `.env`의 `JWT_SECRET`이 비어있어 `JwtProvider` 빈 생성 중 예외 발생
- 해결법 안내함(로컬용 시크릿 값 생성해서 `.env`에 채우기) — **실제로 채워서 재빌드했는지는 이 세션에서 확인되지 않음**

## 5. 다음 세션에서 확인/진행해야 할 것

1. **`.env`의 `JWT_SECRET` 채워졌는지 확인** 후 `./gradlew build` 재실행해서 `BUILD SUCCESSFUL` 뜨는지 확인
2. **로컬 DB 재생성 여부 확인** — `parents` 테이블 구조가 바뀌어서 기존 DB와 안 맞음. `schema.sql` → `seed.sql` 순서로 재실행 필요 (아직 안 했다면 진행)
3. **API 실사용 테스트 진행 여부 확인** — `/auth/signup` → `/auth/login` → `/children` → `/children/{id}/consents` → `/children/{id}/home` 순서로 curl/Postman 호출해서 정상 동작 확인 (아직 안 했다면 진행)
4. **팀 컨펌 2건 진행 여부 확인**
   - `POST /parents/me`를 계속 쓸지, 없앨지
   - 카카오 로그인 방식(클라이언트 토큰 전달 vs 서버 리다이렉트)이 실제 앱 구현 방향과 맞는지
5. 위 4건이 다 끝나면 이영준 담당 파트는 기능적으로 마무리 상태. 이후는 선택 사항(단위 테스트 작성, 비밀번호 정책 강화 등)

## 6. 알아두면 좋은 설계 메모

- `ChildService`가 `ConsentService`를 의존하고 있어서, `ConsentService`가 `ChildService`를 다시 참조하면 순환 의존이 생김 → 그래서 소유권 검증(`getOwnedChild`)은 **컨트롤러**에서 하고, 검증된 `Child`/`childId`만 서비스로 넘기는 패턴을 씀
- `application.yml`의 `ddl-auto: validate` 때문에 테이블은 Hibernate가 자동 생성 안 하고 `schema.sql`이 유일한 스키마 소스임. 엔티티 필드를 바꾸면 `schema.sql`도 같이 바꿔야 함
- `infra` 어댑터 패턴(인터페이스 + `Default*` 구현체, `@Component` + `WebClient` 주입)을 카카오 클라이언트에도 동일하게 적용함 — 구글/네이버 추가 시 같은 패턴 재사용

## 7. 참고 문서 (같은 세션에서 만든 파일들)

- `docs/backend-dev-guide.md` — 원본 개발 가이드 (Supabase Auth 전제 부분은 3번 항목으로 대체됨)
- 창업 운영팀 MVP DB 구조/요건 정리본 — 별도로 전달드린 `굿퀘스천_MVP_DB구조_요건정리.md` 파일 참고
