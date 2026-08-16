package com.mugunghwa.goodquestion.story.content;

import com.mugunghwa.goodquestion.global.vocab.ThinkingElement;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
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

    /**
     * 장면 영상 (스토리지 경로). null이면 image_url만 쓴다.
     *
     * <p>이미지를 대체하지 않고 얹는다 - 영상을 못 받거나 재생에 실패하면 image_url로
     * 떨어져야 한다. 저사양 기기와 데이터 절약 모드가 실제로 그렇게 된다.
     *
     * <p>반복 재생 여부는 컬럼으로 두지 않았다. scene_type이 이미 그 정보를 담고 있어서다 -
     * STORY는 낭독이 끝나면 멈추고, DIALOGUE는 아이가 말하는 동안 계속 돌아야 한다.
     */
    @Column(name = "video_url")
    private String videoUrl;

    /**
     * 캐릭터 참조. characterName은 화면 표시용으로 남기고
     * 페르소나·보이스·표정은 이 참조로 찾는다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "character_id")
    private StoryCharacter character;

    @Column(name = "character_name", length = 50)
    private String characterName;

    /**
     * 장면별 입장 — 같은 캐릭터라도 장면마다 입장이 다르다
     * (시아버지가 대화2에서는 내치려 하고 전개4에서는 후회한다).
     */
    @Column(name = "scene_stance", columnDefinition = "text")
    private String sceneStance;

    /** STT 디코딩 힌트. 아동 발화는 고유명사 오인식이 가장 많다 */
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "proper_nouns", nullable = false, columnDefinition = "text[]")
    private List<String> properNouns = new ArrayList<>();

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
    public StoryScene(Story story, short sceneOrder, SceneType sceneType,
                      String sceneDescription, String conflict, String imageUrl, String videoUrl,
                      StoryCharacter character, String characterName, String sceneStance,
                      List<String> properNouns,
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
        this.videoUrl = videoUrl;
        this.character = character;
        this.characterName = characterName;
        this.sceneStance = sceneStance;
        this.properNouns = properNouns != null ? properNouns : new ArrayList<>();
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

    /**
     * 아직 확인되지 않은 필수 요소. 저장하지 않고 (필수 - 누적)으로 매번 계산한다(진행-04).
     *
     * <p>선언 순서를 그대로 유지한다 - 이 순서가 곧 장면의 기본 유도 우선순위다.
     * 진행 판단, 유도 대상 선택, 진행 상태 응답이 모두 이 한 곳을 쓴다.
     *
     * @param accumulatedElements 세션에 누적된 요소 이름(ThinkingElement.name())
     */
    public List<ThinkingElement> missingElements(List<String> accumulatedElements) {
        List<String> accumulated = accumulatedElements != null ? accumulatedElements : List.of();
        return getRequiredElementTypes().stream()
                .filter(element -> !accumulated.contains(element.name()))
                .toList();
    }

    /** 유도 대상 요소에 대응하는 캐릭터의 남은 걱정 */
    public String getRemainingWorry(ThinkingElement element) {
        return remainingWorries.get(element.name());
    }
}
