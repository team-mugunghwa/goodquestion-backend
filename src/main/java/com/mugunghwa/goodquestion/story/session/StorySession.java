package com.mugunghwa.goodquestion.story.session;

import com.mugunghwa.goodquestion.global.vocab.ResponseMode;
import com.mugunghwa.goodquestion.global.vocab.ThinkingElement;
import com.mugunghwa.goodquestion.story.content.StoryScene;
import com.mugunghwa.goodquestion.story.content.Story;
import com.mugunghwa.goodquestion.user.child.Child;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 이야기 진행 기록 (런타임 상태). 진행 판단 모듈이 참조하는 누적 상태를 보관한다.
 * missing_elements는 저장하지 않고 required - accumulated로 계산한다.
 */
@Entity
@Table(name = "story_sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StorySession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "child_id", nullable = false)
    private Child child;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "story_id", nullable = false)
    private Story story;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_scene_id")
    private StoryScene currentScene;

    @Column(name = "current_child_turn_count", nullable = false)
    private short currentChildTurnCount;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "accumulated_elements", nullable = false, columnDefinition = "text[]")
    private List<String> accumulatedElements = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "last_detected_elements", nullable = false, columnDefinition = "text[]")
    private List<String> lastDetectedElements = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "last_response_mode", length = 20)
    private ResponseMode lastResponseMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_guidance_target", length = 20)
    private ThinkingElement lastGuidanceTarget;

    @Column(name = "turns_without_new_element", nullable = false)
    private short turnsWithoutNewElement;

    @Column(name = "consecutive_low_information_turns", nullable = false)
    private short consecutiveLowInformationTurns;

    @Column(name = "scene_goal_met", nullable = false)
    private boolean sceneGoalMet;

    @Enumerated(EnumType.STRING)
    @Column(name = "scene_end_reason", length = 20)
    private SceneEndReason sceneEndReason;

    /**
     * 현재 장면에서 유도 모드가 한 번이라도 발생했는지 (진행-18).
     * 유도 없이 목표를 통과하면 별가루 장면 보너스 대상이 된다 (보상-04).
     */
    @Column(name = "guided_used_in_scene", nullable = false)
    private boolean guidedUsedInScene;

    /** 현재 장면에서 미션이 노출되었는지 (재노출 방지) */
    @Column(name = "mission_exposed", nullable = false)
    private boolean missionExposed;

    /** 현재 장면의 미션이 완료되었는지 (미션 필수 장면의 종료 조건) */
    @Column(name = "mission_completed", nullable = false)
    private boolean missionCompleted;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SessionStatus status;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "last_activity_at", nullable = false)
    private OffsetDateTime lastActivityAt;

    @Builder
    public StorySession(Child child, Story story, StoryScene currentScene) {
        this.child = child;
        this.story = story;
        this.currentScene = currentScene;
        this.status = SessionStatus.IN_PROGRESS;
        this.startedAt = OffsetDateTime.now();
        this.lastActivityAt = OffsetDateTime.now();
    }

    // ----- 진행 판단·파이프라인에서 사용하는 상태 전이 메서드 -----

    /** 아이 발화 1턴 반영 — TODO: 누적 요소·정체 카운터 갱신 로직 */
    public void applyTurn(List<String> newElements, boolean lowInformation,
                          ResponseMode mode, ThinkingElement guidanceTarget) {
        this.currentChildTurnCount++;
        this.lastDetectedElements = newElements;
        this.lastResponseMode = mode;
        this.lastGuidanceTarget = guidanceTarget;
        // TODO: accumulatedElements 병합(중복 제거), turnsWithoutNewElement / consecutiveLowInformationTurns 갱신
        touch();
    }

    public void closeScene(SceneEndReason reason, boolean goalMet) {
        this.sceneEndReason = reason;
        this.sceneGoalMet = goalMet;
        touch();
    }

    /** 장면 이동 — 장면 단위 누적 상태 초기화 */
    public void moveToScene(StoryScene nextScene) {
        this.currentScene = nextScene;
        this.currentChildTurnCount = 0;
        this.accumulatedElements = new ArrayList<>();
        this.lastDetectedElements = new ArrayList<>();
        this.turnsWithoutNewElement = 0;
        this.consecutiveLowInformationTurns = 0;
        this.guidedUsedInScene = false;
        this.sceneGoalMet = false;
        this.sceneEndReason = null;
        this.missionExposed = false;
        this.missionCompleted = false;
        touch();
    }

    /** 유도 모드가 발생하면 기록한다. 장면 보너스 자격은 이 값이 false일 때만 생긴다. */
    public void markGuidanceUsed() { this.guidedUsedInScene = true; touch(); }

    /** 장면 보너스 자격 — 유도 없이 목표를 달성하고 끝났는가 (보상-04). */
    public boolean isSceneBonusEligible() {
        return !guidedUsedInScene && sceneGoalMet;
    }

    public void exposeMission() { this.missionExposed = true; touch(); }

    public void completeMission() { this.missionCompleted = true; touch(); }

    public void toPostActivity() { this.status = SessionStatus.POST_ACTIVITY; touch(); }

    public void complete() {
        this.status = SessionStatus.COMPLETED;
        this.completedAt = OffsetDateTime.now();
        touch();
    }

    public void stop() { this.status = SessionStatus.STOPPED; touch(); }

    public void touch() { this.lastActivityAt = OffsetDateTime.now(); }

    public boolean isInProgress() { return status == SessionStatus.IN_PROGRESS; }

    /**
     * 플레이 단계 파생 — 저장하지 않고 상태와 현재 장면 유형에서 계산한다.
     * 프론트가 화면을 고르는 기준이라 서버가 단일 근거로 내려준다.
     */
    public PlayPhase resolvePhase() {
        return switch (status) {
            case COMPLETED, STOPPED -> PlayPhase.ENDED;
            case POST_ACTIVITY -> PlayPhase.POST_ACTIVITY;
            case IN_PROGRESS -> (currentScene != null && currentScene.isDialogue())
                    ? PlayPhase.DIALOGUE : PlayPhase.STORY;
        };
    }
}
