package com.mugunghwa.goodquestion.story.session;

import com.mugunghwa.goodquestion.global.vocab.CharacterEmotion;
import com.mugunghwa.goodquestion.story.content.StoryScene;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 세션 내 모든 발화. 원본 음성 미저장, STT 실패 시 메시지 미생성.
 * character_emotion은 캐릭터 발화에만 저장.
 */
@Entity
@Table(name = "messages",
        uniqueConstraints = @UniqueConstraint(columnNames = {"session_id", "turn_order"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private StorySession session;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scene_id", nullable = false)
    private StoryScene scene;

    @Enumerated(EnumType.STRING)
    @Column(name = "speaker_type", nullable = false, length = 20)
    private SpeakerType speakerType;

    @Column(name = "turn_order", nullable = false)
    private int turnOrder;

    @Column(nullable = false, columnDefinition = "text")
    private String text;

    @Column(name = "stt_raw_text", columnDefinition = "text")
    private String sttRawText;

    /** STT 신뢰도(0~1). 기준값이 아직 미정이라 판정은 애플리케이션이 한다. */
    @Column(name = "stt_confidence", precision = 4, scale = 3)
    private BigDecimal sttConfidence;

    /** 기준값 이하 표시 — true면 리포트 대표 발화 후보에서 제외한다. */
    @Column(name = "stt_low_confidence", nullable = false)
    private boolean sttLowConfidence;

    /** 아이가 다시 말한 횟수 */
    @Column(name = "stt_retry_count", nullable = false)
    private short sttRetryCount;

    /**
     * 캐릭터 표정.
     * TODO: 표정 키는 캐릭터마다 다르므로(characters.expression_keys) AI 파이프라인 연동 시
     *       고정 enum을 걷어내고 문자열 키 + fallback으로 바꾼다. DB check는 이미 풀었다.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "character_emotion", length = 20)
    private CharacterEmotion characterEmotion;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private OffsetDateTime createdAt;

    @Builder
    public Message(StorySession session, StoryScene scene, SpeakerType speakerType,
                   int turnOrder, String text, String sttRawText, BigDecimal sttConfidence,
                   boolean sttLowConfidence, short sttRetryCount, CharacterEmotion characterEmotion) {
        this.session = session;
        this.scene = scene;
        this.speakerType = speakerType;
        this.turnOrder = turnOrder;
        this.text = text;
        this.sttRawText = sttRawText;
        this.sttConfidence = sttConfidence;
        this.sttLowConfidence = sttLowConfidence;
        this.sttRetryCount = sttRetryCount;
        this.characterEmotion = characterEmotion;
    }
}
