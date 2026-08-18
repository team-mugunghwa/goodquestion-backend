#!/usr/bin/env bash
# versions.tsv의 지점에 perf/* 태그를 붙인다. 로컬 태그다 - 팀과 공유하려면
# git push origin --tags 'refs/tags/perf/*' 를 직접 실행한다.
set -euo pipefail
source "$(dirname "$0")/lib.sh"

awk -F'\t' 'NR>1 {print $3 "\t" $4 "\t" $8}' "$VERSIONS_TSV" | while IFS=$'\t' read -r tag sha desc; do
    if ! git -C "$REPO_ROOT" cat-file -e "$sha^{commit}" 2>/dev/null; then
        echo "건너뜀: $tag ($sha 없음 - fetch 필요?)" >&2
        continue
    fi
    git -C "$REPO_ROOT" tag -f "$tag" "$sha" >/dev/null
    echo "$tag -> $(git -C "$REPO_ROOT" log -1 --format='%h %s' "$sha")"
done
echo
echo "완료. 확인: git tag -l 'perf/*'"
