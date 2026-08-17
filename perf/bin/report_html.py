"""results/를 읽어 자체 완결 HTML 보고서(인라인 SVG 차트)를 만든다.

외부 라이브러리/CDN 없이 표준 라이브러리만 쓴다 - 발표장에서 파일 하나로 열린다.

사용: python3 perf/bin/report_html.py [--out perf/results/REPORT.html]
"""

import argparse
import html
import json
import pathlib

PALETTE = ["#4878a8", "#e49444", "#5aa469", "#c05d5d", "#8a6fb0", "#7a7a7a"]


def load(path):
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except (OSError, ValueError):
        return None


def esc(text):
    return html.escape(str(text))


# ---- SVG 헬퍼 ----

def grouped_bars(title, groups, series, unit="ms", width=760):
    """groups: [라벨], series: [(이름, [값 or None])]. 그룹 x 시리즈 막대."""
    if not groups or not all(any(v is not None for v in vals) for _, vals in series):
        pass
    values = [v for _, vals in series for v in vals if v is not None]
    if not values:
        return ""
    peak = max(values) or 1
    chart_h, top, bottom, left = 220, 28, 46, 56
    height = chart_h + top + bottom
    group_w = (width - left - 20) / len(groups)
    bar_w = min(34.0, group_w * 0.8 / max(1, len(series)))
    parts = [f'<svg viewBox="0 0 {width} {height}" role="img" aria-label="{esc(title)}">']
    parts.append(f'<text x="{left}" y="16" class="ct">{esc(title)}</text>')
    for frac in (0, 0.5, 1.0):
        y = top + chart_h - chart_h * frac
        parts.append(f'<line x1="{left}" y1="{y:.0f}" x2="{width - 10}" y2="{y:.0f}" class="grid"/>')
        parts.append(f'<text x="{left - 6}" y="{y + 4:.0f}" class="ax" text-anchor="end">'
                     f'{peak * frac:,.0f}</text>')
    for gi, label in enumerate(groups):
        x0 = left + gi * group_w + (group_w - bar_w * len(series)) / 2
        for si, (name, vals) in enumerate(series):
            v = vals[gi]
            if v is None:
                continue
            h = chart_h * v / peak
            x = x0 + si * bar_w
            y = top + chart_h - h
            parts.append(f'<rect x="{x:.1f}" y="{y:.1f}" width="{bar_w - 3:.1f}" '
                         f'height="{h:.1f}" fill="{PALETTE[si % len(PALETTE)]}">'
                         f'<title>{esc(name)} / {esc(label)}: {v:,.0f}{unit}</title></rect>')
            if h > 14:
                parts.append(f'<text x="{x + (bar_w - 3) / 2:.1f}" y="{y - 3:.1f}" class="val" '
                             f'text-anchor="middle">{v:,.0f}</text>')
        parts.append(f'<text x="{left + gi * group_w + group_w / 2:.1f}" '
                     f'y="{top + chart_h + 16}" class="ax" text-anchor="middle">{esc(label)}</text>')
    lx = left
    for si, (name, _) in enumerate(series):
        parts.append(f'<rect x="{lx}" y="{height - 14}" width="10" height="10" '
                     f'fill="{PALETTE[si % len(PALETTE)]}"/>')
        parts.append(f'<text x="{lx + 14}" y="{height - 5}" class="ax">{esc(name)}</text>')
        lx += 14 + 7 * len(str(name)) + 26
    parts.append("</svg>")
    return "".join(parts)


