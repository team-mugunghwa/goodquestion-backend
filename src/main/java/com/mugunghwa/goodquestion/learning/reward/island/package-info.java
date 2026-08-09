/**
 * 섬과 격자 배치(자리표시자) — Island, IslandItem.
 *
 * <p>8x8 격자, 한 칸에 하나. 겹침 방지는 UNIQUE 제약으로 DB가 보장하고
 * 위반은 409로 변환한다(보상-16, 데이터-06). 치우기는 삭제가 아니라 보관함 복귀다(보상-17).
 */
package com.mugunghwa.goodquestion.learning.reward.island;
