package com.mugunghwa.goodquestion.helpdesk.guide;

import com.mugunghwa.goodquestion.global.error.BusinessException;
import com.mugunghwa.goodquestion.global.error.ErrorCode;
import com.mugunghwa.goodquestion.helpdesk.ContentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * 이용안내 조회.
 *
 * <p>목록이 본문까지 함께 내려간다. 문서가 짧고 화면이 아코디언 형태라, 항목을 펼칠
 * 때마다 요청을 보내면 펼침이 한 박자씩 늦는다. 상세 엔드포인트는 딥링크로 특정
 * 문서를 바로 열 때만 쓴다.
 */
@RestController
@RequestMapping("/api/guides")
@RequiredArgsConstructor
public class GuideController {

    private final GuideRepository guideRepository;

    @GetMapping
    @Transactional(readOnly = true)
    public List<GuideResponse> list(@RequestParam(required = false) GuideCategory category) {
        List<Guide> guides = category == null
                ? guideRepository.findAllByStatusOrderByCategoryAscDisplayOrderAsc(ContentStatus.PUBLISHED)
                : guideRepository.findAllByStatusAndCategoryOrderByDisplayOrderAsc(
                        ContentStatus.PUBLISHED, category);
        return guides.stream().map(GuideResponse::from).toList();
    }

    @GetMapping("/{guideId}")
    @Transactional(readOnly = true)
    public GuideResponse get(@PathVariable UUID guideId) {
        return guideRepository.findByIdAndStatus(guideId, ContentStatus.PUBLISHED)
                .map(GuideResponse::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "이용안내를 찾을 수 없습니다."));
    }

    public record GuideResponse(
            UUID id,
            GuideCategory category,
            String title,
            String content,
            short displayOrder
    ) {
        static GuideResponse from(Guide guide) {
            return new GuideResponse(guide.getId(), guide.getCategory(), guide.getTitle(),
                    guide.getContent(), guide.getDisplayOrder());
        }
    }
}
