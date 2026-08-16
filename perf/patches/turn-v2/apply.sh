#!/usr/bin/env bash
# v2(0feceae) worktree에 perf 프로파일 전용 느린 LLM 구현을 추가한다.
# 기존 파일은 건드리지 않는다 - LlmClient 포트 뒤에 @Primary로 끼운다.
set -euo pipefail
WORKTREE="${1:?사용법: apply.sh <worktree경로>}"
HERE="$(cd "$(dirname "$0")" && pwd)"

cp "$HERE/PerfSlowLlmClient.java" \
   "$WORKTREE/src/main/java/com/mugunghwa/goodquestion/ai/llm/PerfSlowLlmClient.java"
echo "패치 적용: turn-v2 느린 LLM 대역 (perf 프로파일 @Primary 추가)"
