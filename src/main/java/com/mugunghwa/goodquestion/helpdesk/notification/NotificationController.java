package com.mugunghwa.goodquestion.helpdesk.notification;

import com.mugunghwa.goodquestion.global.error.BusinessException;
import com.mugunghwa.goodquestion.global.error.ErrorCode;
import com.mugunghwa.goodquestion.global.security.CurrentParentId;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 알림함과 푸시 기기 등록.
 *
 * <p>알림을 만드는 API는 없다. 알림은 관리자 콘솔이 답변을 등록할 때 만든다.
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final DeviceTokenRepository deviceTokenRepository;

    @GetMapping
    @Transactional(readOnly = true)
    public NotificationListResponse list(@CurrentParentId UUID parentId) {
        List<NotificationResponse> notifications =
                notificationRepository.findAllByParentIdOrderByCreatedAtDesc(parentId)
                        .stream().map(NotificationResponse::from).toList();
        return new NotificationListResponse(notifications,
                notificationRepository.countByParentIdAndReadAtIsNull(parentId));
    }

    /**
     * 안 읽은 개수만.
     *
     * <p>배지를 그리려고 목록 전체를 받는 것을 막는다. 홈 화면이 뜰 때마다 부르는
     * 값이라 응답이 작아야 한다.
     */
    @GetMapping("/unread-count")
    @Transactional(readOnly = true)
    public UnreadCountResponse unreadCount(@CurrentParentId UUID parentId) {
        return new UnreadCountResponse(notificationRepository.countByParentIdAndReadAtIsNull(parentId));
    }

    @PatchMapping("/{notificationId}/read")
    @Transactional
    public NotificationResponse markRead(@CurrentParentId UUID parentId,
                                         @PathVariable UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .filter(found -> found.isOwnedBy(parentId))
                // 남의 알림은 404다. 403이면 그 id의 알림이 존재한다는 사실이 새어 나간다.
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "알림을 찾을 수 없습니다."));
        notification.markRead();
        return NotificationResponse.from(notification);
    }

    @PostMapping("/read-all")
    @Transactional
    public UnreadCountResponse markAllRead(@CurrentParentId UUID parentId) {
        notificationRepository.findAllByParentIdAndReadAtIsNull(parentId)
                .forEach(Notification::markRead);
        return new UnreadCountResponse(0);
    }

    /**
     * 푸시 기기 등록.
     *
     * <p>앱이 뜰 때마다 부른다. FCM 토큰은 앱 재설치나 일정 기간 미사용으로 바뀌고,
     * 바뀐 것을 서버가 알 방법이 이 호출뿐이다.
     *
     * <p>같은 토큰이 이미 있으면 소유자를 이 사용자로 바꾼다. 기기를 물려받거나 한
     * 기기에서 계정을 바꿔 로그인한 경우인데, 그대로 두면 앞사람의 알림이 간다.
     */
    @PostMapping("/devices")
    @Transactional
    public ResponseEntity<Void> registerDevice(@CurrentParentId UUID parentId,
                                               @Valid @RequestBody RegisterDeviceRequest request) {
        deviceTokenRepository.findByToken(request.token())
                .ifPresentOrElse(
                        existing -> existing.reassign(parentId, request.platform()),
                        () -> deviceTokenRepository.save(DeviceToken.builder()
                                .parentId(parentId)
                                .token(request.token())
                                .platform(request.platform())
                                .build()));
        return ResponseEntity.noContent().build();
    }

    /** 로그아웃할 때 부른다. 이 기기로는 더 이상 알림이 가지 않는다. */
    @DeleteMapping("/devices/{token}")
    @Transactional
    public ResponseEntity<Void> unregisterDevice(@CurrentParentId UUID parentId,
                                                 @PathVariable String token) {
        deviceTokenRepository.findByToken(token)
                // 남의 토큰을 지우지 못하게 소유자를 확인한다. 없으면 조용히 넘어간다 -
                // 로그아웃은 실패해서는 안 되는 조작이다.
                .filter(deviceToken -> deviceToken.getParentId().equals(parentId))
                .ifPresent(deviceTokenRepository::delete);
        return ResponseEntity.noContent().build();
    }

    public record NotificationListResponse(List<NotificationResponse> notifications, long unreadCount) {
    }

    public record UnreadCountResponse(long unreadCount) {
    }

    public record NotificationResponse(
            UUID id,
            NotificationType type,
            String title,
            String body,
            String linkPath,
            boolean read,
            OffsetDateTime createdAt
    ) {
        static NotificationResponse from(Notification notification) {
            return new NotificationResponse(notification.getId(), notification.getType(),
                    notification.getTitle(), notification.getBody(), notification.getLinkPath(),
                    notification.getReadAt() != null, notification.getCreatedAt());
        }
    }

    public record RegisterDeviceRequest(@NotBlank String token, @NotNull DevicePlatform platform) {
    }
}
