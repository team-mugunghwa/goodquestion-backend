package com.mugunghwa.goodquestion.learning.report.dto;

/**
 * 6각 그래프 축 1개 — 명세: claude/보호자리포트_6축그래프_설계안_D6.md 4장.
 *
 * <p>{@code active == false}(이 세션에서 목표로 삼지 않은 축)면
 * {@code score}·{@code hits}·{@code required}·{@code previousScore}·{@code evidence}가
 * 모두 {@code null}이다. 화면은 이때 0점이 아니라 "측정 안 함"으로 표시해야 한다 —
 * 0으로 보이면 보호자가 "못한 축"으로 오독한다.
 *
 * @param axis           {@link com.mugunghwa.goodquestion.global.vocab.ReportAxis} enum 이름
 * @param label          보호자 화면에 보여줄 축 이름 (예: "이유대기")
 * @param description    보호자 화면에 보여줄 축 설명 한 줄
 * @param active         이번 세션(이야기)에서 목표로 삼은 축인지
 * @param score          0~100, active==false면 null
 * @param hits           후처리를 통과한 발화 수, active==false면 null
 * @param required       세션 내 목표 등장 횟수, active==false면 null
 * @param previousScore  같은 아이의 직전 완료 세션 최대 3회 평균 점수 — 해당 축이 활성인 회차가
 *                       하나도 없으면 null (예: 1회차)
 * @param evidence       이 축이 확인된 아이 발화 원문 한 줄, 없으면 null
 */
public record AxisScoreResponse(
        String axis,
        String label,
        String description,
        boolean active,
        Integer score,
        Integer hits,
        Integer required,
        Integer previousScore,
        String evidence
) {
}
