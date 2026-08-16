#!/usr/bin/env bash
# 버전 하나를 격리 환경으로 띄운다: worktree + 전용 Postgres + (필요시) 패치 + bootRun.
#
# 사용: perf/bin/up.sh perf/turn-v1
#
# - DB는 빈 컨테이너로 시작한다. 그 버전 코드가 부팅하며 자기 시점의
#   Flyway 마이그레이션과 시드를 적용하므로, 스키마/시드가 자동으로
#   그 버전과 일치한다. 최신 DB에 옛 코드를 붙이지 않는 이유다.
# - .env는 저장소 것을 복사한 뒤 DB/포트만 덮어쓴다 (OPENAI 키 등 유지).
# - patch 열이 '-'가 아니면 perf/patches/<이름>/apply.sh를 worktree에 적용하고
#   SPRING_PROFILES_ACTIVE=perf로 띄운다 (턴 트랙의 느린 LLM 대역).
set -euo pipefail
source "$(dirname "$0")/lib.sh"
load_version "${1:?사용법: up.sh <perf/태그>}"

mkdir -p "$RESULTS"

# 1) worktree
if [ ! -d "$WORKTREE" ]; then
    git -C "$REPO_ROOT" worktree add --detach "$WORKTREE" "$TAG"
    echo "worktree: $WORKTREE ($(git -C "$WORKTREE" log -1 --format='%h %s'))"
else
    echo "worktree 재사용: $WORKTREE"
fi

# 2) 패치 (턴 트랙의 느린 LLM 대역 등)
if [ "$PATCH" != "-" ]; then
    bash "$PERF_DIR/patches/$PATCH/apply.sh" "$WORKTREE"
fi

# 3) DB 컨테이너 (빈 DB - 부팅 시 그 버전의 Flyway가 채운다)
if ! docker inspect "$CONTAINER" >/dev/null 2>&1; then
    docker run -d --name "$CONTAINER" -p "$DB_PORT:5432" \
        -e POSTGRES_DB=goodquestion -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=perf \
        postgres:17 >/dev/null
    echo "DB: $CONTAINER (localhost:$DB_PORT)"
fi
for _ in $(seq 1 30); do
    docker exec "$CONTAINER" pg_isready -U postgres -q 2>/dev/null && break
    sleep 1
done

# 4) .env - 저장소 .env 복사 후 DB/포트 덮어쓰기 (뒤에 나온 키가 이기지 않는
#    파서도 있어 파이썬으로 병합한다)
python3 - "$REPO_ROOT/.env" "$WORKTREE/.env" "$DB_PORT" "$SERVER_PORT" <<'PYEOF'
import sys
src, dst, db_port, server_port = sys.argv[1:5]
override = {
    "DB_URL": f"jdbc:postgresql://localhost:{db_port}/goodquestion",
    "DB_USERNAME": "postgres",
    "DB_PASSWORD": "perf",
    "SERVER_PORT": server_port,
}
lines, seen = [], set()
for line in open(src, encoding="utf-8"):
    key = line.split("=", 1)[0].strip() if "=" in line else None
    if key in override:
        lines.append(f"{key}={override[key]}\n"); seen.add(key)
    else:
        lines.append(line)
for key, value in override.items():
    if key not in seen:
        lines.append(f"{key}={value}\n")
open(dst, "w", encoding="utf-8").writelines(lines)
PYEOF

# 5) 서버 기동 (환경변수가 .env보다 우선하는 스프링 규칙도 함께 이용)
PROFILE_ENV=()
if [ "$PATCH" != "-" ]; then
    PROFILE_ENV=(SPRING_PROFILES_ACTIVE=perf PERF_LLM_DELAY_MS="${PERF_LLM_DELAY_MS:-2000}")
fi
echo "부팅 중... (이 버전의 gradle 배포판 첫 다운로드면 수 분 걸릴 수 있음)"
(
    cd "$WORKTREE"
    nohup env SERVER_PORT="$SERVER_PORT" \
        DB_URL="jdbc:postgresql://localhost:$DB_PORT/goodquestion" \
        DB_USERNAME=postgres DB_PASSWORD=perf \
        ${PROFILE_ENV[@]+"${PROFILE_ENV[@]}"} \
        ./gradlew bootRun --console=plain >"$RESULTS/server.log" 2>&1 &
    echo $! > "$RESULTS/gradle.pid"
)

if wait_health "$BASE_URL" 360; then
    echo "UP: $TAG -> $BASE_URL  ($DESC)"
else
    echo "실패: 헬스체크 타임아웃. 로그: $RESULTS/server.log" >&2
    tail -20 "$RESULTS/server.log" >&2
    exit 1
fi
