package com.mugunghwa.goodquestion.learning.reward.stardust;

import com.mugunghwa.goodquestion.global.error.BusinessException;
import com.mugunghwa.goodquestion.global.error.ErrorCode;
import com.mugunghwa.goodquestion.user.child.Child;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 별가루 지갑 — 아이당 1개, 아이 생성 시 함께 만든다(계정-14, 보상-07).
 * totalEarned는 누적 해금 판정 기준이라 사용해도 줄지 않는다.
 */
@Entity
@Table(name = "stardust_wallets")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StardustWallet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "child_id", nullable = false, unique = true)
    private Child child;

    @Column(nullable = false)
    private int balance;

    @Column(name = "total_earned", nullable = false)
    private int totalEarned;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private OffsetDateTime createdAt;

    @Builder
    public StardustWallet(Child child) {
        this.child = child;
        this.balance = 0;
        this.totalEarned = 0;
    }

    public void earn(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("지급액은 양수여야 합니다: " + amount);
        }
        this.balance += amount;
        this.totalEarned += amount;
    }

    /** 잔액이 모자라면 409로 막는다(보상-14). */
    public void spend(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("사용액은 양수여야 합니다: " + amount);
        }
        if (balance < amount) {
            throw new BusinessException(ErrorCode.STARDUST_INSUFFICIENT);
        }
        this.balance -= amount;
    }
}
