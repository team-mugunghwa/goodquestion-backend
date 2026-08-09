package com.mugunghwa.goodquestion.story.dialogue;

import com.mugunghwa.goodquestion.global.vocab.ChildIntent;
import com.mugunghwa.goodquestion.global.vocab.UtteranceValidity;
import com.mugunghwa.goodquestion.story.session.Message;
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
 * 아이 메시지 1건의 발화 분석 결과 (1:1).
 * 분석 LLM은 intent/mainPoint/elements/validity만 제안, 진행 판단은 서버 규칙으로 확정.
 */
@Entity
@Table(name = "utterance_analyses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UtteranceAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "message_id", nullable = false, unique = true)
    private Message message;

    @Enumerated(EnumType.STRING)
    @Column(name = "child_intent", nullable = false, length = 20)
    private ChildIntent childIntent;

    @Column(name = "main_point", columnDefinition = "text")
    private String mainPoint;

    /** 서버 후처리를 통과한 요소만 저장 */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "detected_elements", nullable = false, columnDefinition = "jsonb")
    private List<DetectedElement> detectedElements;

    @Enumerated(EnumType.STRING)
    @Column(name = "utterance_validity", nullable = false, length = 20)
    private UtteranceValidity utteranceValidity;

    @Column(name = "analysis_version", nullable = false, length = 30)
    private String analysisVersion;

    /**
     * 분석에 사용한 LLM 식별자. analysisVersion만으로는 같은 프롬프트를 모델만 바꿔 돌린
     * 경우를 구분할 수 없다. 소급이 안 되는 값이라 처음부터 남긴다.
     */
    @Column(name = "model_id", length = 64)
    private String modelId;

    /** 후처리에서 폐기된 근거 — 분석 LLM이 없는 요소를 만들어내는 빈도 추적용 */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "dropped_evidence", nullable = false, columnDefinition = "jsonb")
    private List<DetectedElement> droppedEvidence = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private OffsetDateTime createdAt;

    @Builder
    public UtteranceAnalysis(Message message, ChildIntent childIntent, String mainPoint,
                             List<DetectedElement> detectedElements,
                             UtteranceValidity utteranceValidity, String analysisVersion,
                             String modelId, List<DetectedElement> droppedEvidence) {
        this.message = message;
        this.childIntent = childIntent;
        this.mainPoint = mainPoint;
        this.detectedElements = detectedElements;
        this.utteranceValidity = utteranceValidity;
        this.analysisVersion = analysisVersion != null ? analysisVersion : "mvp_v1";
        this.modelId = modelId;
        this.droppedEvidence = droppedEvidence != null ? droppedEvidence : new ArrayList<>();
    }
}
