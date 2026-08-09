package com.mugunghwa.goodquestion.learning.reward.stardust;

/**
 * 별가루 증감 사유 — 지급 2종, 사용 1종, 운영 보정 1종.
 *
 * <p>ADMIN_ADJUST는 시연·장애 보정용이다. 지급 경로가 세션 완료 하나뿐이라
 * 이게 없으면 운영 중 보정을 DB에 직접 넣어야 한다.
 */
public enum StardustReason { STORY_COMPLETED, SCENE_BONUS, ITEM_PURCHASE, ADMIN_ADJUST }
