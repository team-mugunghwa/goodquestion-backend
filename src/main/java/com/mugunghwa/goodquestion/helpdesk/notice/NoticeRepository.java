package com.mugunghwa.goodquestion.helpdesk.notice;

import com.mugunghwa.goodquestion.helpdesk.ContentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NoticeRepository extends JpaRepository<Notice, UUID> {

    /**
     * 공개된 공지만. 고정을 먼저, 그다음 공개 시각 최신순.
     *
     * <p>페이징하지 않는다. 공지는 수십 건 단위이고 앱 화면이 한 번에 다 그리는 목록이다.
     */
    List<Notice> findAllByStatusOrderByPinnedDescPublishedAtDesc(ContentStatus status);

    Optional<Notice> findByIdAndStatus(UUID id, ContentStatus status);
}
