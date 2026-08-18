package com.mugunghwa.goodquestion.learning.report;

/**
 * 6각 그래프 축 점수 계산 — 순수 함수, LLM·저장소 의존 없음(단위 테스트 대상).
 *
 * <p>설계: claude/보호자리포트_6축그래프_설계안_D6.md 2장.
 * {@code req == 0}인 축은 이 클래스를 호출하지 않는다 — 그 축은 "측정 안 함"이며
 * 점수가 없는 것이지 0점이 아니다. 호출 전에 {@code required > 0}을 확인해야 한다.
 */
public final class AxisScoreCalculator {

    private AxisScoreCalculator() {
    }

    /**
     * @param hits     후처리(evidence 원문 검증)를 통과한, 해당 축에 매핑된 요소가 확인된 발화 수
     * @param required 세션 내 전 장면의 required_elements 중 해당 축 매핑 요소의 등장 횟수 (> 0)
     * @return 0~100 사이 점수
     */
    public static int score(int hits, int required) {
        if (required <= 0) {
            throw new IllegalArgumentException("required는 0보다 커야 합니다 — req==0 축은 호출 전에 걸러야 합니다.");
        }
        double coverage = Math.min(1.0, hits / (double) required);
        double depth = Math.min(1.0, hits / (double) (required * 2));
        return (int) Math.round(100 * (0.7 * coverage + 0.3 * depth));
    }
}
