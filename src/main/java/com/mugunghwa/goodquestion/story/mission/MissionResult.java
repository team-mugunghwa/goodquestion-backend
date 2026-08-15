package com.mugunghwa.goodquestion.story.mission;

import com.mugunghwa.goodquestion.story.content.StoryScene;
import com.mugunghwa.goodquestion.story.session.StorySession;
import jakarta.persistence.*;
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
 * 미션 수행 결과 — 세션·미션당 1건(미션-11).
 * result는 미션1이 4요소 답, 미션2가 카드별 장점 문장이다.
 *
 * <p><b>현재 아무도 쓰지 않는다(2026-08-15).</b> 미션 제출이 발화 경로
 * (utterances + missionId)로 확정되면서 별도 결과 제출 API가 제거됐고, 발화
 * 경로는 답을 한 문장으로 합쳐 messages에 저장하므로 이 구조화 저장을 타지
 * 않는다. 리포트에 질문별 답을 보여줄 계획이 없다는 것도 확인했다. 스키마
 * 변경의 배포 리스크를 피하려고 테이블과 함께 남겨 둔다 - 구조화 저장이
 * 필요해지면 발화 요청에 선택 필드를 얹고 여기 저장하는 것이 현 설계와 맞다.
 */
@Entity
@Table(name = "mission_results",
        uniqueConstraints = @UniqueConstraint(columnNames = {"session_id", "mission_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MissionResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private StorySession session;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scene_id", nullable = false)
    private StoryScene scene;

    /** mission_1 · mission_2 — 장면의 mission_config가 정의한 식별자 */
    @Column(name = "mission_id", nullable = false, length = 30)
    private String missionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "mission_type", nullable = false, length = 30)
    private MissionType missionType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> result;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private OffsetDateTime createdAt;

    @Builder
    public MissionResult(StorySession session, StoryScene scene, String missionId,
                         MissionType missionType, Map<String, Object> result) {
        this.session = session;
        this.scene = scene;
        this.missionId = missionId;
        this.missionType = missionType;
        this.result = result != null ? result : Map.of();
    }
}
