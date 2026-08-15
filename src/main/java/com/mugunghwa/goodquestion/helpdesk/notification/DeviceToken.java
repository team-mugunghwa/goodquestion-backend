package com.mugunghwa.goodquestion.helpdesk.notification;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 푸시를 받을 기기(FCM 등록 토큰).
 *
 * <p>토큰은 앱 재설치나 기기 복원으로 다른 사용자에게 재발급될 수 있다. 그래서
 * {@code token}이 유일키이고, 같은 토큰이 다른 보호자로 다시 등록되면 소유자를
 * 바꾼다. 이것을 안 하면 기기를 물려받은 사람에게 앞사람의 알림이 간다.
 */
@Entity
@Table(name = "device_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeviceToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "parent_id", nullable = false)
    private UUID parentId;

    @Column(nullable = false, length = 255)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DevicePlatform platform;

    /** 발송이 "등록되지 않은 토큰"으로 거절된 시각. 관리자 콘솔이 남긴다. */
    @Column(name = "disabled_at")
    private OffsetDateTime disabledAt;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false, updatable = false, insertable = false)
    private OffsetDateTime updatedAt;

    @Builder
    public DeviceToken(UUID parentId, String token, DevicePlatform platform) {
        this.parentId = parentId;
        this.token = token;
        this.platform = platform;
    }

    /**
     * 소유자와 플랫폼을 다시 맞추고 비활성 표시를 지운다.
     *
     * <p>앱이 토큰을 다시 등록했다는 것은 그 토큰이 살아 있다는 뜻이므로, 예전에
     * 거절당해 꺼 둔 것이라면 다시 켜야 한다.
     */
    void reassign(UUID parentId, DevicePlatform platform) {
        this.parentId = parentId;
        this.platform = platform;
        this.disabledAt = null;
    }
}
