package com.mugunghwa.goodquestion.helpdesk;

/**
 * 공지와 이용안내가 공유하는 노출 상태.
 *
 * <p>사용자에게 나가는 것은 {@link #PUBLISHED}뿐이다. 나머지 둘은 관리자 콘솔에서만 보인다.
 */
public enum ContentStatus {
    DRAFT,
    PUBLISHED,
    ARCHIVED
}
