package com.mugunghwa.goodquestion.helpdesk.inquiry;

import com.mugunghwa.goodquestion.global.error.BusinessException;
import com.mugunghwa.goodquestion.global.error.ErrorCode;
import com.mugunghwa.goodquestion.helpdesk.inquiry.dto.InquiryDtos.AnswerResponse;
import com.mugunghwa.goodquestion.helpdesk.inquiry.dto.InquiryDtos.CreateInquiryRequest;
import com.mugunghwa.goodquestion.helpdesk.inquiry.dto.InquiryDtos.InquiryDetailResponse;
import com.mugunghwa.goodquestion.helpdesk.inquiry.dto.InquiryDtos.InquiryListResponse;
import com.mugunghwa.goodquestion.helpdesk.inquiry.dto.InquiryDtos.InquirySummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InquiryService {

    private final InquiryRepository inquiryRepository;
    private final InquiryAnswerRepository answerRepository;

    @Transactional
    public InquiryDetailResponse create(UUID parentId, CreateInquiryRequest request) {
        Inquiry inquiry = inquiryRepository.save(Inquiry.builder()
                .parentId(parentId)
                .category(request.category())
                .title(request.title())
                .content(request.content())
                .build());
        return InquiryDetailResponse.of(inquiry, null);
    }

    public InquiryListResponse list(UUID parentId) {
        List<Inquiry> inquiries = inquiryRepository.findAllByParentIdOrderByCreatedAtDesc(parentId);
        List<UUID> ids = inquiries.stream().map(Inquiry::getId).toList();

        // 목록에서 문의마다 답변을 따로 조회하면 문의 수만큼 쿼리가 나간다.
        // 목록이 쓰는 것은 "답변이 있는가"뿐이라 id 집합만 만들어 둔다.
        Set<UUID> answered = ids.isEmpty() ? Set.of()
                : answerRepository.findAllByInquiryIdIn(ids).stream()
                .map(InquiryAnswer::getInquiryId).collect(Collectors.toSet());

        return new InquiryListResponse(inquiries.stream()
                .map(inquiry -> InquirySummaryResponse.of(inquiry, answered.contains(inquiry.getId())))
                .toList());
    }

    /**
     * 문의 상세. 답변이 있으면 함께 내린다.
     *
     * <p>남의 문의는 404다. 403으로 내리면 "그 id의 문의가 존재한다"는 사실이 새어 나가고,
     * 어느 쪽이든 사용자가 할 수 있는 일은 같다.
     */
    /**
     * 문의 수정. <b>답변이 달리기 전에만</b> 할 수 있다 - 답변이 이미 나간
     * 문의의 내용이 바뀌면 답변이 무엇에 대한 것인지 어긋난다. 종료(CLOSED)된
     * 문의도 같다.
     */
    @Transactional
    public InquiryDetailResponse update(UUID parentId, UUID inquiryId, CreateInquiryRequest request) {
        Inquiry inquiry = findOwnedEditable(parentId, inquiryId);
        inquiry.edit(request.category(), request.title(), request.content());
        return InquiryDetailResponse.of(inquiry, null);
    }

    /** 문의 삭제. 수정과 같은 이유로 답변 전에만 할 수 있다. */
    @Transactional
    public void delete(UUID parentId, UUID inquiryId) {
        Inquiry inquiry = findOwnedEditable(parentId, inquiryId);
        inquiryRepository.delete(inquiry);
    }

    private Inquiry findOwnedEditable(UUID parentId, UUID inquiryId) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .filter(found -> found.isOwnedBy(parentId))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "문의를 찾을 수 없습니다."));
        if (inquiry.getStatus() != InquiryStatus.PENDING) {
            throw new BusinessException(ErrorCode.INQUIRY_ALREADY_ANSWERED);
        }
        return inquiry;
    }

    public InquiryDetailResponse get(UUID parentId, UUID inquiryId) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .filter(found -> found.isOwnedBy(parentId))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "문의를 찾을 수 없습니다."));

        AnswerResponse answer = answerRepository.findByInquiryId(inquiryId)
                .map(AnswerResponse::from)
                .orElse(null);
        return InquiryDetailResponse.of(inquiry, answer);
    }
}
