"""results/ 아래 버전별 결과를 트랙별 마크다운 표로 취합한다.

사용: python3 perf/bin/collect.py [--out perf/results/REPORT.md]
"""

import argparse
import json
import pathlib


def load(path):
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except (OSError, ValueError):
        return None


def k6_metric(data, name, stat):
    metric = (data or {}).get("metrics", {}).get(name, {})
    value = metric.get(stat)
    return round(value, 1) if isinstance(value, (int, float)) else None


def turn_table(results_dir, versions):
    lines = ["## 트랙 A - 턴 부하 중 무관 API 지연", "",
             "| 버전 | 설명 | 로그인 p95(ms) | 로그인 성공률 | 홈 p95(ms) | 턴 p95(ms) |",
             "|---|---|---|---|---|---|"]
    found = False
    for name, desc in versions:
        data = load(results_dir / name / "turn-load.json")
        if not data:
            continue
        found = True
        ok = k6_metric(data, "probe_login_ok", "rate")
        lines.append("| {} | {} | {} | {} | {} | {} |".format(
            name, desc,
            k6_metric(data, "probe_login_duration", "p(95)"),
            f"{ok * 100:.1f}%" if ok is not None else None,
            k6_metric(data, "probe_home_duration", "p(95)"),
            k6_metric(data, "turn_duration", "p(95)")))
    return lines if found else []


def summary_table(results_dir, versions, filename, title, columns):
    lines = [f"## {title}", "",
             "| 버전 | 설명 | " + " | ".join(label for label, _ in columns) + " |",
             "|---" * (len(columns) + 2) + "|"]
    found = False
    for name, desc in versions:
        data = load(results_dir / name / filename)
        if not data:
            continue
        found = True
        summary = data.get("summary", {})
        cells = [str(summary.get(key)) for _, key in columns]
        lines.append(f"| {name} | {desc} | " + " | ".join(cells) + " |")
    return lines if found else []


def main():
    parser = argparse.ArgumentParser()
    perf_dir = pathlib.Path(__file__).resolve().parent.parent
    parser.add_argument("--out", default=str(perf_dir / "results" / "REPORT.md"))
    args = parser.parse_args()

    versions = {"turn": [], "word": [], "stt": []}
    for line in (perf_dir / "versions.tsv").read_text(encoding="utf-8").splitlines()[1:]:
        cols = line.split("\t")
        versions[cols[0]].append((cols[2].removeprefix("perf/"), cols[7]))

    results_dir = perf_dir / "results"
    sections = ["# 성능 재현 측정 결과", "",
                "현재 환경에서의 재현 측정이다 - 방법은 perf/README.md 참고.", ""]
    sections += turn_table(results_dir, versions["turn"]) + [""]
    sections += summary_table(
        results_dir, versions["word"], "word-quality.json",
        "트랙 B - 단어 뜻 생성",
        [("p50(ms)", "latency_p50_ms"), ("p95(ms)", "latency_p95_ms"),
         ("오거절", "reject_count"), ("20자 초과", "over_20chars"),
         ("5~7어절", "eojeol_in_range"),
         ("평균 어절", "avg_eojeol"), ("예문 3종", "examples_full")]) + [""]
    sections += summary_table(
        results_dir, versions["stt"], "stt-eval.json",
        "트랙 C - STT",
        [("환각율", "hallucination_rate"), ("실발화 CER", "real_cer_avg"),
         ("신뢰도 평균", "confidence_avg"),
         ("p50(ms)", "latency_p50_ms"), ("p95(ms)", "latency_p95_ms")]) + [""]

    out = pathlib.Path(args.out)
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text("\n".join(sections), encoding="utf-8")
    print("\n".join(sections))
    print(f"\n저장: {out}")


if __name__ == "__main__":
    main()
