package com.mugunghwa.goodquestion.learning.reward.shop;

import com.mugunghwa.goodquestion.user.child.Child;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 보유 아이템 (보상-14, 보상-20).
 * 같은 아이템 중복 구매를 허용하므로 (child, item) 유니크를 두지 않는다.
 * 삭제 경로가 없어 한 번 사면 사라지지 않는다 — 치우기는 배치만 지우고 여기는 남는다(보상-02).
 */
@Entity
@Table(name = "child_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChildItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "child_id", nullable = false)
    private Child child;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(name = "acquired_at", nullable = false, updatable = false, insertable = false)
    private OffsetDateTime acquiredAt;

    @Builder
    public ChildItem(Child child, Item item) {
        this.child = child;
        this.item = item;
    }
}
