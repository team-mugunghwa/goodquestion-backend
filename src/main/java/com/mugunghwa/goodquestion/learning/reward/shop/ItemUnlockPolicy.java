package com.mugunghwa.goodquestion.learning.reward.shop;

import com.mugunghwa.goodquestion.learning.reward.StoryPlayCounter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 아이템 해금 판정 (보상-10).
 *
 * <p>해금 상태는 저장하지 않고 아이 상태에서 매번 계산한다. 저장해 두면 별가루가 늘거나
 * 이야기를 완주했을 때 갱신을 빠뜨린 만큼 실제와 어긋난다.
 *
 * <p>상점 목록과 행성의 다음 해금 안내가 같은 판정을 써야 해서 한 곳에 둔다.
 */
@Component
@RequiredArgsConstructor
public class ItemUnlockPolicy {

    private final StoryPlayCounter playCounter;

    /** @param totalEarned 누적 획득량. 사용해도 줄지 않으므로 잔액이 아니라 이 값이 기준이다 */
    public boolean isUnlocked(ItemView item, UUID childId, int totalEarned) {
        return switch (item.unlockType()) {
            case ALWAYS -> true;
            case STORY_COMPLETE -> item.unlockStoryId() != null
                    && playCounter.get(childId, item.unlockStoryId()) >= 1;
            case STARDUST_CUMULATIVE -> item.unlockStardustTotal() != null
                    && totalEarned >= item.unlockStardustTotal();
        };
    }

    /**
     * 엔티티 경로(단건 구매 검증). 판정 규칙은 뷰 쪽 한 곳에만 둔다 - 두 벌이면
     * 한쪽만 고쳐져 목록에선 열렸는데 구매는 거절되는 식으로 갈라진다.
     */
    public boolean isUnlocked(Item item, UUID childId, int totalEarned) {
        return isUnlocked(ItemView.from(item), childId, totalEarned);
    }

    /**
     * 잠긴 아이템의 해금 조건 안내 문구.
     *
     * <p>아이가 읽는 문장이라 조건을 그대로 옮기지 않고 무엇을 하면 열리는지로 적는다.
     */
    public String conditionText(ItemView item) {
        return switch (item.unlockType()) {
            case ALWAYS -> null;
            case STORY_COMPLETE -> item.unlockStoryTitle() == null ? null
                    : "%s 이야기를 끝까지 하면 열려요".formatted(item.unlockStoryTitle());
            case STARDUST_CUMULATIVE -> item.unlockStardustTotal() == null ? null
                    : "별가루를 모두 %d개 모으면 열려요".formatted(item.unlockStardustTotal());
        };
    }
}
