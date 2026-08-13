package com.mugunghwa.goodquestion.learning.report;

import com.mugunghwa.goodquestion.story.session.StorySession;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** 보호자 리포트 (세션당 1건).  */
@Entity
@Table(name = "reports")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false, unique = true)
    private StorySession session;

    @Column(nullable = false, columnDefinition = "text")
    private String summary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<ReportItem> strengths;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "next_focus", nullable = false, columnDefinition = "jsonb")
    private List<ReportItem> nextFocus;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private ReportAnalysis analysis;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private OffsetDateTime createdAt;

    @Builder
    public Report(StorySession session, String summary,
                  List<ReportItem> strengths, List<ReportItem> nextFocus,
                  ReportAnalysis analysis) {
        this.session = session;
        this.summary = summary;
        this.strengths = strengths;
        this.nextFocus = nextFocus;
        // 요건이 일부 영역만 구성해도 성립한다고 열어 두었고, 생성이 부분 실패해도
        // 리포트 전체가 안 열리는 것보다 낫다. 시드 데모 리포트에도 이 값이 없다.
        this.analysis = analysis != null ? analysis : ReportAnalysis.empty();
    }
}
