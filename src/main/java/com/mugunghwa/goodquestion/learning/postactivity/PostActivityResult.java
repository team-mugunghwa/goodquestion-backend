package com.mugunghwa.goodquestion.learning.postactivity;

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

/** 말하기 후 활동 최종 결과 (세션당 1건). 시도별 과정은 저장하지 않는다. */
@Entity
@Table(name = "post_activity_results")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostActivityResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false, unique = true)
    private StorySession session;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "submitted_order", columnDefinition = "text[]")
    private List<String> submittedOrder;

    @Column(name = "is_order_correct")
    private Boolean isOrderCorrect;

    @Column(name = "attempt_count", nullable = false)
    private short attemptCount;

    @Column(name = "retelling_text", columnDefinition = "text")
    private String retellingText;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Builder
    public PostActivityResult(StorySession session) {
        this.session = session;
        this.attemptCount = 0;
    }

    public void submitOrder(List<String> order, boolean correct) {
        this.submittedOrder = order;
        this.isOrderCorrect = correct;
        this.attemptCount++;
    }

    public void completeRetelling(String retellingText) {
        this.retellingText = retellingText;
        this.completedAt = OffsetDateTime.now();
    }
}
