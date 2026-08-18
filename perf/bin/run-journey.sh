#!/usr/bin/env bash
# 여정 트랙 측정 - 이야기 시작부터 종료까지 구간별 실측 + TTS/STT 길이 곡선.
# 실제 LLM/STT/TTS 호출이 나간다 (비용 주의).
# 사용: perf/bin/run-journey.sh perf/journey-v4 [--rounds 3] [--skip-tts]
set -euo pipefail
source "$(dirname "$0")/lib.sh"
load_version "${1:?사용법: run-journey.sh <perf/태그>}"
shift
mkdir -p "$RESULTS"

echo "== 여정 (구간별) =="
python3 "$PERF_DIR/eval/journey.py" --base "$BASE_URL" --out "$RESULTS" \
    --container "$CONTAINER" --server-log "$RESULTS/server.log" "$@"

echo "== TTS 길이/캐시 곡선 =="
python3 "$PERF_DIR/eval/tts_eval.py" --base "$BASE_URL" --out "$RESULTS" --rounds 3

echo "== STT 길이 곡선 + 환각율 =="
[ -f "$PERF_DIR/eval/samples/noise_10s.wav" ] || python3 "$PERF_DIR/eval/gen_samples.py"
python3 "$PERF_DIR/eval/stt_eval.py" --base "$BASE_URL" --out "$RESULTS" --rounds 3
