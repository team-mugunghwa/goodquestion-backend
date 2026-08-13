package com.mugunghwa.goodquestion.learning.report;

/**
 * 역량 한 항목 (리포트 요건 3-2·3-3). 필드 순서가 곧 화면 표시 순서다(4절).
 *
 * <p>name에는 "관점과 공감"처럼 보호자가 읽을 이름을 넣는다. DECISION·REASON 같은
 * 내부 분석 태그는 화면에 노출하지 않는다(4절).
 */
public record Competency(String name,
                         String finding,
                         String evidenceUtterance,
                         String strength,
                         String nextFocus) {
}