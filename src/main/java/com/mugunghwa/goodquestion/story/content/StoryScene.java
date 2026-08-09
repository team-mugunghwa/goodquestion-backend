package com.mugunghwa.goodquestion.story.content;

import com.mugunghwa.goodquestion.global.vocab.ThinkingElement;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 장면 (정적 콘텐츠).
 * scene_type이 STORY면 내레이션 장면(대화 필드 null), DIALOGUE면 대화 장면(대화 필드 필수 — DB check로 보장).
 * required_elements는 장면 내 여러 발화에 걸쳐 누적 확인하는 목표 요소.
 * character_closing은 고정 마지막 대사 사용으로 확정 (콘텐츠 문서 공통 처리 규칙).
 */
@Entity
@Table(name = "story_scenes",
        uniqueConstraints = @UniqueConstraint(columnNames = {"story_id", "scene_order"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoryScene {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "story_id", nullable = false)
    private Story story;

    @Column(name = "scene_order", nullable = false)
    private short sceneOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "scene_type", nullable = false, length = 20)
    private SceneType sceneType;

    /** STORY: 내레이션 본문 / DIALOGUE: 장면 상황·대화 맥락 (분석 LLM sceneContext) */
    @Column(name = "scene_description", nullable = false, columnDefinition = "text")
    private String sceneDescription;

    @Column(columnDefinition = "text")
    private String conflict;

    /** 장면 이미지 (Supabase Storage 경로) */
    @Column(name = "image_url", columnDefinition = "text")
    private String imageUrl;

    @Column(name = "character_name", length = 50)
    private String characterName;

    /** 캐릭터 성격·상태 설명 — 캐릭터 LLM 입력. 장면별로 두어 이야기 진행에 따른 변화 반영 */
    @Column(name = "character_persona", columnDefinition = "text")
    private String characterPersona;

    @Column(name = "character_opening", columnDefinition = "text")
    private String characterOpening;

    /** 고정 마지막 대사. 최대 턴 도달 시에도 LLM 짧은 반응 후 이 대사를 재생 */
    @Column(name = "character_closing", columnDefinition = "text")
    private String characterClosing;

    @Column(name = "scene_goal", columnDefinition = "text")
    private String sceneGoal;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "required_elements", columnDefinition = "text[]")
    private List<String> requiredElements;

    /** 장면별 요소 인정 기준 — 분석 LLM 입력(elementCriteria) */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "element_criteria", nullable = false, columnDefinition = "jsonb")
    private Map<String, String> elementCriteria;

    /** 요소별 캐릭터의 남은 걱정 — 유도 시 캐릭터 LLM에 전달(remainingWorries) */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "remaining_worries", nullable = false, columnDefinition = "jsonb")
    private Map<String, String> remainingWorries;

    /** 이야기 내 미션 설정 (목적·노출 조건·확인 요소). 미션 없는 장면은 null */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "mission_config", columnDefinition = "jsonb")
    private Map<String, Object> missionConfig;

    @Column(name = "preferred_turns")
    private Short preferredTurns;

    @Column(name = "max_turns")
    private Short maxTurns;

    @Builder
    public StoryScene(Story story, short sceneOrder, SceneType sceneType, String sceneDescription,
                      String conflict, String imageUrl, String characterName, String characterPersona,
                      String characterOpening, String characterClosing, String sceneGoal,
                      List<String> requiredElements, Map<String, String> elementCriteria,
                      Map<String, String> remainingWorries, Map<String, Object> missionConfig,
                      Short preferredTurns, Short maxTurns) {
        this.story = story;
        this.sceneOrder = sceneOrder;
        this.sceneType = sceneType;
        this.sceneDescription = sceneDescription;
        this.conflict = conflict;
        this.imageUrl = imageUrl;
        this.characterName = characterName;
        this.characterPersona = characterPersona;
        this.characterOpening = characterOpening;
        this.characterClosing = characterClosing;
        this.sceneGoal = sceneGoal;
        this.requiredElements = requiredElements;
        this.elementCriteria = elementCriteria != null ? elementCriteria : Map.of();
        this.remainingWorries = remainingWorries != null ? remainingWorries : Map.of();
        this.missionConfig = missionConfig;
        this.preferredTurns = preferredTurns;
        this.maxTurns = maxTurns;
    }

    public boolean isDialogue() {
        return sceneType == SceneType.DIALOGUE;
    }

    public boolean hasMission() {
        return missionConfig != null;
    }

    public List<ThinkingElement> getRequiredElementTypes() {
        if (requiredElements == null) return List.of();
        return requiredElements.stream().map(ThinkingElement::valueOf).toList();
    }

    /** 유도 대상 요소에 대응하는 캐릭터의 남은 걱정 */
    public String getRemainingWorry(ThinkingElement element) {
        return remainingWorries.get(element.name());
    }
}
