package com.mugunghwa.goodquestion.ai.stt;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/** 저신뢰 판정 경계. 기준값 0.5(확정값)를 기본값으로 검증한다. */
class SttConfidencePolicyTest {

    private final SttConfidencePolicy policy = new SttConfidencePolicy(new BigDecimal("0.5"));

    @Test
    void 기준값_미만이면_낮음이다() {
        assertThat(policy.isLow(new BigDecimal("0.499"))).isTrue();
        assertThat(policy.isLow(new BigDecimal("0.420"))).isTrue();
    }

    @Test
    void 기준값_이상이면_낮음이_아니다() {
        assertThat(policy.isLow(new BigDecimal("0.5"))).isFalse();
        assertThat(policy.isLow(new BigDecimal("0.880"))).isFalse();
    }

    /** 낮음(0)과 모름(null)은 다르다. 신뢰도를 못 받은 발화를 걸러내면 안 된다. */
    @Test
    void 신뢰도가_없으면_낮음으로_보지_않는다() {
        assertThat(policy.isLow(null)).isFalse();
    }
}
