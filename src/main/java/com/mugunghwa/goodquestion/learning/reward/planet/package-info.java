/**
 * 행성과 격자 배치(자리표시자) — Planet, PlanetItem.
 *
 * <p>좌표는 프론트(planet/)와 같은 축좌표(q, r)를 쓰고, 판 크기·모양은 클라이언트 카탈로그가
 * 단일 소스라 서버에 두지 않는다. 한 칸에 하나이며 겹침 방지는 UNIQUE 제약으로 DB가 보장하고
 * 위반은 409로 변환한다(보상-16, 데이터-06). 치우기는 삭제가 아니라 보관함 복귀다(보상-17).
 */
package com.mugunghwa.goodquestion.learning.reward.planet;
