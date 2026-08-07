package com.mugunghwa.goodquestion.learning.wordbook;

import com.mugunghwa.goodquestion.story.scene.StoryScene;
import com.mugunghwa.goodquestion.user.child.Child;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/** 단어장. meaning·example_sentence는 저장 시 서버가 LLM으로 생성. */
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

    @Column(nullable = false, columnDefinition = "text")
    private String meaning;

    @Column(name = "example_sentence", columnDefinition = "text")
    private String exampleSentence;

    @Column(name = "is_favorite", nullable = false)
    private boolean favorite;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_scene_id")
    private StoryScene sourceScene;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private OffsetDateTime createdAt;

    @Builder
    public Wordbook(Child child, String word, String meaning,
                    String exampleSentence, StoryScene sourceScene) {
        this.child = child;
        this.word = word;
        this.meaning = meaning;
        this.exampleSentence = exampleSentence;
        this.sourceScene = sourceScene;
        this.favorite = false;
    }

    public void toggleFavorite() { this.favorite = !this.favorite; }
}
