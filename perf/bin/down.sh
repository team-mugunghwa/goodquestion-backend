#!/usr/bin/env bash
# up.sh가 띄운 것을 내린다. 결과(results/)는 남긴다.
# 사용: perf/bin/down.sh perf/turn-v1 [--keep-worktree]
set -euo pipefail
source "$(dirname "$0")/lib.sh"
load_version "${1:?사용법: down.sh <perf/태그> [--keep-worktree]}"

if [ -f "$RESULTS/gradle.pid" ]; then
    kill "$(cat "$RESULTS/gradle.pid")" 2>/dev/null || true
    rm -f "$RESULTS/gradle.pid"
fi
# gradle이 낳은 자식 JVM까지 - worktree 경로로 좁혀서 다른 프로세스는 건드리지 않는다
pkill -f "$WORKTREE" 2>/dev/null || true

docker rm -f "$CONTAINER" >/dev/null 2>&1 && echo "DB 제거: $CONTAINER" || true

if [ "${2:-}" != "--keep-worktree" ] && [ -d "$WORKTREE" ]; then
    git -C "$REPO_ROOT" worktree remove --force "$WORKTREE" && echo "worktree 제거: $WORKTREE"
fi
echo "DOWN: $TAG"
