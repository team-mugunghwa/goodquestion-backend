package com.mugunghwa.goodquestion.helpdesk.notification;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 알림함의 한 줄. 관리자 콘솔이 만들고 여기는 읽기와 "읽음 표시"만 한다.
 *
 * <p>푸시와 별개로 이 행이 남는 것이 중요하다. 푸시는 기기가 꺼져 있거나 알림 권한이
 * 없거나 토큰이 만료되면 도착하지 않는다. 그때도 사용자가 앱에서 답변을 확인할 수
 * 있어야 "답변이 등록되면 사용자가 확인할 수 있다"가 성립한다.
 */
@Entity
@Table(name = "notifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "parent_id", nullable = false)
    private UUID parentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String body;

    /** 앱이 이동할 화면 경로. 예: {@code /support/{inquiryId}} */
    @Column(name = "link_path", length = 200)
    private String linkPath;

    @Column(name = "read_at")
    private OffsetDateTime readAt;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private OffsetDateTime createdAt;

    public boolean isOwnedBy(UUID parentId) {
        return this.parentId.equals(parentId);
    }

    /** 이미 읽은 알림을 다시 열어도 처음 읽은 시각을 유지한다. */
    void markRead() {
        if (readAt == null) {
            readAt = OffsetDateTime.now();
        }
    }
}
