#!/usr/bin/env bash
# 공용 헬퍼 - versions.tsv 행 조회와 경로 규약.
# 사용: source lib.sh; load_version perf/turn-v1

REPO_ROOT="$(git -C "$(dirname "${BASH_SOURCE[0]}")" rev-parse --show-toplevel)"
PERF_DIR="$REPO_ROOT/perf"
VERSIONS_TSV="$PERF_DIR/versions.tsv"
# worktree는 저장소 밖에 둔다 - 저장소 안에 두면 IDE/검색이 오염된다
WORK_BASE="${PERF_WORK_DIR:-$HOME/git/goodquestion-perf}"

load_version() {
    local tag="$1"
    local row
    row=$(awk -F'\t' -v t="$tag" '$3 == t' "$VERSIONS_TSV")
    if [ -z "$row" ]; then
        echo "오류: versions.tsv에 없는 태그: $tag" >&2
        echo "사용 가능한 태그:" >&2
        awk -F'\t' 'NR>1 {print "  " $3}' "$VERSIONS_TSV" >&2
        return 1
    fi
    TRACK=$(echo "$row" | cut -f1)
    VER=$(echo "$row" | cut -f2)
    TAG=$(echo "$row" | cut -f3)
    SHA=$(echo "$row" | cut -f4)
    SERVER_PORT=$(echo "$row" | cut -f5)
    DB_PORT=$(echo "$row" | cut -f6)
    PATCH=$(echo "$row" | cut -f7)
    DESC=$(echo "$row" | cut -f8)
    NAME="${TAG#perf/}"                      # turn-v1
    WORKTREE="$WORK_BASE/$NAME"
    CONTAINER="gq-perf-$NAME"
    RESULTS="$PERF_DIR/results/$NAME"
    BASE_URL="http://localhost:$SERVER_PORT"
}

wait_health() {
    local url="$1" timeout="${2:-360}" i=0
    while [ $i -lt "$timeout" ]; do
        if curl -s -o /dev/null -w '%{http_code}' "$url/actuator/health" 2>/dev/null | grep -q 200; then
            return 0
        fi
        sleep 2; i=$((i + 2))
    done
    return 1
}
