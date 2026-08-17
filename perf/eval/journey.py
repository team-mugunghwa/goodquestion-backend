"""여정 트랙 - 이야기 시작부터 종료까지 구간별 실측.

아이 한 명이 이야기 하나를 처음부터 끝까지 노는 흐름을 클라이언트 관점에서
그대로 밟으며, 모든 HTTP 호출을 구간 이름표를 붙여 기록한다. 서버가 재는
구간은 턴 하나뿐(TurnTimer)이라 클라이언트 계측이 기본이고, 턴 내부
분해(준비/분석/판단/대사/반영)는 측정 후 server.log를 파싱해 합류시킨다.

구간(segment):
  session_start   세션 생성 (DB)
  story_advance   스토리 장면 넘기기 (DB)
  scene_fetch     세션/장면 상태 조회 (DB, 복구용 호출 포함)
  turn            발화 제출 = 분석 LLM + 캐릭터 LLM. 발화 길이 버킷 기록
  tts             캐릭터 응답 문장별 합성 - 클라이언트가 문장 수만큼 순차
                  호출하는 실제 경로 그대로 (숨은 체감 구간)
  pa_start/pa_order/pa_retelling   후속 활동. retelling이 완료 트랜잭션
  stardust        지갑 조회
  report_ready    비동기 리포트가 준비될 때까지 폴링한 시간 (아이가 기다리는
                  시간은 아니므로 별도 지표)

발화 길이 의존성: 턴마다 짧은/중간/긴 발화를 돌아가며 보내고(short/medium/
long 버킷), 캐릭터 응답의 글자 수와 문장 수를 함께 기록한다 - 응답(출력)
길이가 대사 구간을 지배한다는 가설을 데이터로 확인하기 위해서다.

주의: 실제 LLM/TTS 호출이 나간다. 한 라운드에 대략 턴 8~14회 x LLM 2회
+ TTS 문장 수만큼. --skip-tts로 TTS를 뺄 수 있다.

사용:
  python3 perf/eval/journey.py --base http://localhost:8106 \
      --out perf/results/journey-v4 --rounds 3 \
      --server-log perf/results/journey-v4/server.log \
      --container gq-perf-journey-v4
"""

import argparse
import json
import pathlib
import re
import subprocess
import sys
import time

sys.path.insert(0, str(pathlib.Path(__file__).parent))
from gqapi import Api, DEMO_CHILD_ID, percentile

STORY_ID = "11111111-1111-1111-1111-111111111111"

# 발화 길이 버킷. 이야기(방귀 뀌는 며느리) 맥락에 맞는 문장이라 분석 LLM이
# 요소를 찾을 수 있고, 장면이 자연스럽게 진행된다. 전 버전에 같은 문장을 써야
# 비교가 성립하므로 바꾸지 말 것.
UTTERANCES = {
    "short": "방귀를 참으면 안 돼요",
    "medium": "며느리가 방귀를 참아서 배가 아프고 얼굴도 노래졌으니까 참지 말라고 말해 주고 싶어요",
    "long": ("며느리가 시댁 식구들한테 미움받을까 봐 방귀를 꾹 참았는데 그래서 배가 점점 아프고 "
             "얼굴색도 변했어요. 몸이 아픈 것보다 창피한 게 나으니까 시아버지한테 사실대로 말하고 "
             "시원하게 방귀를 뀌는 게 좋다고 생각해요"),
}
BUCKET_ORDER = ["short", "medium", "long"]

RETELLING_TEXT = ("며느리가 방귀를 참다가 병이 났는데 사실대로 말하고 방귀를 뀌었더니 "
                  "집이 들썩였지만 마음이 편해졌고 가족들도 웃으면서 이해해 줬어요")

TURN_LOG = re.compile(r"턴 처리 sessionId=(\S+) 총 (\d+)ms(?: \((.*)\))?")


