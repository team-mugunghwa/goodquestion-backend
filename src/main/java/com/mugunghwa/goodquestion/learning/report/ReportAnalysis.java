package com.mugunghwa.goodquestion.learning.report;

import java.util.List;

/**
 * 리포트 분석 본문 (리포트 요건 3~7절). reports.analysis에 jsonb로 저장한다.
 *
 * <p>영역별로 없을 수 있다 — 요건이 일부 구성만으로도 성립한다고 열어 두었고,
 * 생성이 부분 실패해도 리포트 전체가 안 열리는 것보다는 낫다.
 */
public record ReportAnalysis(VocabularyAnalysis vocabulary,
                             List<Competency> competencies,
                             RepresentativeUtterance representativeUtterance,
                             HomeGuide homeGuide) {

    /** 분석 본문이 아직 없는 리포트. 컬럼이 not null이라 null 대신 이 값을 넣는다. */
    public static ReportAnalysis empty() {
        return new ReportAnalysis(null, List.of(), null, null);
    }
}