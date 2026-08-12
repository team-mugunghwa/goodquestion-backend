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
    public boolean isUnlocked(Item item, UUID childId, int totalEarned) {
        return switch (item.getUnlockType()) {
            case ALWAYS -> true;
            case STORY_COMPLETE -> item.getUnlockStory() != null
                    && playCounter.get(childId, item.getUnlockStory().getId()) >= 1;
            case STARDUST_CUMULATIVE -> item.getUnlockStardustTotal() != null
                    && totalEarned >= item.getUnlockStardustTotal();
        };
    }

    /**
     * 잠긴 아이템의 해금 조건 안내 문구.
     *
     * <p>아이가 읽는 문장이라 조건을 그대로 옮기지 않고 무엇을 하면 열리는지로 적는다.
     */
    public String conditionText(Item item) {
        return switch (item.getUnlockType()) {
            case ALWAYS -> null;
            case STORY_COMPLETE -> item.getUnlockStory() == null ? null
                    : "%s 이야기를 끝까지 하면 열려요".formatted(item.getUnlockStory().getTitle());
            case STARDUST_CUMULATIVE -> item.getUnlockStardustTotal() == null ? null
                    : "별가루를 모두 %d개 모으면 열려요".formatted(item.getUnlockStardustTotal());
        };
    }
}
