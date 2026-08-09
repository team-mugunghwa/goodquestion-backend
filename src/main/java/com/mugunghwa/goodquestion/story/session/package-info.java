/**
 * 세션 애그리거트 — StorySession(루트)과 Message.
 *
 * <p>Message를 별도 패키지로 두지 않는 이유: 세션과 같은 애그리거트에 속하고
 * 같은 트랜잭션에서 함께 변이하기 때문이다. 턴 처리의 트랜잭션 경계도 이 애그리거트다.
 */
package com.mugunghwa.goodquestion.story.session;
