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


def noise_frames(seconds, seed):
    rng = random.Random(seed)  # 고정 시드 - 같은 잡음으로 재현 가능해야 한다
    count = RATE * seconds
    samples = [max(-32768, min(32767, int(rng.gauss(0, 1500)))) for _ in range(count)]
    return struct.pack(f"<{count}h", *samples)


def main():
    out = pathlib.Path(__file__).parent / "samples"
    out.mkdir(exist_ok=True)
    (out / "real").mkdir(exist_ok=True)

    count = RATE * SECONDS
    write(out / "silence.wav", struct.pack(f"<{count}h", *([0] * count)))
    write(out / "noise.wav", noise_frames(SECONDS, 20260816))
    tone = [int(12000 * math.sin(2 * math.pi * 440 * i / RATE)) for i in range(count)]
    write(out / "tone.wav", struct.pack(f"<{count}h", *tone))

    # STT 지연의 음원 길이 스케일 측정용. 내용은 같은 성질의 잡음이고 길이만
    # 다르다 - 벤더 처리 시간이 길이에 비례하는지 본다.
    write(out / "noise_5s.wav", noise_frames(5, 20260817))
    write(out / "noise_10s.wav", noise_frames(10, 20260818))


if __name__ == "__main__":
    main()
