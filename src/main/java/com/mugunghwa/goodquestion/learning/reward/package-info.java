/**
 * 보상 — 섬 꾸미기(별가루 경제·상점·격자 배치).
 *
 * <p>지급은 세션 완료 트랜잭션 안에서 1회만 실행하며 (세션, 사유) 유니크로 멱등을 보장한다(보상-04, 데이터-06).
 * 지급 규칙은 LLM이 아닌 도메인 서비스가 계산한다.
 */
package com.mugunghwa.goodquestion.learning.reward;
