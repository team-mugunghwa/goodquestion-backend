package com.mugunghwa.goodquestion.learning.wordbook;

import com.mugunghwa.goodquestion.user.child.Child;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 단어 말하기 연습 — 보상이 나간 것만 남는다.
 *
 * <p>단어당 최초 1회 지급을 {@code wordbook_id} UNIQUE가 보장한다. 하루 상한(3)에 걸린 날의
 * 성공은 기록하지 않아, 그 단어는 다음 날 다시 성공하면 보상받을 수 있다.
 *
 * <p>{@code spokenText}는 아이가 만든 문장이다. <b>음성은 저장하지 않는다</b> — 보호자가
 * "무슨 문장을 만들었는지" 볼 수 있게 텍스트만 남긴다.
 */
@Entity
@Table(name = "word_practices")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WordPractice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wordbook_id", nullable = false, unique = true)
    private Wordbook word;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "child_id", nullable = false)
    private Child child;

    @Column(name = "spoken_text", nullable = false, columnDefinition = "text")
    private String spokenText;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private OffsetDateTime createdAt;

    @Builder
    private WordPractice(Wordbook word, Child child, String spokenText) {
        this.word = word;
        this.child = child;
        this.spokenText = spokenText;
    }
}
