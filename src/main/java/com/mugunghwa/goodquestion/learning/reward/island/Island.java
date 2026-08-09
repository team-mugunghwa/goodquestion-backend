package com.mugunghwa.goodquestion.learning.reward.island;

import com.mugunghwa.goodquestion.user.child.Child;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/** 아이의 섬 — 아이당 1개, 아이 생성 시 함께 만든다(계정-14, 보상-15~16). */
@Entity
@Table(name = "islands")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Island {

    /** 화면에는 "내 행성"으로 보이지만 도메인·코드는 Island로 통일한다. */
    public static final String DEFAULT_NAME = "내 행성";
    private static final short DEFAULT_GRID = 8;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "child_id", nullable = false, unique = true)
    private Child child;

    @Column(nullable = false, length = 30)
    private String name;

    @Column(name = "grid_width", nullable = false)
    private short gridWidth;

    @Column(name = "grid_height", nullable = false)
    private short gridHeight;

    /** 최초 진입 시 배치 1회 따라 하기를 마쳤는지(보상-22) */
    @Column(name = "tutorial_completed", nullable = false)
    private boolean tutorialCompleted;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private OffsetDateTime createdAt;

    @Builder
    public Island(Child child) {
        this.child = child;
        this.name = DEFAULT_NAME;
        this.gridWidth = DEFAULT_GRID;
        this.gridHeight = DEFAULT_GRID;
        this.tutorialCompleted = false;
    }

    public void rename(String name) {
        this.name = name;
    }

    public void completeTutorial() {
        this.tutorialCompleted = true;
    }

    /** 격자 범위를 벗어난 좌표는 422로 막는다. */
    public boolean contains(int gridX, int gridY) {
        return gridX >= 0 && gridX < gridWidth && gridY >= 0 && gridY < gridHeight;
    }
}
