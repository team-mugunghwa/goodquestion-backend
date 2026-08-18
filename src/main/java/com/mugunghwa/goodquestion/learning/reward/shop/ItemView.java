package com.mugunghwa.goodquestion.learning.reward.shop;

import com.mugunghwa.goodquestion.story.content.Story;

import java.util.UUID;

/**
 * 아이템 마스터의 읽기 전용 뷰. 캐시와 목록 응답 조립이 이것만 본다.
 *
 * <p>엔티티를 캐시에 담으면 영속성 컨텍스트 밖에서 여러 요청이 같은 인스턴스를 공유하고,
 * unlockStory 같은 LAZY 연관은 세션이 닫힌 뒤 터진다. 필요한 값을 여기 다 풀어 두면
 * 둘 다 원천적으로 없다. 해금 이야기의 제목과 이미지도 적재 시점에 풀어 둔다 -
 * 목록을 낼 때마다 이야기를 다시 읽지 않기 위해서다.
 */
public record ItemView(UUID id, String name, ItemCategory category, int price,
                       UnlockType unlockType, UUID unlockStoryId, String unlockStoryTitle,
                       String unlockStoryImageUrl, Integer unlockStardustTotal,
                       String modelUrl, String thumbnailUrl) {

    /** LAZY unlockStory를 초기화하므로 트랜잭션 안에서 불러야 한다. */
    static ItemView from(Item item) {
        Story story = item.getUnlockStory();
        return new ItemView(
                item.getId(), item.getName(), item.getCategory(), item.getPrice(),
                item.getUnlockType(),
                story == null ? null : story.getId(),
                story == null ? null : story.getTitle(),
                story == null ? null : story.getImageUrl(),
                item.getUnlockStardustTotal(),
                item.getModelUrl(), item.getThumbnailUrl());
    }
}