def scatter(title, points, x_label, y_label, width=760):
    """points: [(x, y)]. 산점도 + 축 라벨."""
    points = [(x, y) for x, y in points if x is not None and y is not None]
    if len(points) < 3:
        return ""
    xs, ys = [p[0] for p in points], [p[1] for p in points]
    x_max, y_max = max(xs) or 1, max(ys) or 1
    chart_h, chart_w, top, left = 220, width - 90, 28, 66
    height = chart_h + top + 50
    parts = [f'<svg viewBox="0 0 {width} {height}" role="img" aria-label="{esc(title)}">']
    parts.append(f'<text x="{left}" y="16" class="ct">{esc(title)}</text>')
    for frac in (0, 0.5, 1.0):
        y = top + chart_h - chart_h * frac
        parts.append(f'<line x1="{left}" y1="{y:.0f}" x2="{left + chart_w}" y2="{y:.0f}" class="grid"/>')
        parts.append(f'<text x="{left - 6}" y="{y + 4:.0f}" class="ax" text-anchor="end">'
                     f'{y_max * frac:,.0f}</text>')
        x = left + chart_w * frac
        parts.append(f'<text x="{x:.0f}" y="{top + chart_h + 16}" class="ax" '
                     f'text-anchor="middle">{x_max * frac:,.0f}</text>')
    for x, y in points:
        cx = left + chart_w * x / x_max
        cy = top + chart_h - chart_h * y / y_max
        parts.append(f'<circle cx="{cx:.1f}" cy="{cy:.1f}" r="4" fill="{PALETTE[0]}" '
                     f'fill-opacity="0.55"><title>{x:,.0f}, {y:,.0f}</title></circle>')
    parts.append(f'<text x="{left + chart_w / 2:.0f}" y="{height - 6}" class="ax" '
                 f'text-anchor="middle">{esc(x_label)}</text>')
    parts.append(f'<text x="14" y="{top + chart_h / 2:.0f}" class="ax" '
                 f'transform="rotate(-90 14 {top + chart_h / 2:.0f})" '
                 f'text-anchor="middle">{esc(y_label)}</text>')
    parts.append("</svg>")
    return "".join(parts)


def table(headers, rows):
    head = "".join(f"<th>{esc(h)}</th>" for h in headers)
    body = "".join("<tr>" + "".join(f"<td>{esc(c if c is not None else '-')}</td>" for c in r)
                   + "</tr>" for r in rows)
    return f"<table><thead><tr>{head}</tr></thead><tbody>{body}</tbody></table>"


# ---- 트랙별 섹션 ----

def versions_of(perf_dir, track):
    out = []
    for line in (perf_dir / "versions.tsv").read_text(encoding="utf-8").splitlines()[1:]:
        cols = line.split("\t")
        if cols[0] == track:
            out.append((cols[2].removeprefix("perf/"), cols[7]))
    return out


