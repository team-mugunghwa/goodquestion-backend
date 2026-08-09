package com.mugunghwa.goodquestion.learning.reward.stardust;

import com.mugunghwa.goodquestion.learning.reward.shop.Item;
import com.mugunghwa.goodquestion.story.content.StoryScene;
import com.mugunghwa.goodquestion.story.session.StorySession;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 별가루 증감 이력 (보상-07~08).
 *
 * <p>amount는 지급 +, 사용 −. (session, reason) 유니크가 지급 멱등을 DB에서 보장한다(데이터-06).
 * acknowledged=false면 행성 진입 시 떨어지는 연출 대상이다.
 */
@Entity
@Table(name = "stardust_transactions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StardustTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wallet_id", nullable = false)
    private StardustWallet wallet;

    @Column(nullable = false)
    private int amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StardustReason reason;

    /** 지급 근거 세션. 구매는 null. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private StorySession session;

    /**
     * 장면 보너스의 대상 장면. 완주 보상·구매는 null.
     * 장면 보너스는 장면마다 최대 1회라 멱등 판정에 장면까지 필요하다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scene_id")
    private StoryScene scene;

    /** 구매 대상 아이템. 지급은 null. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    private Item item;

    @Column(nullable = false)
    private boolean acknowledged;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private OffsetDateTime createdAt;

    @Builder
    public StardustTransaction(StardustWallet wallet, int amount, StardustReason reason,
                               StorySession session, StoryScene scene, Item item) {
        this.wallet = wallet;
        this.amount = amount;
        this.reason = reason;
        this.session = session;
        this.scene = scene;
        this.item = item;
        // 사용 이력은 연출 대상이 아니므로 바로 확인 처리한다
        this.acknowledged = amount < 0;
    }

    public void acknowledge() {
        this.acknowledged = true;
    }
}
