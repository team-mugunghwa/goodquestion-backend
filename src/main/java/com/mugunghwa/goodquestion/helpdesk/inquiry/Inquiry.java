package com.mugunghwa.goodquestion.helpdesk.inquiry;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 고객센터 문의. <b>사용자가 만들고 관리자 콘솔이 답변한다.</b>
 *
 * <p>상태를 바꾸는 메서드가 없다. 답변 등록과 종료는 관리자 콘솔 쪽 코드가 하고,
 * 이쪽은 만들기와 읽기만 한다. 사용자가 자기 문의를 지우는 것도 두지 않았다 -
 * 답변 이력이 함께 사라지면 같은 문의가 반복될 때 앞선 응대를 확인할 수 없다.
 */
@Entity
@Table(name = "inquiries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Inquiry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "parent_id", nullable = false)
    private UUID parentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InquiryCategory category;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InquiryStatus status;

    @Column(name = "answered_at")
    private OffsetDateTime answeredAt;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false, updatable = false, insertable = false)
    private OffsetDateTime updatedAt;

    @Builder
    public Inquiry(UUID parentId, InquiryCategory category, String title, String content) {
        this.parentId = parentId;
        this.category = category != null ? category : InquiryCategory.ETC;
        this.title = title;
        this.content = content;
        this.status = InquiryStatus.PENDING;
    }

    public boolean isOwnedBy(UUID parentId) {
        return this.parentId.equals(parentId);
    }
}
