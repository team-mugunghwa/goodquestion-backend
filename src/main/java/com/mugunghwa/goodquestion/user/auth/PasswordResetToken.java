package com.mugunghwa.goodquestion.user.auth;

import com.mugunghwa.goodquestion.user.parent.Parent;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 비밀번호 재설정 토큰 (계정-06).
 * 원문은 보관하지 않고 해시만 저장한다 — DB가 유출돼도 토큰을 재사용할 수 없게 한다.
 * 1회용이라 회전 대신 소비(consume) 개념을 쓴다.
 */
@Entity
@Table(name = "password_reset_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parent_id", nullable = false)
    private Parent parent;

    @Column(name = "token_hash", nullable = false, unique = true, length = 100)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    /** 소비된 시각. null이면 아직 안 썼다. */
    @Column(name = "consumed_at")
    private OffsetDateTime consumedAt;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private OffsetDateTime createdAt;

    @Builder
    public PasswordResetToken(Parent parent, String tokenHash, OffsetDateTime expiresAt) {
        this.parent = parent;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public void consume() {
        this.consumedAt = OffsetDateTime.now();
    }

    public boolean isUsable() {
        return consumedAt == null && expiresAt.isAfter(OffsetDateTime.now());
    }
}
