"""TTS 지연 평가 - 텍스트 길이 의존성과 서버 캐시 효과.

캐릭터 응답은 문장 수만큼 /api/tts가 순차 호출되는 체감 구간이므로,
문장 길이별 지연 곡선과 캐시 적중 시 지연을 따로 잰다.

- 길이 버킷: short(~15자) / medium(~40자) / long(~90자)
- 캐시 회피: 라운드마다 다른 문장을 쓴다 (서버가 voice+text 키로 캐시)
- 캐시 적중: 같은 문장을 두 번 보내 두 번째 지연을 기록한다

사용:
  python3 perf/eval/tts_eval.py --base http://localhost:8106 \
      --out perf/results/journey-v4 --rounds 3
"""

import argparse
import json
import pathlib
import sys

sys.path.insert(0, str(pathlib.Path(__file__).parent))
from gqapi import Api, percentile

# 라운드마다 같은 버킷에서 다른 문장을 골라 캐시를 피한다. 길이는 버킷 안에서
# 거의 같게 맞췄다 - 문장이 다른데 길이까지 다르면 곡선이 흐려진다.
SENTENCES = {
    "short": [
        "방귀를 참으면 안 돼요.",
        "배가 아프면 말해야 해요.",
        "오늘은 정말 신나는 날이야.",
        "며느리가 웃고 있어요.",
        "시원하게 뀌어 보렴.",
    ],
    "medium": [
        "며느리가 방귀를 참아서 배가 아프고 얼굴도 노랗게 변해 버렸어요.",
        "시아버지가 깜짝 놀라서 기왓장이 들썩일 만큼 크게 웃으셨답니다.",
        "참는 것보다 사실대로 말하는 게 훨씬 용감한 일이라고 생각해요.",
        "가마솥에 눌은 누룽지를 온 식구가 나눠 먹으면서 이야기했어요.",
        "친정에 가는 길에 배나무 아래에서 잠깐 쉬었다 가기로 했어요.",
    ],
    "long": [
        "며느리가 방귀를 꾹 참다가 몸이 아파졌지만 용기를 내서 사실대로 말했더니 온 집안이 "
        "들썩일 만큼 큰 방귀가 나왔고 가족들은 놀랐다가 곧 크게 웃으며 이해해 주었어요.",
        "시아버지는 처음에는 화가 난 것처럼 보였지만 며느리의 얼굴이 노랗게 변한 걸 보고는 "
        "몸이 상하는 것보다 시원하게 뀌는 게 낫다며 온 식구를 마당에 불러 모으셨답니다.",
        "장대 같은 방귀 바람에 기왓장이 들썩이고 가마솥 뚜껑이 덜컹거렸지만 며느리는 "
        "오랜만에 배가 편안해져서 눈물이 날 만큼 후련했고 그날부터 참지 않기로 했어요.",
    ],
}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--base", required=True)
    parser.add_argument("--out", required=True)
    parser.add_argument("--rounds", type=int, default=3)
    parser.add_argument("--character", default=None, help="화자 이름 (없으면 기본 화자)")
    args = parser.parse_args()

    api = Api(args.base)
    api.login()

    rows = []
    for round_no in range(1, args.rounds + 1):
        for bucket, sentences in SENTENCES.items():
            text = sentences[(round_no - 1) % len(sentences)]
            status, _, latency = api.request(
                "POST", "/api/tts", {"text": text, "characterName": args.character},
                timeout=45)
            rows.append({"round": round_no, "bucket": bucket, "text_chars": len(text),
                         "cache": "miss", "status": status, "latency_ms": round(latency, 1)})
            print(f"[{round_no}] {bucket}({len(text)}자) miss: {status} {latency:.0f}ms")
            # 같은 문장 재요청 - 서버 캐시 적중 지연
            status, _, latency = api.request(
                "POST", "/api/tts", {"text": text, "characterName": args.character},
                timeout=45)
            rows.append({"round": round_no, "bucket": bucket, "text_chars": len(text),
                         "cache": "hit", "status": status, "latency_ms": round(latency, 1)})
            print(f"[{round_no}] {bucket}({len(text)}자) hit:  {status} {latency:.0f}ms")

    summary = {}
    for bucket in SENTENCES:
        for cache in ("miss", "hit"):
            vals = [r["latency_ms"] for r in rows
                    if r["bucket"] == bucket and r["cache"] == cache and r["status"] == 200]
            if vals:
                summary[f"{bucket}_{cache}"] = {"count": len(vals),
                                                "p50_ms": percentile(vals, 50),
                                                "p95_ms": percentile(vals, 95)}

    out = pathlib.Path(args.out)
    out.mkdir(parents=True, exist_ok=True)
    (out / "tts-eval.json").write_text(
        json.dumps({"summary": summary, "rows": rows}, ensure_ascii=False, indent=2),
        encoding="utf-8")
    print("\n요약:", json.dumps(summary, ensure_ascii=False))


if __name__ == "__main__":
    main()
