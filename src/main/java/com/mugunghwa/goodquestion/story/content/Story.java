package com.mugunghwa.goodquestion.story.content;

import jakarta.persistence.*;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * 이야기 (정적 콘텐츠). 토픽은 story_topics로 분리 관리.
 * 세션에서 사용된 이야기는 직접 수정하지 않고 복사해 새 story_id로 등록한다.
 */
@Entity
@Table(name = "stories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Story {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String summary;

    /** 아이가 맡는 역할 — 상세 화면 표시용 (선택-03) */
    @Column(name = "child_role", length = 50)
    private String childRole;

    /** 도입·상황 소개 — 상세 화면 표시용 (선택-03) */
    @Column(columnDefinition = "text")
    private String intro;

    /** 대표 이미지 (Supabase Storage 경로) */
    @Column(name = "image_url", columnDefinition = "text")
    private String imageUrl;

    @Column(nullable = false, length = 20)
    private String difficulty;

    @Column(name = "estimated_minutes")
    private Short estimatedMinutes;

    /** cards(내용·정답 순서), retelling_keywords 등 후속 활동 설정 */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "post_activity_config", columnDefinition = "jsonb")
    private Map<String, Object> postActivityConfig;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StoryStatus status;

    @Generated(event = EventType.INSERT)
    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private OffsetDateTime createdAt;

    @Builder
    public Story(String title, String summary, String childRole, String intro, String imageUrl,
                 String difficulty, Short estimatedMinutes,
                 Map<String, Object> postActivityConfig, StoryStatus status) {
        this.title = title;
        this.summary = summary;
        this.childRole = childRole;
        this.intro = intro;
        this.imageUrl = imageUrl;
        this.difficulty = difficulty;
        this.estimatedMinutes = estimatedMinutes;
        this.postActivityConfig = postActivityConfig;
        this.status = status != null ? status : StoryStatus.DRAFT;
    }
}
