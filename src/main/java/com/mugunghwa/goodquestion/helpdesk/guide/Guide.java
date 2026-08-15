package com.mugunghwa.goodquestion.helpdesk.guide;

import com.mugunghwa.goodquestion.helpdesk.ContentStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 이용안내 문서. 관리자 콘솔이 쓰고 여기는 읽기만 한다.
 *
 * <p>공지와 달리 시간순이 아니라 {@code displayOrder} 순으로 나온다. 도움말은 읽는
 * 순서가 있어서(가입 -> 첫 이야기 -> 보상) 최신 글이 위로 올라오면 흐름이 흐트러진다.
 */
@Entity
@Table(name = "guides")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Guide {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GuideCategory category;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "display_order", nullable = false)
    private short displayOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ContentStatus status;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false, updatable = false, insertable = false)
    private OffsetDateTime updatedAt;
}
