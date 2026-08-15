package com.mugunghwa.goodquestion.helpdesk.inquiry.dto;

import com.mugunghwa.goodquestion.helpdesk.inquiry.Inquiry;
import com.mugunghwa.goodquestion.helpdesk.inquiry.InquiryAnswer;
import com.mugunghwa.goodquestion.helpdesk.inquiry.InquiryCategory;
import com.mugunghwa.goodquestion.helpdesk.inquiry.InquiryStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class InquiryDtos {

    private InquiryDtos() {
    }

    public record CreateInquiryRequest(
            InquiryCategory category,
            @NotBlank @Size(max = 200, message = "제목은 200자 이하로 입력해 주세요.") String title,
            @NotBlank @Size(max = 2000, message = "내용은 2000자 이하로 입력해 주세요.") String content
    ) {
    }

    public record InquiryListResponse(List<InquirySummaryResponse> inquiries) {
    }

    /**
     * 목록 한 줄. 본문은 싣지 않는다.
     *
     * @param answered 답변 여부. status로도 알 수 있지만 화면이 배지 하나로 쓰기 좋게 따로 준다.
     */
    public record InquirySummaryResponse(
            UUID id,
            InquiryCategory category,
            String title,
            InquiryStatus status,
            boolean answered,
            OffsetDateTime createdAt
    ) {
        public static InquirySummaryResponse of(Inquiry inquiry, boolean answered) {
            return new InquirySummaryResponse(inquiry.getId(), inquiry.getCategory(),
                    inquiry.getTitle(), inquiry.getStatus(), answered, inquiry.getCreatedAt());
        }
    }

    public record InquiryDetailResponse(
            UUID id,
            InquiryCategory category,
            String title,
            String content,
            InquiryStatus status,
            OffsetDateTime createdAt,
            /** 아직 답변이 없으면 null. 화면은 이 값으로 "답변 대기" 안내를 그린다. */
            AnswerResponse answer
    ) {
        public static InquiryDetailResponse of(Inquiry inquiry, AnswerResponse answer) {
            return new InquiryDetailResponse(inquiry.getId(), inquiry.getCategory(),
                    inquiry.getTitle(), inquiry.getContent(), inquiry.getStatus(),
                    inquiry.getCreatedAt(), answer);
        }
    }

    public record AnswerResponse(String adminName, String content, OffsetDateTime answeredAt) {
        public static AnswerResponse from(InquiryAnswer answer) {
            // 수정된 답변이면 사용자가 보는 것은 고친 내용이므로 updatedAt이 맞다.
            return new AnswerResponse(answer.getAdminName(), answer.getContent(),
                    answer.getUpdatedAt());
        }
    }
}
