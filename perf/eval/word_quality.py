"""트랙 B - 단어 뜻 생성 품질/지연 평가.

평가셋(wordset.txt)의 낱말을 라운드마다 저장하고, 응답의 뜻/예문과 지연을
기록한다. 사전(story_vocabulary)에 없는 낱말이라 전 버전이 LLM 경로를 탄다.

라운드 사이에는 wordbook 표를 비워 DUPLICATE_WORD를 피한다 (컨테이너에
psql로 직접 지운다 - 삭제 API가 없던 버전에서도 같은 방식이 통한다).

주의: 라운드 x 낱말 수만큼 실제 LLM 호출이 나간다 - 비용과 시간이 든다.

사용:
  python3 perf/eval/word_quality.py --base http://localhost:8093 \
      --container gq-perf-word-v1 --out perf/results/word-v1 --rounds 3
"""

import argparse
import json
import pathlib
import subprocess
import sys

sys.path.insert(0, str(pathlib.Path(__file__).parent))
from gqapi import Api, DEMO_CHILD_ID, percentile

MAX_MEANING_CHARS = 20      # v3(#92)의 기준
EOJEOL_RANGE = (5, 7)       # v4(#94)의 기준


def wipe_wordbook(container):
    subprocess.run(
        ["docker", "exec", container, "psql", "-U", "postgres", "-d", "goodquestion",
         "-q", "-c", f"delete from wordbook where child_id = '{DEMO_CHILD_ID}';"],
        check=True, capture_output=True)


def evaluate(api, word):
    status, body, latency = api.request(
        "POST", f"/api/children/{DEMO_CHILD_ID}/words",
        {"word": word, "entryType": "UNKNOWN"})
    row = {"word": word, "status": status, "latency_ms": round(latency, 1)}
    if status == 201:
        data = json.loads(body)
        meaning = (data.get("meaning") or "").strip()
        row["meaning"] = meaning
        row["meaning_chars"] = len(meaning)
        row["meaning_eojeol"] = len(meaning.split())
        # 예문 3종은 V14(word-v5 부근)부터다 - 없던 버전은 null로 남는다
        row["examples"] = sum(1 for k in
                              ("exampleSentence", "exampleSentenceDaily", "exampleSentenceAdvanced")
                              if data.get(k))
    elif status == 422:
        row["rejected"] = True     # INVALID_WORD 오거절 (실재 낱말이므로 전부 오거절)
    return row


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--base", required=True)
    parser.add_argument("--container", required=True, help="이 버전의 DB 컨테이너 이름")
    parser.add_argument("--out", required=True)
    parser.add_argument("--rounds", type=int, default=3)
    parser.add_argument("--wordset", default=str(pathlib.Path(__file__).parent / "wordset.txt"))
    args = parser.parse_args()

    words = [w.strip() for w in open(args.wordset, encoding="utf-8")
             if w.strip() and not w.startswith("#")]
    api = Api(args.base)
    api.login()

    rows = []
    for round_no in range(1, args.rounds + 1):
        wipe_wordbook(args.container)
        for word in words:
            row = evaluate(api, word)
            row["round"] = round_no
            rows.append(row)
            print(f"[{round_no}] {word}: {row['status']} {row['latency_ms']}ms "
                  f"{row.get('meaning', '(거절)' if row.get('rejected') else '')}")

    saved = [r for r in rows if r["status"] == 201]
    latencies = [r["latency_ms"] for r in saved]
    summary = {
        "samples": len(rows),
        "rounds": args.rounds,
        "latency_p50_ms": percentile(latencies, 50),
        "latency_p95_ms": percentile(latencies, 95),
        "reject_count": sum(1 for r in rows if r.get("rejected")),
        "over_20chars": sum(1 for r in saved if r["meaning_chars"] > MAX_MEANING_CHARS),
        "eojeol_in_range": sum(1 for r in saved
                               if EOJEOL_RANGE[0] <= r["meaning_eojeol"] <= EOJEOL_RANGE[1]),
        "avg_chars": round(sum(r["meaning_chars"] for r in saved) / len(saved), 1) if saved else None,
        "avg_eojeol": round(sum(r["meaning_eojeol"] for r in saved) / len(saved), 1) if saved else None,
        "examples_full": sum(1 for r in saved if r.get("examples") == 3),
    }

    out = pathlib.Path(args.out)
    out.mkdir(parents=True, exist_ok=True)
    (out / "word-quality.json").write_text(
        json.dumps({"summary": summary, "rows": rows}, ensure_ascii=False, indent=2),
        encoding="utf-8")
    print("\n요약:", json.dumps(summary, ensure_ascii=False))


if __name__ == "__main__":
    main()
