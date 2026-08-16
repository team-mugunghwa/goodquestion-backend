package com.mugunghwa.goodquestion.learning.wordbook;

import com.mugunghwa.goodquestion.story.content.StoryScene;
import com.mugunghwa.goodquestion.user.child.Child;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/** 단어장. meaning·exampleSentence는 저장 요청에 없으면 서버가 LLM으로 생성한다(단어-02). */
@Entity
@Table(name = "wordbook",
        uniqueConstraints = @UniqueConstraint(columnNames = {"child_id", "word"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Wordbook {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "child_id", nullable = false)
    private Child child;

    @Column(nullable = false, length = 50)
    private String word;

    /** 생성 전에는 null일 수 있다. */
    @Column(columnDefinition = "text")
    private String meaning;

    @Column(name = "example_sentence", columnDefinition = "text")
    private String exampleSentence;

    /** 일상 예문 - 이야기 밖 쓰임 (V14). */
    @Column(name = "example_daily", length = 300)
    private String exampleDaily;

    /** 심화 예문 - 일상 예문보다 한 단계 어려운 문장 (V14). */
    @Column(name = "example_advanced", length = 300)
    private String exampleAdvanced;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 20)
    private WordEntryType entryType;

    /** 이 단어를 처음 만난 장면 (단어-06) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_scene_id")
    private StoryScene sourceScene;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private OffsetDateTime createdAt;

    @Builder
    public Wordbook(Child child, String word, String meaning, String exampleSentence,
                    String exampleDaily, String exampleAdvanced,
                    WordEntryType entryType, StoryScene sourceScene) {
        this.child = child;
        this.word = word;
        this.meaning = meaning;
        this.exampleSentence = exampleSentence;
        this.exampleDaily = exampleDaily;
        this.exampleAdvanced = exampleAdvanced;
        this.entryType = entryType != null ? entryType : WordEntryType.UNKNOWN;
        this.sourceScene = sourceScene;
    }

    /** 뜻·예문을 LLM으로 생성한 뒤 채운다. */
    public void fillExplanation(String meaning, String exampleSentence) {
        this.meaning = meaning;
        this.exampleSentence = exampleSentence;
    }

    public void changeEntryType(WordEntryType entryType) {
        this.entryType = entryType;
    }
}
