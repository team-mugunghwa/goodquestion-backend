package com.mugunghwa.goodquestion.learning.reward.shop;

import com.mugunghwa.goodquestion.global.config.CacheConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 판매 중 아이템 목록. 상점, 행성의 다음 해금 안내, 완주 시 새 해금 계산이 같은 목록을 쓴다.
 *
 * <p>items는 시드로만 바뀌는 45행짜리 마스터인데, 세 경로가 각자 전체를 다시 읽고 있었다
 * (완주 한 번에 두 번). 첫 조회만 DB를 타고 그 뒤로는 캐시에서 낸다(무효화가 필요 없는
 * 근거는 CacheConfig 참고).
 *
 * <p>해금 여부는 여기 없다. 목록은 모든 아이에게 같고, 해금은 아이마다 달라 매번
 * 계산한다(ItemUnlockPolicy). 이 경계가 무너지면 한 아이의 해금 상태가 캐시를 타고
 * 다른 아이에게 보인다.
 */
@Service
@RequiredArgsConstructor
public class ItemCatalog {

    private final ItemRepository itemRepository;

    /** 진열 순서대로. LAZY 해금 이야기까지 적재 시점에 풀어 두므로 트랜잭션이 필요하다. */
    @Cacheable(CacheConfig.ITEMS)
    @Transactional(readOnly = true)
    public List<ItemView> activeItems() {
        return itemRepository.findAllByStatusOrderByDisplayOrderAsc(ItemStatus.ACTIVE).stream()
                .map(ItemView::from).toList();
    }
}
