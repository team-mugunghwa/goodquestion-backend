#!/usr/bin/env bash
# 트랙 C 측정 - STT 환각/정확도/지연. 실제 STT 호출이 나간다 (비용 주의).
# 사용: perf/bin/run-stt.sh perf/stt-v1 [--rounds 5]
set -euo pipefail
source "$(dirname "$0")/lib.sh"
load_version "${1:?사용법: run-stt.sh <perf/태그>}"
shift
[ -f "$PERF_DIR/eval/samples/silence.wav" ] || python3 "$PERF_DIR/eval/gen_samples.py"
python3 "$PERF_DIR/eval/stt_eval.py" --base "$BASE_URL" --out "$RESULTS" "$@"
