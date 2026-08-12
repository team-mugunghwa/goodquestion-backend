package com.mugunghwa.goodquestion.learning.reward.shop;

import com.mugunghwa.goodquestion.story.content.Story;
import jakarta.persistence.*;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 아이템 마스터 (보상-09~11). 읽기 전용 콘텐츠로 시드에서 등록한다.
 * 해금 판정은 저장하지 않고 아이 상태로 매번 계산한다 — 해금 테이블을 따로 두지 않는 이유다.
 */
@Entity
@Table(name = "items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ItemCategory category;

    /** 별가루 가격 — 화면에는 아이콘 개수로 표시한다(보상-12). */
    @Column(nullable = false)
    private int price;

    @Enumerated(EnumType.STRING)
    @Column(name = "unlock_type", nullable = false, length = 30)
    private UnlockType unlockType;

    /** unlockType=STORY_COMPLETE일 때 해금 대상 이야기 (동물↔이야기 1:1) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unlock_story_id")
    private Story unlockStory;

    /** unlockType=STARDUST_CUMULATIVE일 때 필요한 누적 획득량 */
    @Column(name = "unlock_stardust_total")
    private Integer unlockStardustTotal;

    @Column(name = "model_url", columnDefinition = "text")
    private String modelUrl;

    @Column(name = "thumbnail_url", columnDefinition = "text")
    private String thumbnailUrl;

    @Column(name = "display_order", nullable = false)
    private short displayOrder;

    /** 운영 중 내리기 — child_items가 FK로 물고 있어 행 삭제가 불가능하므로 상태로 감춘다. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ItemStatus status;

    @Generated(event = EventType.INSERT)
    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private OffsetDateTime createdAt;

    @Builder
    public Item(String name, ItemCategory category, int price, UnlockType unlockType,
                Story unlockStory, Integer unlockStardustTotal,
                String modelUrl, String thumbnailUrl, short displayOrder, ItemStatus status) {
        this.status = status != null ? status : ItemStatus.ACTIVE;
        this.name = name;
        this.category = category;
        this.price = price;
        this.unlockType = unlockType;
        this.unlockStory = unlockStory;
        this.unlockStardustTotal = unlockStardustTotal;
        this.modelUrl = modelUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.displayOrder = displayOrder;
    }
}
