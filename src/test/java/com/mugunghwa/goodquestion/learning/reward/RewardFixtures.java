package com.mugunghwa.goodquestion.learning.reward;

import java.util.UUID;

/**
 * Flyway가 적용하는 R__ 시드의 고정 식별자.
 *
 * <p>보상은 지갑·행성·아이템·세션이 얽혀 있어 테스트마다 상수를 다시 적으면 어긋나기 쉽다.
 *
 * <p>지우: 잔액 14 / 누적 25, 방귀 이야기 1회 완주, 돌·풀·작은나무 보유(풀은 보관함),
 * 미확인 지급 1건(ADMIN_ADJUST +20).
 * 하준: 지갑 0 / 누적 0, 완주 없음, 보유 아이템 없음.
 */
public final class RewardFixtures {

    public static final UUID PARENT_ID = UUID.fromString("99999999-9999-9999-9999-000000000001");
    public static final UUID OTHER_PARENT_ID = UUID.fromString("99999999-9999-9999-9999-000000000002");

    /** 지우 */
    public static final UUID CHILD_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-000000000001");
    /** 하준 - 같은 보호자의 다른 아이 */
    public static final UUID SIBLING_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-000000000002");

    public static final UUID STORY_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    public static final UUID COMPLETED_SESSION_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-000000000001");
    public static final UUID IN_PROGRESS_SESSION_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-000000000002");

    /** 작은 돌 - 항상 열림, 1 (진열 1번) */
    public static final UUID ROCK_ITEM_ID = UUID.fromString("44444444-4444-4444-4444-000000000001");
    /** 우리 집 - 누적 9로 해금, 2 */
    public static final UUID HOUSE_ITEM_ID = UUID.fromString("44444444-4444-4444-4444-00000000000d");
    /** 토끼 - 방귀 이야기 완주로 해금, 2 */
    public static final UUID BUNNY_ITEM_ID = UUID.fromString("44444444-4444-4444-4444-00000000000e");
    /** 고양이 - 누적 5로 해금, 2 */
    public static final UUID CAT_ITEM_ID = UUID.fromString("44444444-4444-4444-4444-00000000000f");
    /** 여우 - 누적 11로 해금, 2 */
    public static final UUID FOX_ITEM_ID = UUID.fromString("44444444-4444-4444-4444-000000000010");

    /** 풀 - 지우의 보관함에 있는 보유 아이템 */
    public static final UUID STORED_CHILD_ITEM_ID = UUID.fromString("b2220000-0000-0000-0000-000000000002");
    /** 작은 돌 - 지우가 (0,0)에 놓아 둔 배치 */
    public static final UUID PLACED_AT_ORIGIN_ID = UUID.fromString("c3330000-0000-0000-0000-000000000001");

    private RewardFixtures() {
    }
}
