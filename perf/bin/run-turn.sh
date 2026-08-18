#!/usr/bin/env bash
# 트랙 A 측정 - 떠 있는 버전에 턴 부하 + 무관 API 프로브를 건다.
# 사용: perf/bin/run-turn.sh perf/turn-v1 [k6 추가 인자...]
set -euo pipefail
source "$(dirname "$0")/lib.sh"
load_version "${1:?사용법: run-turn.sh <perf/태그>}"
shift
mkdir -p "$RESULTS"

# 워밍업 - JIT가 덜 된 초반 표본이 섞이지 않게 30초 가볍게 돌리고 버린다
echo "워밍업 30s..."
k6 run --quiet -e BASE="$BASE_URL" -e TURN_VUS=2 -e DURATION=30s \
    "$PERF_DIR/k6/login-under-turn-load.js" >/dev/null || true

echo "본 측정..."
k6 run -e BASE="$BASE_URL" \
    --summary-export "$RESULTS/turn-load.json" \
    "$@" "$PERF_DIR/k6/login-under-turn-load.js"
echo "결과: $RESULTS/turn-load.json"
