#!/usr/bin/env bash
# v1(57fb36f) worktree에 느린 LLM 대역을 덮어쓴다.
# v1은 analysis/character가 별도 스텁 클래스라 두 파일을 통째로 교체한다.
set -euo pipefail
WORKTREE="${1:?사용법: apply.sh <worktree경로>}"
HERE="$(cd "$(dirname "$0")" && pwd)"
BASE="$WORKTREE/src/main/java/com/mugunghwa/goodquestion/ai"

cp "$HERE/AnalysisLlmClient.java" "$BASE/analysis/AnalysisLlmClient.java"
cp "$HERE/CharacterLlmClient.java" "$BASE/character/CharacterLlmClient.java"
echo "패치 적용: turn-v1 느린 LLM 대역 (analysis + character 스텁 교체)"
