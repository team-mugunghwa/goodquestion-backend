# Railway 배포 가이드

Railway에 Spring Boot 백엔드와 PostgreSQL을 함께 배포하는 절차. Dockerfile 빌드
방식을 사용하며, DB 마이그레이션은 Flyway가 앱 기동 시 자동으로 적용한다.

## 사전 준비

- Railway 계정 (railway.app)
- GitHub 저장소에 push된 코드
- 카카오 REST API 키, JWT 시크릿, STT/TTS/LLM API 키 등 환경변수 값

## 1. Railway 프로젝트 생성

1. Railway 대시보드에서 New Project 클릭
2. Deploy from GitHub repo 선택, `goodquestion-backend` 저장소 선택
3. Railway가 저장소 루트의 `Dockerfile`과 `railway.toml`을 감지해서 빌드 시작
4. 첫 배포는 환경변수가 없어 실패한다. 정상. 다음 단계 진행

## 2. PostgreSQL 서비스 추가

1. 프로젝트 화면에서 New -> Database -> Add PostgreSQL 선택
2. 생성되면 `Postgres` 서비스가 뜨고 다음 변수가 자동으로 만들어진다
   - `PGHOST`, `PGPORT`, `PGUSER`, `PGPASSWORD`, `PGDATABASE`
   - `DATABASE_URL` (postgresql://... 형식, JDBC 아님)
3. 앱 서비스와 같은 프로젝트 안에 있으면 내부 네트워크로 연결된다

## 3. 앱 서비스 환경변수 설정

앱 서비스 -> Variables 탭에서 다음 값을 추가한다. Railway는 `${{ Postgres.VAR }}`
문법으로 다른 서비스의 변수를 참조할 수 있다.

### DB 연결 (Postgres 서비스 참조)

```
DB_URL=jdbc:postgresql://${{ Postgres.PGHOST }}:${{ Postgres.PGPORT }}/${{ Postgres.PGDATABASE }}
DB_USERNAME=${{ Postgres.PGUSER }}
DB_PASSWORD=${{ Postgres.PGPASSWORD }}
```

Postgres 서비스가 주는 `DATABASE_URL`은 `postgresql://` 스킴이지만 Spring
DataSource는 `jdbc:postgresql://` 형식을 요구하므로, 위처럼 개별 변수를 조립해
JDBC URL을 만든다.

### 인증

```
JWT_SECRET=<openssl rand -base64 32로 생성한 값>
JWT_EXPIRATION_MS=604800000
KAKAO_CLIENT_ID=<카카오 REST API 키>
KAKAO_CLIENT_SECRET=<카카오 콘솔에서 client secret을 '사용함'으로 설정한 경우에만>
```

### 외부 API

```
STT_API_KEY=<값>
TTS_API_KEY=<값>
LLM_API_KEY=<값>
ANALYSIS_MODEL=<선택, 미설정 시 기본값>
CHARACTER_MODEL=<선택, 미설정 시 기본값>
```

### PORT

`PORT`는 Railway가 자동으로 주입한다. 직접 설정하지 않는다. `application.yml`이
`${PORT:8080}`으로 받는다.

## 4. 외부 접근 도메인 발급

1. 앱 서비스 -> Settings -> Networking -> Generate Domain 클릭
2. `xxx.up.railway.app` 형식의 도메인이 발급된다
3. 카카오 개발자 콘솔의 Redirect URI에 `https://xxx.up.railway.app/api/auth/kakao/callback`
   (실제 경로는 프로젝트 구현에 맞춰서) 등록

## 5. 배포 확인

환경변수를 저장하면 Railway가 자동으로 재배포한다. 확인 순서:

1. Deployments 탭에서 빌드 로그가 성공하는지
2. Logs에 다음이 순서대로 뜨는지
   ```
   Migrating schema "public" to version "1 - init schema"
   Migrating schema "public" with repeatable migration "1 seed content"
   Migrating schema "public" with repeatable migration "2 seed demo data"
   Successfully applied 3 migrations
   Tomcat started on port ...
   Started GoodquestionBackendApplication
   ```
3. `https://<domain>/actuator/health`가 `{"status":"UP"}` 반환

## 6. 이후 배포

`main` 또는 설정한 브랜치에 push하면 Railway가 자동으로 감지해서 재배포한다.
브랜치는 앱 서비스 Settings -> Source에서 지정한다.

## 트러블슈팅

**빌드가 Java 25를 못 찾는다**
- `Dockerfile`이 `eclipse-temurin:25-jdk`를 명시하므로 발생하지 않아야 한다.
  Nixpacks 자동 감지 모드로 잘못 갔다면 `railway.toml`의 `builder = "DOCKERFILE"`을
  확인한다.

**Flyway가 "found non-empty schema without schema history table" 오류**
- DB에 이력 테이블 없이 다른 테이블만 있는 경우. Railway의 Postgres 콘솔에서
  `drop schema public cascade; create schema public;` 실행 후 재배포.

**Health check timeout**
- 앱 기동이 120초를 넘는다는 뜻. `railway.toml`의 `healthcheckTimeout`을 늘리거나
  로그로 원인 파악. Flyway 마이그레이션이 오래 걸리는 경우가 있다.

**카카오 로그인 401**
- Redirect URI가 카카오 콘솔에 등록된 것과 정확히 일치하는지 확인.
  `https://` 스킴, 도메인, 경로가 완전히 같아야 한다.
