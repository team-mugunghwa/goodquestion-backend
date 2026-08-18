package com.mugunghwa.goodquestion.global.config;

import com.mugunghwa.goodquestion.learning.reward.shop.ItemCatalog;
import com.mugunghwa.goodquestion.learning.reward.shop.ItemView;
import com.mugunghwa.goodquestion.learning.reward.shop.UnlockType;
import com.mugunghwa.goodquestion.story.content.TopicService;
import com.mugunghwa.goodquestion.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 콘텐츠 마스터 캐시(topics, items) 검증.
 *
 * <p>확인하는 것은 두 가지다. 두 번째 호출부터 캐시가 그대로 나오는지(동일 참조 -
 * 캐시가 안 걸리면 매번 새 리스트가 만들어지므로 참조가 다르다), 그리고 캐시에 담긴
 * 값이 시드와 일치하는지. LAZY 해금 이야기가 적재 시점에 풀려 있는지도 본다 -
 * 안 풀려 있으면 세션이 닫힌 뒤 목록을 낼 때 터진다.
 */
@IntegrationTest
class ContentCacheTest {

    @Autowired
    private TopicService topicService;

    @Autowired
    private ItemCatalog itemCatalog;

    @Test
    void 토픽은_두_번째_호출부터_캐시에서_나온다() {
        var first = topicService.getTopics();
        var second = topicService.getTopics();

        assertThat(second).isSameAs(first);
        assertThat(first).isNotEmpty();
    }

    @Test
    void 아이템_목록은_두_번째_호출부터_캐시에서_나온다() {
        List<ItemView> first = itemCatalog.activeItems();
        List<ItemView> second = itemCatalog.activeItems();

        assertThat(second).isSameAs(first);
        assertThat(first).isNotEmpty();
    }

    @Test
    void 완주_해금_아이템은_이야기_제목이_적재_시점에_풀려_있다() {
        List<ItemView> storyUnlocks = itemCatalog.activeItems().stream()
                .filter(item -> item.unlockType() == UnlockType.STORY_COMPLETE)
                .toList();

        assertThat(storyUnlocks).isNotEmpty();
        assertThat(storyUnlocks).allSatisfy(item -> {
            assertThat(item.unlockStoryId()).isNotNull();
            assertThat(item.unlockStoryTitle()).isNotBlank();
        });
    }

    /** 목록은 모든 아이에게 같아야 한다 - 아이별 상태(해금 여부)가 뷰에 섞이면 캐시를 타고 샌다. */
    @Test
    void 아이템_뷰에는_아이별_상태가_없다() {
        assertThat(ItemView.class.getRecordComponents())
                .extracting(java.lang.reflect.RecordComponent::getName)
                .noneMatch(name -> name.toLowerCase().contains("unlocked")
                        || name.toLowerCase().contains("child"));
    }
}
