package com.mugunghwa.goodquestion.learning.reward.planet;

import com.mugunghwa.goodquestion.user.child.Child;
import jakarta.persistence.*;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/** 아이의 행성 — 아이당 1개, 아이 생성 시 함께 만든다(계정-14, 보상-15~16). */
@Entity
@Table(name = "planets")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Planet {

    public static final String DEFAULT_NAME = "내 행성";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "child_id", nullable = false, unique = true)
    private Child child;

    @Column(nullable = false, length = 30)
    private String name;

    /** 최초 진입 시 배치 1회 따라 하기를 마쳤는지(보상-22) */
    @Column(name = "tutorial_completed", nullable = false)
    private boolean tutorialCompleted;

    @Generated(event = EventType.INSERT)
    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private OffsetDateTime createdAt;

    @Builder
    public Planet(Child child) {
        this.child = child;
        this.name = DEFAULT_NAME;
        this.tutorialCompleted = false;
    }

    public void rename(String name) {
        this.name = name;
    }

    public void completeTutorial() {
        this.tutorialCompleted = true;
    }
}
