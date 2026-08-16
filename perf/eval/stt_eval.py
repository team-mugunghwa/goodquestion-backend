"""트랙 C - STT 정확도/환각/지연 평가.

samples/의 비발화 음원(무음/잡음/순음)과 real/의 실제 발화 샘플을 라운드마다
/api/stt에 보내 기록한다.

지표:
- 비발화 환각율: 무음/잡음/순음에서 빈 텍스트가 아닌 응답이 나온 비율.
  어휘 힌트 에코(v3에서 잡은 문제)가 정확히 이 지표로 드러난다.
- 실발화 CER: real/ 샘플의 파일명(기대 문장) 대비 문자 오류율.
- confidence 분포: v2(#34)부터 응답에 실린다 - 없던 버전은 null.
- 지연 p50/p95.

422 STT_EMPTY_TEXT는 "잘 걸러냈다"이므로 비발화 입력에서는 성공으로 센다.

사용:
  python3 perf/eval/stt_eval.py --base http://localhost:8098 \
      --out perf/results/stt-v1 --rounds 5
"""

import argparse
import json
import pathlib
import sys

sys.path.insert(0, str(pathlib.Path(__file__).parent))
from gqapi import Api, percentile

NON_SPEECH = ("silence.wav", "noise.wav", "tone.wav")


def cer(expected, actual):
    """문자 오류율 = 편집거리 / 기대 길이. 공백은 표기 차이라 제외한다."""
    e = expected.replace(" ", "")
    a = (actual or "").replace(" ", "")
    if not e:
        return None
    prev = list(range(len(a) + 1))
    for i, ec in enumerate(e, 1):
        cur = [i]
        for j, ac in enumerate(a, 1):
            cur.append(min(prev[j - 1] + (ec != ac), prev[j] + 1, cur[j - 1] + 1))
        prev = cur
    return round(prev[len(a)] / len(e), 3)


def submit(api, path):
    status, body, latency = api.post_multipart(
        "/api/stt", "audio", path.name, path.read_bytes(), "audio/wav")
    row = {"sample": path.name, "status": status, "latency_ms": round(latency, 1)}
    if status == 200:
        data = json.loads(body)
        row["text"] = data.get("text") or ""
        row["confidence"] = data.get("confidence")
        row["lowConfidence"] = data.get("lowConfidence")
    else:
        try:
            row["code"] = json.loads(body).get("code")
        except (ValueError, AttributeError):
            row["code"] = body[:80]
    return row


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--base", required=True)
    parser.add_argument("--out", required=True)
    parser.add_argument("--rounds", type=int, default=5)
    args = parser.parse_args()

    samples_dir = pathlib.Path(__file__).parent / "samples"
    non_speech = [samples_dir / n for n in NON_SPEECH if (samples_dir / n).exists()]
    real = sorted((samples_dir / "real").glob("*.wav"))
    if not non_speech:
        sys.exit("샘플이 없다 - 먼저 python3 perf/eval/gen_samples.py 를 실행할 것")

    api = Api(args.base)
    api.login()

    rows = []
    for round_no in range(1, args.rounds + 1):
        for path in non_speech + real:
            row = submit(api, path)
            row["round"] = round_no
            row["kind"] = "non_speech" if path.name in NON_SPEECH else "real"
            if row["kind"] == "real" and row.get("text") is not None:
                row["expected"] = path.stem
                row["cer"] = cer(path.stem, row.get("text"))
            rows.append(row)
            shown = row.get("text", row.get("code", ""))
            print(f"[{round_no}] {path.name}: {row['status']} {row['latency_ms']}ms \"{shown}\"")

    ns = [r for r in rows if r["kind"] == "non_speech"]
    # 환각 = 비발화인데 200으로 텍스트가 나옴. 422(STT_EMPTY_TEXT)와 빈 텍스트는 정상 차단.
    hallucinated = [r for r in ns if r["status"] == 200 and (r.get("text") or "").strip()]
    real_rows = [r for r in rows if r["kind"] == "real" and r.get("cer") is not None]
    latencies = [r["latency_ms"] for r in rows]
    confidences = [r["confidence"] for r in rows if r.get("confidence") is not None]

    summary = {
        "rounds": args.rounds,
        "non_speech_samples": len(ns),
        "hallucination_rate": round(len(hallucinated) / len(ns), 3) if ns else None,
        "hallucinated_texts": sorted({r["text"] for r in hallucinated}),
        "real_samples": len(real_rows),
        "real_cer_avg": round(sum(r["cer"] for r in real_rows) / len(real_rows), 3)
                        if real_rows else None,
        "confidence_avg": round(sum(confidences) / len(confidences), 3) if confidences else None,
        "latency_p50_ms": percentile(latencies, 50),
        "latency_p95_ms": percentile(latencies, 95),
    }

    out = pathlib.Path(args.out)
    out.mkdir(parents=True, exist_ok=True)
    (out / "stt-eval.json").write_text(
        json.dumps({"summary": summary, "rows": rows}, ensure_ascii=False, indent=2),
        encoding="utf-8")
    print("\n요약:", json.dumps(summary, ensure_ascii=False))


if __name__ == "__main__":
    main()
