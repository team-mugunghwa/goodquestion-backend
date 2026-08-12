package com.mugunghwa.goodquestion.story.dialogue;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 턴 처리 구간별 소요 시간 수집기.
 *
 * <p>이 파이프라인에서 느린 곳은 하나뿐이지만(외부 LLM 호출) 그것을 숫자로 갖고 있지 않으면
 * "느리다"에서 더 나아갈 수가 없다. 구간을 나눠 재 두면 DB 작업과 외부 호출의 비중이 드러나고,
 * 트랜잭션 경계를 옮겼을 때 무엇이 달라졌는지 같은 기준으로 비교할 수 있다.
 *
 * <p>스레드 하나가 턴 하나를 처리하는 동안만 살아 있는 객체라 동기화하지 않는다.
 */
final class TurnTimer {

    private final long startedAt = System.nanoTime();
    private final Map<String, Long> stageMillis = new LinkedHashMap<>();

    private long lastMarkAt = startedAt;

    static TurnTimer start() {
        return new TurnTimer();
    }

    /** 직전 구분점부터 지금까지를 하나의 구간으로 기록한다. */
    void mark(String stage) {
        long now = System.nanoTime();
        stageMillis.merge(stage, millisBetween(lastMarkAt, now), Long::sum);
        lastMarkAt = now;
    }

    /**
     * 로그 한 줄로 남길 요약.
     *
     * <p>아이 발화 원문이나 캐릭터 대사는 담지 않는다. 시간을 보려고 남기는 로그에 대화 내용이
     * 딸려 들어가면 그때부터 로그 보관 기간이 개인정보 문제가 된다.
     *
     * <p>기록하지 않은 구간이 남을 수 있어(예외로 중간에 빠져나온 경우) 총합은 따로 잰다.
     */
    String summary() {
        String breakdown = stageMillis.entrySet().stream()
                .map(stage -> "%s %dms".formatted(stage.getKey(), stage.getValue()))
                .collect(Collectors.joining(", "));
        long total = millisBetween(startedAt, System.nanoTime());

        return breakdown.isEmpty()
                ? "총 %dms".formatted(total)
                : "총 %dms (%s)".formatted(total, breakdown);
    }

    private long millisBetween(long fromNanos, long toNanos) {
        return Duration.ofNanos(toNanos - fromNanos).toMillis();
    }
}
