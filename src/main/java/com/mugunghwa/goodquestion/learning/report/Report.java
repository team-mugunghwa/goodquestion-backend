package com.mugunghwa.goodquestion.learning.report;

import com.mugunghwa.goodquestion.session.session.StorySession;
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

/** 보호자 리포트 (세션당 1건). 대표 발화는 저장하지 않고 조회 시 messages에서 구성. */
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

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private OffsetDateTime createdAt;

    @Builder
    public Report(StorySession session, String summary,
                  List<ReportItem> strengths, List<ReportItem> nextFocus) {
        this.session = session;
        this.summary = summary;
        this.strengths = strengths;
        this.nextFocus = nextFocus;
    }
}
