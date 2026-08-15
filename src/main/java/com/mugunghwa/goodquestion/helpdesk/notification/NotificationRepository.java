package com.mugunghwa.goodquestion.helpdesk.notification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findAllByParentIdOrderByCreatedAtDesc(UUID parentId);

    List<Notification> findAllByParentIdAndReadAtIsNull(UUID parentId);

    /** 배지에 쓰는 값. 목록을 받아 세지 않고 count로 묻는다. */
    long countByParentIdAndReadAtIsNull(UUID parentId);
}
