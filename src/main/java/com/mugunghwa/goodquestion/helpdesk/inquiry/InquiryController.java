package com.mugunghwa.goodquestion.helpdesk.inquiry;

import com.mugunghwa.goodquestion.global.security.CurrentParentId;
import com.mugunghwa.goodquestion.helpdesk.inquiry.dto.InquiryDtos.CreateInquiryRequest;
import com.mugunghwa.goodquestion.helpdesk.inquiry.dto.InquiryDtos.InquiryDetailResponse;
import com.mugunghwa.goodquestion.helpdesk.inquiry.dto.InquiryDtos.InquiryListResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * 고객센터 문의.
 *
 * <p>사용자는 만들고 읽는다. 답변은 관리자 콘솔이 등록하고, 등록되는 순간 알림이 생기며
 * 푸시가 나간다. 사용자는 그 알림을 눌러 여기 상세로 들어온다.
 */
@RestController
@RequestMapping("/api/inquiries")
@RequiredArgsConstructor
public class InquiryController {

    private final InquiryService inquiryService;

    @PostMapping
    public InquiryDetailResponse create(@CurrentParentId UUID parentId,
                                        @Valid @RequestBody CreateInquiryRequest request) {
        return inquiryService.create(parentId, request);
    }

    @GetMapping
    public InquiryListResponse list(@CurrentParentId UUID parentId) {
        return inquiryService.list(parentId);
    }

    /** 답변 전에만 수정할 수 있다. 답변/종료 후에는 409 INQUIRY_ALREADY_ANSWERED. */
    @PatchMapping("/{inquiryId}")
    public InquiryDetailResponse update(@CurrentParentId UUID parentId,
                                        @PathVariable UUID inquiryId,
                                        @Valid @RequestBody CreateInquiryRequest request) {
        return inquiryService.update(parentId, inquiryId, request);
    }

    /** 답변 전에만 삭제할 수 있다. */
    @DeleteMapping("/{inquiryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@CurrentParentId UUID parentId, @PathVariable UUID inquiryId) {
        inquiryService.delete(parentId, inquiryId);
    }

    @GetMapping("/{inquiryId}")
    public InquiryDetailResponse get(@CurrentParentId UUID parentId, @PathVariable UUID inquiryId) {
        return inquiryService.get(parentId, inquiryId);
    }
}
