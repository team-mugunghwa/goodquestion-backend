package com.mugunghwa.goodquestion.helpdesk.inquiry;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 문의 답변. 관리자 콘솔이 쓰고 여기는 읽기만 한다. 문의당 한 건이다(DB에 unique).
 *
 * <p>{@code adminId}는 매핑하지 않는다. 사용자에게 필요한 것은 "고객센터가 답했다"는
 * 사실과 내용이지 어느 계정이 썼는지가 아니다.
 */
@Entity
@Table(name = "inquiry_answers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InquiryAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "inquiry_id", nullable = false)
    private UUID inquiryId;

    /** 화면에 "고객센터"처럼 표시할 이름. */
    @Column(name = "admin_name", nullable = false, length = 50)
    private String adminName;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false, updatable = false, insertable = false)
    private OffsetDateTime updatedAt;
}
