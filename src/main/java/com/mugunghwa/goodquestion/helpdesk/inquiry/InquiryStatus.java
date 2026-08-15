package com.mugunghwa.goodquestion.helpdesk.inquiry;

public enum InquiryStatus {
    /** 답변 대기 */
    PENDING,
    ANSWERED,
    /** 종료. 답변 없이 닫힌 것(중복 문의 등)도 포함한다. */
    CLOSED
}
