package com.mugunghwa.goodquestion.learning.report;

/**
 * 대표 발화 1건과 선정 이유 (리포트 요건 5절).
 *
 * <p>조회할 때마다 다시 고르지 않고 저장한다. 선정 이유를 LLM이 쓰므로 매번 만들면
 * 같은 리포트가 열 때마다 달라진다. 리포트는 그 회차의 스냅샷이어야 한다.
 */
public record RepresentativeUtterance(String text, String reason) {
}