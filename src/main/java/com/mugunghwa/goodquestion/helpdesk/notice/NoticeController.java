package com.mugunghwa.goodquestion.helpdesk.notice;

import com.mugunghwa.goodquestion.global.error.BusinessException;
import com.mugunghwa.goodquestion.global.error.ErrorCode;
import com.mugunghwa.goodquestion.helpdesk.ContentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 공지사항 조회.
 *
 * <p>내용은 관리자 콘솔이 쓴다. 여기서 하는 일은 <b>공개된 것만</b> 골라 내리는 것과
 * 조회수를 세는 것뿐이다. 비공개/보관 상태의 공지는 id를 알아도 404다.
 */
@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeRepository noticeRepository;

    @GetMapping
    @Transactional(readOnly = true)
    public List<NoticeSummary> list() {
        return noticeRepository
                .findAllByStatusOrderByPinnedDescPublishedAtDesc(ContentStatus.PUBLISHED)
                .stream().map(NoticeSummary::from).toList();
    }

    /**
     * 상세. 읽을 때마다 조회수를 올린다.
     *
     * <p>같은 사람이 여러 번 열면 그만큼 올라간다. 중복을 걸러 내려면 누가 읽었는지를
     * 남겨야 하는데, 공지 조회수는 "얼마나 눈에 띄었나"를 보는 값이라 그 정확도를 위해
     * 읽기 기록을 쌓을 이유가 없다.
     */
    @GetMapping("/{noticeId}")
    @Transactional
    public NoticeDetail get(@PathVariable UUID noticeId) {
        Notice notice = noticeRepository.findByIdAndStatus(noticeId, ContentStatus.PUBLISHED)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "공지를 찾을 수 없습니다."));
        notice.increaseViewCount();
        return NoticeDetail.from(notice);
    }

    /** 목록용. 본문은 싣지 않는다 - 길고, 목록에서 쓰지 않는다. */
    public record NoticeSummary(
            UUID id,
            String title,
            NoticeCategory category,
            boolean pinned,
            OffsetDateTime publishedAt
    ) {
        static NoticeSummary from(Notice notice) {
            return new NoticeSummary(notice.getId(), notice.getTitle(), notice.getCategory(),
                    notice.isPinned(), notice.getPublishedAt());
        }
    }

    public record NoticeDetail(
            UUID id,
            String title,
            String content,
            NoticeCategory category,
            boolean pinned,
            OffsetDateTime publishedAt
    ) {
        static NoticeDetail from(Notice notice) {
            return new NoticeDetail(notice.getId(), notice.getTitle(), notice.getContent(),
                    notice.getCategory(), notice.isPinned(), notice.getPublishedAt());
        }
    }
}
