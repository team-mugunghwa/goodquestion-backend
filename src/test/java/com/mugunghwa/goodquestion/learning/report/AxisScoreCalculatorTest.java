package com.mugunghwa.goodquestion.learning.report;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 순수 함수라 리포지토리·세션 없이 검증한다.
 * 공식: claude/보호자리포트_6축그래프_설계안_D6.md 2장.
 */
class AxisScoreCalculatorTest {

    @Test
    void 목표를_전부_채우면_만점에_가깝다() {
        // coverage = 3/3 = 1.0, depth = 3/6 = 0.5 → 100*(0.7*1 + 0.3*0.5) = 85
        assertThat(AxisScoreCalculator.score(3, 3)).isEqualTo(85);
    }

    @Test
    void 목표의_두_배를_채우면_만점이다() {
        // coverage = 1.0(상한), depth = 6/6 = 1.0 → 100
        assertThat(AxisScoreCalculator.score(6, 3)).isEqualTo(100);
    }

    @Test
    void 한_번도_확인되지_않으면_0점이다() {
        assertThat(AxisScoreCalculator.score(0, 3)).isZero();
    }

    @Test
    void 목표보다_적게_채우면_커버리지만큼만_반영된다() {
        // coverage = 1/3, depth = 1/6 → 100*(0.7*0.333.. + 0.3*0.1666..) = 100*(0.2333+0.05) = round(28.33) = 28
        assertThat(AxisScoreCalculator.score(1, 3)).isEqualTo(28);
    }

    @Test
    void required가_0이면_예외를_던진다() {
        // req==0인 축은 "측정 안 함"이라 호출 전에 걸러야 한다 — 여기까지 오면 호출부 버그다.
        assertThatThrownBy(() -> AxisScoreCalculator.score(0, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
