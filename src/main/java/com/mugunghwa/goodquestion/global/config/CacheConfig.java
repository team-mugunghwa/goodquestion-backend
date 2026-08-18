package com.mugunghwa.goodquestion.global.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 콘텐츠 마스터 조회 캐시.
 *
 * <p>대상은 topics와 items 두 개다. 둘 다 Flyway 시드로만 바뀌는 읽기 전용 데이터라
 * (이 서버에 쓰기 경로가 없다 - 각 리포지토리 참조 전수 확인, 2026-08-18) 무효화가
 * 필요 없고, 재배포가 곧 재적재다. <b>이 전제가 캐시의 존립 근거다.</b> 관리자 콘솔
 * 같은 다른 프로세스가 이 테이블을 쓰게 되면 TTL을 달거나 캐시를 빼야 한다.
 *
 * <p>TTL을 두지 않는 이유: 낡지 않는 데이터에 TTL을 달면 멀쩡한 값을 주기적으로 버리고
 * 다시 읽을 뿐이다. maximumSize는 정책이 아니라 안전판이다 - 캐시 키가 인자 없는
 * 메서드라 엔트리가 캐시당 1개뿐이지만, 나중에 누가 키 있는 메서드를 같은 캐시에
 * 얹었을 때 무한히 자라는 것만 막는다.
 *
 * <p>세션, 지갑, 해금처럼 아이마다 다르고 매번 달라지는 값은 여기 얹지 않는다.
 * 해금 판정을 저장하지 않고 매번 계산하는 이유는 ItemUnlockPolicy 주석 참고.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String TOPICS = "topics";
    public static final String ITEMS = "items";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager(TOPICS, ITEMS);
        manager.setCaffeine(Caffeine.newBuilder().maximumSize(100));
        return manager;
    }
}
