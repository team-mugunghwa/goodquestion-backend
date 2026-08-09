package com.mugunghwa.goodquestion.learning.reward.island;

import com.mugunghwa.goodquestion.learning.reward.shop.ChildItem;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 격자 배치 (보상-16~17).
 *
 * <p>한 칸에 하나(island, x, y 유니크)와 보유 아이템 하나는 한 곳에만(childItem 유니크)을
 * DB 제약으로 보장한다 — 겹침을 코드로 막지 않는 이유다(보상-02).
 * 치우기는 이 행을 지우는 것이고 ChildItem은 남는다 = 보관함 복귀.
 */
@Entity
@Table(name = "island_items",
        uniqueConstraints = @UniqueConstraint(columnNames = {"island_id", "grid_x", "grid_y"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IslandItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "island_id", nullable = false)
    private Island island;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "child_item_id", nullable = false, unique = true)
    private ChildItem childItem;

    @Column(name = "grid_x", nullable = false)
    private short gridX;

    @Column(name = "grid_y", nullable = false)
    private short gridY;

    @Column(name = "placed_at", nullable = false, updatable = false, insertable = false)
    private OffsetDateTime placedAt;

    @Builder
    public IslandItem(Island island, ChildItem childItem, short gridX, short gridY) {
        this.island = island;
        this.childItem = childItem;
        this.gridX = gridX;
        this.gridY = gridY;
    }

    /** 옮기기 — 같은 섬 안에서 좌표만 바꾼다. */
    public void moveTo(short gridX, short gridY) {
        this.gridX = gridX;
        this.gridY = gridY;
    }
}
