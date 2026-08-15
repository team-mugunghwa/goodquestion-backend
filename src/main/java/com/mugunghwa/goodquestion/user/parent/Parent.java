package com.mugunghwa.goodquestion.user.parent;

import com.mugunghwa.goodquestion.user.auth.AuthProvider;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 보호자 계정. Supabase Auth를 사용하지 않아 서버가 직접 발급·관리한다.
 * provider=LOCAL은 email+passwordHash로, 소셜 계정은 provider+providerId로 식별한다.
 */
@Entity
@Table(name = "parents")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Parent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(length = 255)
    private String email;

    @Column(name = "password_hash", length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuthProvider provider;

    @Column(name = "provider_id", length = 100)
    private String providerId;

    @Column(nullable = false, length = 50)
    private String name;

    /** 연속 실패 횟수. 성공 시 0으로 돌아간다. */
    @Column(name = "failed_login_attempts", nullable = false)
    private short failedLoginAttempts;

    /** 잠금 해제 시각. null이거나 과거면 로그인할 수 있다. */
    @Column(name = "locked_until")
    private OffsetDateTime lockedUntil;

    @Column(name = "last_login_ip", length = 45)
    private String lastLoginIp;

    /**
     * 계정 상태. 관리자 콘솔이 바꾼다(admin-goodquestion-backend).
     *
     * <p>여기서는 읽기만 한다. 정지된 계정은 로그인이 거부되고, 이미 발급된
     * 리프레시 토큰도 관리자 쪽에서 함께 끊긴다. 액세스 토큰은 만료(30분)까지 남는다.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ParentStatus status;

    @Column(name = "suspended_at")
    private OffsetDateTime suspendedAt;

    @Column(name = "suspended_reason", columnDefinition = "text")
    private String suspendedReason;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private OffsetDateTime createdAt;

    @Builder
    public Parent(String email, String passwordHash, AuthProvider provider, String providerId, String name) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.provider = provider;
        this.providerId = providerId;
        this.name = name;
        this.status = ParentStatus.ACTIVE;
    }

    public static Parent ofLocal(String email, String passwordHash, String name) {
        return Parent.builder()
                .email(email).passwordHash(passwordHash).provider(AuthProvider.LOCAL).name(name).build();
    }

    public static Parent ofKakao(String providerId, String email, String name) {
        return Parent.builder()
                .providerId(providerId).email(email).provider(AuthProvider.KAKAO).name(name).build();
    }

    public static Parent ofGoogle(String providerId, String email, String name) {
        return Parent.builder()
                .providerId(providerId).email(email).provider(AuthProvider.GOOGLE).name(name).build();
    }

    public void updateName(String name) {
        this.name = name;
    }

    /** 비밀번호 재설정(계정-06)에서 쓴다. 소셜 계정은 애초에 비밀번호가 없어 바꿀 대상이 없다. */
    public void updatePassword(String passwordHash) {
        if (!isLocal()) {
            throw new IllegalStateException("소셜 로그인 계정은 비밀번호를 변경할 수 없습니다.");
        }
        this.passwordHash = passwordHash;
    }

    public boolean isLocal() {
        return provider == AuthProvider.LOCAL;
    }

    /** 관리자가 막은 계정. 로그인 실패 잠금(lockedUntil)과는 다른 사유다. */
    public boolean isSuspended() {
        return status == ParentStatus.SUSPENDED;
    }


    private static final int LOCK_THRESHOLD = 5;
    private static final Duration BASE_LOCK = Duration.ofMinutes(15);
    private static final Duration MAX_LOCK = Duration.ofHours(24);

    /**
     * 로그인 실패 기록. 5회부터 잠그고 이후 실패마다 잠금 시간을 2배로 늘린다.
     *
     * <p>고정 24시간으로 두면 이메일만 아는 사람이 남의 계정을 하루 동안 막을 수 있다.
     * 정상 사용자는 15분만 기다리면 되고, 계속 두드리는 쪽만 시간이 길어진다.
     */
    public void recordLoginFailure() {
        this.failedLoginAttempts++;
        if (failedLoginAttempts < LOCK_THRESHOLD) {
            return;
        }
        int over = Math.min(failedLoginAttempts - LOCK_THRESHOLD, 10);   // 시프트 오버플로 방지
        Duration lock = BASE_LOCK.multipliedBy(1L << over);
        this.lockedUntil = OffsetDateTime.now()
                .plus(lock.compareTo(MAX_LOCK) > 0 ? MAX_LOCK : lock);
    }

    public void recordLoginSuccess(String clientIp) {
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
        this.lastLoginIp = clientIp;
    }

    public boolean isLocked() {
        return lockedUntil != null && lockedUntil.isAfter(OffsetDateTime.now());
    }
}
