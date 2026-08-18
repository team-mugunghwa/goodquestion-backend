#!/usr/bin/env bash
# 트랙 B 측정 - 뜻 생성 품질/지연. 실제 LLM 호출이 나간다 (비용 주의).
# 사용: perf/bin/run-word.sh perf/word-v1 [--rounds 3]
set -euo pipefail
source "$(dirname "$0")/lib.sh"
load_version "${1:?사용법: run-word.sh <perf/태그>}"
shift
python3 "$PERF_DIR/eval/word_quality.py" \
    --base "$BASE_URL" --container "$CONTAINER" --out "$RESULTS" "$@"
