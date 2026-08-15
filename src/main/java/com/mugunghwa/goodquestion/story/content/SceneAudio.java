package com.mugunghwa.goodquestion.story.content;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.UUID;

/**
 * 사전 렌더 음성 (정적 콘텐츠).
 *
 * <p>고정 대사는 매번 합성할 이유가 없다. 미리 렌더해 두면 벤더 과금과 왕복 지연이 사라지고,
 * 무엇보다 <b>문장별 실측 시각</b>이 남아 자막·화면 전환을 글자수 비례 추정이 아니라
 * 실제 낭독 시각에 맞출 수 있다.
 *
 * <p>읽기 전용이다 — 행을 만드는 것은 콘텐츠 시드(R__1_seed_content.sql)의 몫이고,
 * 서버는 재생할 것을 찾아 주기만 한다.
 */
@Entity
@Table(name = "scene_audio")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SceneAudio {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scene_id", nullable = false)
    private StoryScene scene;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SceneAudioSlot slot;

    /** 아이 이름이 들어가는 대사만 아이별로 렌더한다. null이면 공용 음성. */
    @Column(name = "child_id")
    private UUID childId;

    /** 정적 리소스 기준 상대 경로(예: {@code tts/banggui/sc_banggui_01.mp3}). 앞의 슬래시는 없다. */
    @Column(name = "storage_path", nullable = false, columnDefinition = "text")
    private String storagePath;

    /**
     * 렌더 원본 텍스트의 SHA-256(소문자 16진).
     *
     * <p>대사를 고쳤을 때 화면엔 새 문장, 스피커엔 옛 문장인 상태를 잡는 유일한 수단이다.
     * 재생 전에 지금 내보내는 문장의 해시와 대조해, 어긋나면 이 음성을 쓰지 않는다.
     */
    // 스키마에서 유일한 char(N) 컬럼이라 JDBC 타입을 명시해야 한다. 기본값이면 Hibernate가
    // varchar로 기대해 ddl-auto=validate가 기동을 막는다
    // ("found [bpchar (Types#CHAR)], but expecting [... (Types#VARCHAR)]").
    // columnDefinition만 적는 것으로는 부족하다 - 타입 이름만 바뀌고 JDBC 타입 코드는 그대로다.
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "text_hash", nullable = false, length = 64)
    private String textHash;

    @Column(nullable = false, length = 64)
    private String engine;

    @Column(nullable = false, length = 64)
    private String voice;

    @Column(name = "duration_ms", nullable = false)
    private int durationMs;

    /** 문장별 실측 시작·끝(초). 문장마다 따로 합성해 이어 붙이며 잰 값이다. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "sentence_timings", nullable = false, columnDefinition = "jsonb")
    private List<SentenceTiming> sentenceTimings;

    /** 한 문장의 실측 구간. 자막과 화면 전환이 이 값에 붙는다. */
    public record SentenceTiming(int index, String text, double start, double end) {
    }

    /**
     * 재생 가능한 URL.
     *
     * <p>{@code storage_path}는 앞 슬래시 없는 상대 경로로 심겨 있고(예: {@code tts/banggui/...}),
     * 정적 리소스는 루트에서 서빙된다. 이미지가 {@code /stories/...}로 저장돼 있는 것과 모양을 맞춘다.
     *
     * <p>조립 규칙을 쓰는 곳이 셋(장면 콘텐츠·캐릭터 메시지·해석기)이라 여기 하나만 둔다.
     */
    public String url() {
        return storagePath.startsWith("/") || storagePath.startsWith("http")
                ? storagePath : "/" + storagePath;
    }
}
