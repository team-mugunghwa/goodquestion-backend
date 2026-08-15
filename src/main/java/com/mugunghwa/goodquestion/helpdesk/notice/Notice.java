package com.mugunghwa.goodquestion.helpdesk.notice;

import com.mugunghwa.goodquestion.helpdesk.ContentStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 공지사항. <b>관리자 콘솔이 쓰고 여기는 읽기만 한다.</b>
 *
 * <p>그래서 생성자도 수정 메서드도 없다. 유일한 예외가 {@link #increaseViewCount()}인데,
 * 조회수는 읽은 쪽에서만 셀 수 있는 값이다.
 */
@Entity
@Table(name = "notices")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NoticeCategory category;

    /** 목록 맨 위 고정. 점검 공지처럼 기간이 지나면 내리는 것에 쓴다. */
    @Column(nullable = false)
    private boolean pinned;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ContentStatus status;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    @Column(name = "view_count", nullable = false)
    private int viewCount;

    @Column(name = "author_name", length = 50)
    private String authorName;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false, updatable = false, insertable = false)
    private OffsetDateTime updatedAt;

    void increaseViewCount() {
        this.viewCount++;
    }
}
