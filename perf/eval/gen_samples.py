"""트랙 C - STT 평가용 음원 생성 (16kHz 모노 PCM16 WAV, 표준 라이브러리만).

무음/백색잡음/순음은 "발화가 아닌 입력"이다. 좋은 STT는 빈 텍스트로 걸러야
하고, 여기서 낱말이 튀어나오면 환각(힌트 에코 포함)이다.

실제 아이 발화 샘플은 자동 생성할 수 없다 - perf/eval/samples/real/ 에
"기대문장.wav" 형식(파일명이 곧 정답 전사)으로 직접 녹음해 넣으면
stt_eval.py가 함께 평가한다.
"""

import math
import pathlib
import random
import struct
import wave

RATE = 16000
SECONDS = 2


def write(path, frames):
    with wave.open(str(path), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(frames)
    print(f"생성: {path}")


def main():
    out = pathlib.Path(__file__).parent / "samples"
    out.mkdir(exist_ok=True)
    (out / "real").mkdir(exist_ok=True)

    count = RATE * SECONDS
    write(out / "silence.wav", struct.pack(f"<{count}h", *([0] * count)))

    rng = random.Random(20260816)  # 고정 시드 - 같은 잡음으로 재현 가능해야 한다
    noise = [int(rng.gauss(0, 1500)) for _ in range(count)]
    noise = [max(-32768, min(32767, s)) for s in noise]
    write(out / "noise.wav", struct.pack(f"<{count}h", *noise))

    tone = [int(12000 * math.sin(2 * math.pi * 440 * i / RATE)) for i in range(count)]
    write(out / "tone.wav", struct.pack(f"<{count}h", *tone))


if __name__ == "__main__":
    main()
