package com.mugunghwa.goodquestion.learning.reward.shop;

import com.mugunghwa.goodquestion.global.config.CacheConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

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
 *
 * <p><b>{@code @Transactional}을 걸지 않는다.</b> 캐시 메서드에 트랜잭션을 함께 걸면
 * 두 어드바이저의 순서에 결과가 딸려간다 - 트랜잭션이 바깥이면 캐시가 맞는 요청도
 * 매번 커넥션을 얻는다. 순서에 기대는 대신 트랜잭션이 필요할 이유 자체를 없앴다.
 * 리포지토리가 해금 이야기까지 페치 조인으로 읽어 오므로 LAZY 초기화가 없고,
 * 조회 자체는 Spring Data 리포지토리가 자기 트랜잭션에서 처리한다.
 */
@Service
@RequiredArgsConstructor
public class ItemCatalog {

    private final ItemRepository itemRepository;

    /** 진열 순서대로. 캐시가 맞으면 DB도 트랜잭션도 건드리지 않는다. */
    @Cacheable(CacheConfig.ITEMS)
    public List<ItemView> activeItems() {
        return itemRepository.findAllActiveWithUnlockStory(ItemStatus.ACTIVE).stream()
                .map(ItemView::from).toList();
    }
}
