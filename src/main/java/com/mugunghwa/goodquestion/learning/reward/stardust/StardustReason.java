package com.mugunghwa.goodquestion.learning.reward.stardust;

/**
 * 별가루 증감 사유 — 지급 3종, 사용 1종, 운영 보정 1종.
 *
 * <p>WORD_PRACTICED는 단어 말하기 연습 보상이다 — 단어당 최초 1회 · 1개 · 하루 최대 3개.
 * 상한 판정은 word_practices를 세는 WordPracticeService가 한다.
 *
 * <p>SENTENCE_PRACTICED는 예문 따라 말하기 보상이다 — 예문(단어 x 유형)당 최초 1회 · 2개 ·
 * 하루 최대 2건. 상한 판정은 sentence_practices를 세는 SentencePracticeService가 한다.
 *
 * <p>WELCOME은 아이 생성 시 1회 지급이다. 첫 완주 전에는 상점에서 아무것도 살 수 없어
 * 행성 탭을 먼저 연 아이가 빈 화면만 만난다 — 사이클을 시작 전에 한 바퀴 맛보게 한다.
 *
 * <p>FIRST_LOGIN은 계정당 최초 로그인 1회 지급이다. 지갑은 아이당이지만 로그인은 보호자
 * 단위라, 한 번의 지급으로 그 계정의 아이 전원이 같은 양을 받는다. 두 번째 로그인부터는
 * first_login_bonus_grants에 선점 기록이 남아 지급되지 않는다.
 *
 * <p>ADMIN_ADJUST는 시연·장애 보정용이다. 지급 경로가 세션 완료 하나뿐이라
 * 이게 없으면 운영 중 보정을 DB에 직접 넣어야 한다.
 */
public enum StardustReason {
    STORY_COMPLETED, SCENE_BONUS, ITEM_PURCHASE, WELCOME, FIRST_LOGIN,
    WORD_PRACTICED, SENTENCE_PRACTICED, ADMIN_ADJUST
}
