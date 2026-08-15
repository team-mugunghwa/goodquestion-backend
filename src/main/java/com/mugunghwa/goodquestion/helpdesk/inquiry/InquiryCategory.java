package com.mugunghwa.goodquestion.helpdesk.inquiry;

/** 문의 분류. 작성 화면에서 사용자가 고른다. */
public enum InquiryCategory {
    ACCOUNT,
    PAYMENT,
    /** 이야기 내용, 표현 */
    CONTENT,
    /** 오류 신고 */
    BUG,
    SUGGESTION,
    ETC
}