def journey_section(results, versions):
    data = {name: load(results / name / "journey.json") for name, _ in versions}
    data = {k: v for k, v in data.items() if v}
    if not data:
        return ""
    parts = ["<h2>여정 - 이야기 시작부터 종료까지</h2>",
             "<p>클라이언트 관점 구간별 p50. 발화 길이 버킷(short/medium/long)을 "
             "턴마다 순환시켜 입력 길이 의존을 함께 쟀다.</p>"]

    segment_names = ["session_start", "story_advance", "turn", "tts",
                     "pa_start", "pa_order", "pa_retelling", "stardust"]
    labels = list(data.keys())
    series = [(seg, [((data[v]["summary"].get(seg) or {}).get("p50_ms")) for v in labels])
              for seg in segment_names]
    parts.append(grouped_bars("구간별 p50 (ms)", labels, series))

    bucket_series = [(b, [((data[v]["summary"].get(f"turn_{b}") or {}).get("p50_ms"))
                          for v in labels]) for b in ("short", "medium", "long")]
    parts.append(grouped_bars("턴 지연 p50 - 발화 길이 버킷별 (ms)", labels, bucket_series))

    # 최신 버전의 서버 내부 분해와 응답 길이 산점도
    latest = labels[-1]
    turns = [r for r in data[latest]["rows"] if r.get("segment") == "turn"
             and r.get("status") == 200]
    server_turns = [r["server"] for r in turns if r.get("server")]
    if server_turns:
        stages = ["준비", "분석", "판단", "대사", "반영"]
        med = []
        for stage in stages:
            vals = sorted(t.get(stage, 0) for t in server_turns)
            med.append(vals[len(vals) // 2] if vals else None)
        parts.append(grouped_bars(f"턴 내부 구간 중앙값 ({latest}, 서버 TurnTimer)",
                                  stages, [("중앙값", med)]))
    pts = [(r.get("reply_chars"), (r.get("server") or {}).get("대사"))
           for r in turns if r.get("server")]
    chart = scatter(f"캐릭터 응답 길이 vs 대사 구간 ({latest})", pts,
                    "응답 글자 수", "대사 구간 ms")
    if chart:
        parts.append(chart)
    rows = [(v,
             data[v]["summary"].get("turn", {}).get("p50_ms"),
             data[v]["summary"].get("tts", {}).get("p50_ms"),
             data[v]["summary"].get("tts", {}).get("count"),
             data[v]["summary"].get("pa_retelling", {}).get("p50_ms"),
             data[v]["summary"].get("report_ready", {}).get("p50_ms"))
            for v in labels]
    parts.append(table(["버전", "턴 p50", "TTS p50", "TTS 호출 수", "완료(retelling) p50",
                        "리포트 준비 p50"], rows))
    return "".join(parts)


def tts_section(results, versions):
    data = {name: load(results / name / "tts-eval.json") for name, _ in versions}
    data = {k: v for k, v in data.items() if v}
    if not data:
        return ""
    latest = list(data.keys())[-1]
    summary = data[latest]["summary"]
    buckets = ["short", "medium", "long"]
    series = [(c, [(summary.get(f"{b}_{c}") or {}).get("p50_ms") for b in buckets])
              for c in ("miss", "hit")]
    return ("<h2>TTS - 문장 길이와 캐시</h2>"
            + grouped_bars(f"TTS p50 - 길이 버킷 x 캐시 ({latest})", buckets, series))


def stt_section(results, versions):
    parts = []
    labels, hall, latency = [], [], []
    scale_pts = []
    for name, _ in versions:
        data = load(results / name / "stt-eval.json")
        if not data:
            continue
        labels.append(name)
        hall.append(round((data["summary"].get("hallucination_rate") or 0) * 100, 1))
        latency.append(data["summary"].get("latency_p50_ms"))
        for r in data.get("rows", []):
            if r.get("audio_seconds") and r.get("latency_ms"):
                scale_pts.append((r["audio_seconds"], r["latency_ms"]))
    if not labels:
        return ""
    parts.append("<h2>STT - 환각율과 길이 스케일</h2>")
    parts.append(grouped_bars("비발화 환각율 (%)", labels, [("환각율", hall)], unit="%"))
    parts.append(grouped_bars("STT p50 (ms)", labels, [("p50", latency)]))
    chart = scatter("음원 길이 vs STT 지연 (전 버전 합산)", scale_pts, "음원 초", "지연 ms")
    if chart:
        parts.append(chart)
    return "".join(parts)


def k6_section(results, versions):
    rows = []
    for name, desc in versions:
        data = load(results / name / "turn-load.json")
        if not data:
            continue
        m = data.get("metrics", {})

        def val(metric, stat):
            v = m.get(metric, {}).get(stat)
            return round(v, 1) if isinstance(v, (int, float)) else None
        rows.append((name, desc, val("probe_login_duration", "p(95)"),
                     val("probe_home_duration", "p(95)"), val("turn_duration", "p(95)")))
    if not rows:
        return ""
    parts = ["<h2>턴 동시 부하 - 무관 API 영향</h2>"]
    labels = [r[0] for r in rows]
    parts.append(grouped_bars("턴 부하 중 p95 (ms)", labels,
                              [("로그인", [r[2] for r in rows]),
                               ("홈", [r[3] for r in rows])]))
    parts.append(table(["버전", "설명", "로그인 p95", "홈 p95", "턴 p95"], rows))
    return "".join(parts)


def word_section(results, versions):
    rows = []
    for name, desc in versions:
        data = load(results / name / "word-quality.json")
        if not data:
            continue
        s = data["summary"]
        rows.append((name, s.get("latency_p50_ms"), s.get("reject_count"),
                     s.get("over_20chars"), s.get("eojeol_in_range"),
                     s.get("avg_eojeol"), s.get("examples_full")))
    if not rows:
        return ""
    labels = [r[0] for r in rows]
    parts = ["<h2>단어 뜻 생성</h2>"]
    parts.append(grouped_bars("뜻 생성 p50 (ms)", labels, [("p50", [r[1] for r in rows])]))
    parts.append(grouped_bars("품질 (건수)", labels,
                              [("오거절", [r[2] for r in rows]),
                               ("20자 초과", [r[3] for r in rows]),
                               ("5~7어절", [r[4] for r in rows])], unit="건"))
    parts.append(table(["버전", "p50(ms)", "오거절", "20자 초과", "5~7어절", "평균 어절",
                        "예문 3종"], rows))
    return "".join(parts)


STYLE = """
body { font-family: 'Apple SD Gothic Neo', Pretendard, sans-serif; margin: 40px auto;
       max-width: 860px; color: #222; background: #fff; }
h1 { font-size: 26px; } h2 { font-size: 20px; margin-top: 40px;
     border-bottom: 2px solid #eee; padding-bottom: 6px; }
h3 { font-size: 16px; margin-top: 26px; color: #333; }
p, li { color: #444; line-height: 1.7; }
a { color: #4878a8; }
ul.refs { font-size: 12px; } ul.refs li { color: #666; line-height: 1.5; }
svg { display: block; margin: 18px 0; }
svg .ct { font-size: 13px; font-weight: 600; fill: #333; }
svg .ax { font-size: 11px; fill: #777; }
svg .val { font-size: 10px; fill: #444; }
svg .grid { stroke: #eee; }
table { border-collapse: collapse; margin: 14px 0; font-size: 13px; }
th, td { border: 1px solid #ddd; padding: 6px 10px; text-align: right; }
th:first-child, td:first-child { text-align: left; }
.note { background: #f7f7f2; border-left: 4px solid #d0d0c0; padding: 10px 14px;
        font-size: 13px; }
"""


def main():
    parser = argparse.ArgumentParser()
    perf_dir = pathlib.Path(__file__).resolve().parent.parent
    parser.add_argument("--out", default=str(perf_dir / "results" / "REPORT.html"))
    args = parser.parse_args()

    results = perf_dir / "results"
    body = ["<h1>굿퀘스천 성능 재현 측정 보고서</h1>",
            "<p class='note'>현재 환경에서의 재현 측정이다. 당시 운영 수치가 아니며, "
            "방법과 공정성 체크리스트는 perf/README.md에 있다. 여정 비교는 코드와 "
            "이야기 콘텐츠가 함께 변한 총체의 비교라는 점에 주의.</p>"]
    # 측정 회차의 관찰/제안을 담는 자유 서술 조각. 생성기와 분리해 둔다.
    notes = results / "NOTES.html"
    if notes.exists():
        body.append(notes.read_text(encoding="utf-8"))
    body.append(journey_section(results, versions_of(perf_dir, "journey")))
    body.append(tts_section(results, versions_of(perf_dir, "journey")))
    # STT의 버전 이야기(환각율 100% -> 0%)는 stt 트랙에 있다 - 여정 버전은
    # 전부 필터 이후라 개선사가 안 보인다.
    body.append(stt_section(results, versions_of(perf_dir, "stt"))
                or stt_section(results, versions_of(perf_dir, "journey")))
    body.append(k6_section(results, versions_of(perf_dir, "turn")))
    body.append(word_section(results, versions_of(perf_dir, "word")))

    out = pathlib.Path(args.out)
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text("<!doctype html><meta charset='utf-8'>"
                   f"<title>성능 재현 측정</title><style>{STYLE}</style>"
                   + "".join(b for b in body if b), encoding="utf-8")
    print(f"저장: {out}")


if __name__ == "__main__":
    main()
