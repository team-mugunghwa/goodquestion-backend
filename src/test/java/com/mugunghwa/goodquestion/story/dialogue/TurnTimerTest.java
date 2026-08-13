package com.mugunghwa.goodquestion.story.dialogue;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** 턴 소요 시간 요약의 단위 테스트. 로그 한 줄이 성능 비교의 기준이라 형식이 어긋나면 안 된다. */
class TurnTimerTest {

    @Test
    void 구간을_기록한_순서대로_요약한다() {
        TurnTimer timer = TurnTimer.start();
        timer.mark("저장");
        timer.mark("분석");

        assertThat(timer.summary()).matches("총 \\d+ms \\(저장 \\d+ms, 분석 \\d+ms\\)");
    }

    @Test
    void 같은_구간을_여러_번_기록하면_합산한다() {
        TurnTimer timer = TurnTimer.start();
        timer.mark("분석");
        timer.mark("분석");

        assertThat(timer.summary()).matches("총 \\d+ms \\(분석 \\d+ms\\)");
    }

    /** 예외로 구간을 하나도 못 남기고 빠져나온 경우. 총 시간만이라도 남아야 한다. */
    @Test
    void 기록한_구간이_없어도_총_시간은_남는다() {
        assertThat(TurnTimer.start().summary()).matches("총 \\d+ms");
    }
}