class Journey:
    def __init__(self, api, rows, skip_tts):
        self.api = api
        self.rows = rows
        self.skip_tts = skip_tts
        self.turn_index = 0
        self.vendor_calls = 0

    def record(self, segment, method, path, payload=None, extra=None, timeout=60):
        status, body, latency = self.api.request(method, path, payload, timeout=timeout)
        row = {"segment": segment, "status": status, "latency_ms": round(latency, 1)}
        if extra:
            row.update(extra)
        self.rows.append(row)
        try:
            return status, json.loads(body)
        except ValueError:
            return status, {}

    # ---- 구간들 ----

    def start_session(self):
        status, data = self.record("session_start", "POST",
                                   f"/api/children/{DEMO_CHILD_ID}/sessions",
                                   {"storyId": STORY_ID})
        if status not in (200, 201):
            raise RuntimeError(f"세션 시작 실패 {status}: {data}")
        return data

    def fetch_session(self, session_id):
        _, data = self.record("scene_fetch", "GET", f"/api/sessions/{session_id}")
        return data

    def advance_story(self, session_id):
        status, data = self.record("story_advance", "POST",
                                   f"/api/sessions/{session_id}/scenes/current/story-complete")
        return status, data

    def submit_turn(self, session_id):
        bucket = BUCKET_ORDER[self.turn_index % len(BUCKET_ORDER)]
        self.turn_index += 1
        text = UTTERANCES[bucket]
        status, data = self.record(
            "turn", "POST", f"/api/sessions/{session_id}/utterances",
            {"text": text},
            extra={"bucket": bucket, "utterance_chars": len(text)}, timeout=40)
        if status == 200:
            self.vendor_calls += 2  # 분석 + 캐릭터 (고정 대사 턴은 1이지만 상한으로 센다)
            reply = ((data.get("characterMessage") or {}).get("text") or "")
            self.rows[-1]["reply_chars"] = len(reply)
            self.rows[-1]["reply_sentences"] = len(split_sentences(reply))
            self.synthesize_reply(data)
        return status, data

    def synthesize_reply(self, turn_data):
        """클라이언트 경로 그대로 - 응답 문장 수만큼 /api/tts를 순차 호출한다."""
        if self.skip_tts:
            return
        character = None
        message = turn_data.get("characterMessage") or {}
        # 캐릭터 이름이 응답에 없으면 장면 것을 쓸 수 없어 기본 화자로 간다
        for key in ("characterName", "speakerName"):
            if message.get(key):
                character = message[key]
        texts = []
        closing = turn_data.get("closingReaction") or {}
        if closing.get("text"):
            texts.append(closing["text"])
        if message.get("text"):
            texts.extend(split_sentences(message["text"]))
        for sentence in texts:
            status, _ = self.record("tts", "POST", "/api/tts",
                                    {"text": sentence, "characterName": character},
                                    extra={"text_chars": len(sentence)}, timeout=45)
            if status == 200:
                self.vendor_calls += 1

    def post_activity(self, session_id, container):
        status, data = self.record("pa_start", "POST",
                                   f"/api/sessions/{session_id}/post-activity/start")
        if status != 200:
            return False
        cards = [c.get("cardId") for c in data.get("cards", [])]
        order = correct_card_order(container) or cards
        status, data = self.record("pa_order", "POST",
                                   f"/api/sessions/{session_id}/post-activity/order",
                                   {"submittedOrder": order})
        if status == 200 and not data.get("correct", False):
            # 정답을 DB에서 못 얻었을 때의 한 번뿐인 대안 - 받은 순서 그대로
            self.record("pa_order", "POST",
                        f"/api/sessions/{session_id}/post-activity/order",
                        {"submittedOrder": cards})
        status, _ = self.record("pa_retelling", "POST",
                                f"/api/sessions/{session_id}/post-activity/retelling",
                                {"text": RETELLING_TEXT})
        return status == 200

    def wait_report(self, session_id, limit_s=90):
        """비동기 리포트 준비 시간. 아이의 체감이 아니라 보호자 화면의 지표다."""
        t0 = time.perf_counter()
        while time.perf_counter() - t0 < limit_s:
            status, _, _ = self.api.request("GET", f"/api/sessions/{session_id}/report")
            if status == 200:
                elapsed = round((time.perf_counter() - t0) * 1000, 1)
                self.rows.append({"segment": "report_ready", "status": 200,
                                  "latency_ms": elapsed})
                return
            time.sleep(2)
        self.rows.append({"segment": "report_ready", "status": 0, "latency_ms": None})


def split_sentences(text):
    parts = re.split(r"(?<=[.!?])\s+", text.strip())
    return [p for p in parts if p]


def correct_card_order(container):
    """정답 카드 순서는 응답에 없다(의도) - 콘텐츠의 correct_order를 DB에서 읽는다."""
    if not container:
        return None
    try:
        out = subprocess.run(
            ["docker", "exec", container, "psql", "-U", "postgres", "-d", "goodquestion",
             "-t", "-A", "-c",
             f"select post_activity_config from stories where id = '{STORY_ID}';"],
            check=True, capture_output=True, text=True).stdout.strip()
        config = json.loads(out)
        cards = sorted(config.get("cards", []), key=lambda c: c.get("correct_order", 0))
        return [c.get("id") or c.get("cardId") for c in cards] or None
    except (subprocess.CalledProcessError, ValueError, KeyError):
        return None


