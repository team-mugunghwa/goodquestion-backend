/**
 * 상점·보유 아이템 — Item, ChildItem.
 *
 * <p>해금 3종(항상·이야기 완주·누적 획득)은 서버가 계산해 응답에 담는다(보상-10, 보상-12).
 * 구매는 해금·잔액 검증 + 차감 + 이력 + 보유 등록을 단일 트랜잭션으로 처리한다(보상-14).
 */
package com.mugunghwa.goodquestion.learning.reward.shop;
