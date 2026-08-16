package com.mugunghwa.goodquestion.learning.wordbook;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/** 예문 따라 말하기 채점 - 스프링 컨텍스트 없는 순수 단위 테스트. */
class SentenceSimilarityTest {

    @Test
    void 완전히_같으면_1이다() {
        assertThat(SentenceSimilarity.score("가마솥에 누룽지가 눌었어요", "가마솥에 누룽지가 눌었어요"))
                .isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    void 띄어쓰기와_문장부호_차이는_점수를_깎지_않는다() {
        assertThat(SentenceSimilarity.score("가마솥에 누룽지가 눌었어요.", "가마 솥에 누룽지가 눌었어요!"))
                .isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    void 한_글자_틀리면_그만큼만_깎인다() {
        // 정규화하면 12자, 1자 치환 -> 1 - 1/12 = 0.9166... -> 0.92
        assertThat(SentenceSimilarity.score("가마솥에 누룽지가 눌었어요", "가마솥에 누룽지가 눌었어유"))
                .isEqualByComparingTo(new BigDecimal("0.92"));
    }

    @Test
    void 절반쯤_다르면_점수도_절반쯤이다() {
        assertThat(SentenceSimilarity.score("가마솥에 누룽지가 눌었어요", "가마솥에 밥을 지었어요"))
                .isLessThan(new BigDecimal("0.90"));
    }

    @Test
    void 전혀_다른_말이면_0에_가깝다() {
        assertThat(SentenceSimilarity.score("가마솥에 누룽지가 눌었어요", "오늘 날씨 참 좋다"))
                .isLessThan(new BigDecimal("0.30"));
    }

    @Test
    void 빈_발화는_0이다() {
        assertThat(SentenceSimilarity.score("가마솥에 누룽지가 눌었어요", "   "))
                .isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(SentenceSimilarity.score("가마솥에 누룽지가 눌었어요", null))
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    /** 목표보다 길게 말한 경우도 긴 쪽 기준으로 깎인다 - 덧붙인 말도 "그대로"는 아니다. */
    @Test
    void 목표보다_길게_말하면_넘치는_만큼_깎인다() {
        assertThat(SentenceSimilarity.score("배가 고파요", "배가 고파요 정말 많이 고파요"))
                .isLessThan(new BigDecimal("0.90"));
    }
}