def run_round(api, rows, container, skip_tts, skip_report):
    journey = Journey(api, rows, skip_tts)
    t_round = time.perf_counter()
    data = journey.start_session()
    session_id = data.get("sessionId")
    scene_type = (data.get("currentScene") or {}).get("sceneType")
    phase = data.get("phase")
    completed = False

    for _ in range(40):  # 안전 상한
        if phase == "POST_ACTIVITY":
            completed = journey.post_activity(session_id, container)
            break
        if phase in ("COMPLETED", "STOPPED"):
            completed = True
            break
        if scene_type == "STORY":
            status, data = journey.advance_story(session_id)
            if status != 200:
                break
            scene_type = (data.get("currentScene") or {}).get("sceneType")
            phase = data.get("phase") or phase
            continue
        if scene_type == "DIALOGUE":
            moved = False
            for _ in range(8):  # 장면당 턴 상한 + 여유
                status, data = journey.submit_turn(session_id)
                if status == 409:
                    break  # MAX_TURNS 등 - 상태를 다시 읽는다
                if status != 200:
                    break
                transition = data.get("sceneTransition") or {}
                if transition:
                    target = transition.get("next")
                    if target == "SCENE":
                        scene_type = transition.get("nextSceneType")
                        moved = True
                        break
                    if target in ("POST_ACTIVITY", "COMPLETED"):
                        phase = target
                        moved = True
                        break
            if not moved:
                state = journey.fetch_session(session_id)
                scene_type = (state.get("currentScene") or {}).get("sceneType")
                phase = state.get("phase")
                if state.get("status") in ("COMPLETED", "STOPPED"):
                    completed = True
                    break
            continue
        # 알 수 없는 상태 - 다시 읽고 그래도 모르면 중단
        state = journey.fetch_session(session_id)
        scene_type = (state.get("currentScene") or {}).get("sceneType")
        phase = state.get("phase")
        if not scene_type and not phase:
            break

    journey.record("stardust", "GET", f"/api/children/{DEMO_CHILD_ID}/stardust")
    if completed and not skip_report:
        journey.wait_report(session_id)
    if not completed:
        api.request("POST", f"/api/sessions/{session_id}/stop")

    return {
        "session_id": session_id,
        "completed": completed,
        "wall_ms": round((time.perf_counter() - t_round) * 1000, 1),
        "vendor_calls": journey.vendor_calls,
    }


def join_server_log(log_path, rows):
    """server.log의 TurnTimer 줄을 세션별로 모아 턴 행에 순서대로 합류시킨다."""
    if not log_path or not pathlib.Path(log_path).exists():
        return
    by_session = {}
    for line in open(log_path, encoding="utf-8", errors="replace"):
        m = TURN_LOG.search(line)
        if not m:
            continue
        stages = {}
        if m.group(3):
            for part in m.group(3).split(", "):
                name, ms = part.rsplit(" ", 1)
                stages[name] = int(ms.removesuffix("ms"))
        by_session.setdefault(m.group(1), []).append(
            {"server_total_ms": int(m.group(2)), **stages})
    # 서버는 실패한 제출(409 등)에도 finally에서 한 줄을 남기므로, 클라이언트의
    # 모든 턴 시도를 세어야 순서가 어긋나지 않는다. 붙이는 건 200 행에만 한다.
    counters = {}
    for row in rows:
        if row.get("segment") != "turn":
            continue
        sid = row.get("session_id")
        i = counters.get(sid, 0)
        turns = by_session.get(sid, [])
        if row.get("status") == 200 and i < len(turns):
            row["server"] = turns[i]
        counters[sid] = i + 1


def summarize(rows):
    segments = {}
    for row in rows:
        segments.setdefault(row["segment"], []).append(row)
    out = {}
    for name, seg_rows in segments.items():
        latencies = [r["latency_ms"] for r in seg_rows
                     if r.get("latency_ms") is not None and r["status"] in (0, 200, 201)]
        out[name] = {
            "count": len(seg_rows),
            "p50_ms": percentile(latencies, 50),
            "p95_ms": percentile(latencies, 95),
        }
    # 발화 길이 버킷별 턴 지연
    for bucket in BUCKET_ORDER:
        vals = [r["latency_ms"] for r in segments.get("turn", [])
                if r.get("bucket") == bucket and r["status"] == 200]
        if vals:
            out[f"turn_{bucket}"] = {"count": len(vals),
                                     "p50_ms": percentile(vals, 50),
                                     "p95_ms": percentile(vals, 95)}
    return out


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--base", required=True)
    parser.add_argument("--out", required=True)
    parser.add_argument("--rounds", type=int, default=3)
    parser.add_argument("--container", default=None, help="카드 정답 조회용 DB 컨테이너")
    parser.add_argument("--server-log", default=None, help="TurnTimer 합류용 server.log 경로")
    parser.add_argument("--skip-tts", action="store_true")
    parser.add_argument("--skip-report", action="store_true")
    args = parser.parse_args()

    api = Api(args.base)
    api.login()

    rows, rounds_meta = [], []
    for round_no in range(1, args.rounds + 1):
        start_index = len(rows)
        meta = run_round(api, rows, args.container, args.skip_tts, args.skip_report)
        for row in rows[start_index:]:
            row["round"] = round_no
            row["session_id"] = meta["session_id"]
        rounds_meta.append(meta)
        print(f"[라운드 {round_no}] 완료={meta['completed']} 벽시계={meta['wall_ms']}ms "
              f"호출 {len(rows) - start_index}건 (벤더 {meta['vendor_calls']}회)")

    join_server_log(args.server_log, rows)

    summary = summarize(rows)
    out = pathlib.Path(args.out)
    out.mkdir(parents=True, exist_ok=True)
    (out / "journey.json").write_text(
        json.dumps({"summary": summary, "rounds": rounds_meta, "rows": rows},
                   ensure_ascii=False, indent=2), encoding="utf-8")
    print("\n구간 요약:")
    for name, stat in sorted(summary.items()):
        print(f"  {name}: n={stat['count']} p50={stat['p50_ms']}ms p95={stat['p95_ms']}ms")


if __name__ == "__main__":
    main()
