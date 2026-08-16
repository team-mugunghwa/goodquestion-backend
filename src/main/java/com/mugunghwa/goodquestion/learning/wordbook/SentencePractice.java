package com.mugunghwa.goodquestion.learning.wordbook;

import com.mugunghwa.goodquestion.user.child.Child;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 예문 따라 말하기 — 보상이 나간 것만 남는다.
 *
 * <p>예문(단어 x 유형)당 최초 1회 지급을 {@code (wordbook_id, sentence_type)} UNIQUE가
 * 보장한다. 하루 상한(2건)에 걸린 날의 성공은 기록하지 않아, 그 예문은 다음 날 다시
 * 성공하면 보상받을 수 있다(word_practices와 같은 방식).
 *
 * <p>{@code spokenText}는 STT가 인식한 발화 텍스트다. <b>음성은 저장하지 않는다</b> —
 * 일치율과 함께 남겨 보호자가 "얼마나 정확히 따라 말했는지" 볼 수 있게 한다.
 */
@Entity
@Table(name = "sentence_practices",
        uniqueConstraints = @UniqueConstraint(columnNames = {"wordbook_id", "sentence_type"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SentencePractice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wordbook_id", nullable = false)
    private Wordbook word;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "child_id", nullable = false)
    private Child child;

    @Enumerated(EnumType.STRING)
    @Column(name = "sentence_type", nullable = false, length = 20)
    private ExampleSentenceType sentenceType;

    @Column(name = "spoken_text", nullable = false, columnDefinition = "text")
    private String spokenText;

    /** 채점된 일치율(0.00~1.00). 지급 기준(0.90 이상)을 통과한 값만 저장된다. */
    @Column(nullable = false, precision = 3, scale = 2)
    private BigDecimal similarity;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private OffsetDateTime createdAt;

    @Builder
    private SentencePractice(Wordbook word, Child child, ExampleSentenceType sentenceType,
                             String spokenText, BigDecimal similarity) {
        this.word = word;
        this.child = child;
        this.sentenceType = sentenceType;
        this.spokenText = spokenText;
        this.similarity = similarity;
    }
}
