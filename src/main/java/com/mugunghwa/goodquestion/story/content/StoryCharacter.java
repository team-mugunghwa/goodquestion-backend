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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 캐릭터 레지스트리 (정적 콘텐츠).
 *
 * <p>장면에 흩어져 있던 캐릭터 속성을 한 곳으로 모은다. 특히 TTS 화자 고정이 여기 걸린다 —
 * 페르소나가 장면마다 따로 있으면 같은 캐릭터가 장면별로 다른 목소리로 합성되는 것을
 * 막을 방법이 없다. 장면에 따라 달라지는 것(입장·요소 기준·남은 걱정)만 StoryScene에 남긴다.
 *
 * <p>클래스명이 Character가 아닌 이유는 java.lang.Character와 충돌하기 때문이다.
 */
@Entity
@Table(name = "characters",
        uniqueConstraints = @UniqueConstraint(columnNames = {"story_id", "character_key"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoryCharacter {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "story_id", nullable = false)
    private Story story;

    /**
     * 표정 이미지 파일명의 키 — {characterKey}_{expression}.png.
     * 임의로 바꾸면 런타임 이미지 조회가 깨진다.
     */
    @Column(name = "character_key", nullable = false, length = 64)
    private String characterKey;

    /** 화면 표시 이름 */
    @Column(nullable = false, length = 50)
    private String name;

    /** 성격·말투 — 캐릭터 LLM 페르소나 */
    @Column(nullable = false, columnDefinition = "text")
    private String personality;

    /** 유도를 어떻게 드러낼지 — GUIDED 모드 대사 생성 입력 */
    @Column(name = "guidance_style", columnDefinition = "text")
    private String guidanceStyle;

    @Column(name = "tts_voice", length = 64)
    private String ttsVoice;

    /**
     * Gemini 계열 연기 지시문. 보이스 이름이 성별을 보장하지 않으므로
     * 성별·연령을 반드시 포함한다.
     */
    @Column(name = "tts_style", columnDefinition = "text")
    private String ttsStyle;

    /** 성별 검증 기대값. null이면 검사하지 않는다. */
    @Enumerated(EnumType.STRING)
    @Column(name = "tts_gender", length = 10)
    private TtsGender ttsGender;

    /** 이 캐릭터가 실제로 가진 표정. 없는 표정을 요구하면 fallback으로 내린다. */
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "expression_keys", nullable = false, columnDefinition = "text[]")
    private List<String> expressionKeys = new ArrayList<>();

    @Generated(event = EventType.INSERT)
    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private OffsetDateTime createdAt;

    @Builder
    public StoryCharacter(Story story, String characterKey, String name, String personality,
                          String guidanceStyle, String ttsVoice, String ttsStyle,
                          TtsGender ttsGender, List<String> expressionKeys) {
        this.story = story;
        this.characterKey = characterKey;
        this.name = name;
        this.personality = personality;
        this.guidanceStyle = guidanceStyle;
        this.ttsVoice = ttsVoice;
        this.ttsStyle = ttsStyle;
        this.ttsGender = ttsGender;
        this.expressionKeys = expressionKeys != null ? expressionKeys : new ArrayList<>();
    }

    /** 요청한 표정을 이 캐릭터가 가지고 있는지 — 없으면 호출부가 fallback을 고른다. */
    public boolean hasExpression(String expressionKey) {
        return expressionKeys.contains(expressionKey);
    }
}
