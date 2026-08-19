package com.mugunghwa.goodquestion.learning.reward.stardust;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface FirstLoginBonusGrantRepository extends JpaRepository<FirstLoginBonusGrant, UUID> {

    /**
     * 예외 없는 선점. 같은 계정으로 두 요청이 동시에 들어와도 삽입에 성공한 쪽만 지급한다.
     * PostgreSQL은 유니크 위반이 나면 트랜잭션 전체가 중단되므로 위반 자체를 피한다
     * ({@code IdempotentRequestRepository.insertIfAbsent}와 같은 방식).
     *
     * @return 1이면 이번 호출이 선점(지급해야 함), 0이면 이미 지급된 계정
     */
    @Modifying
    @Query(value = """
            insert into first_login_bonus_grants (parent_id, granted_at)
            values (:parentId, now())
            on conflict (parent_id) do nothing
            """, nativeQuery = true)
    int claim(@Param("parentId") UUID parentId);
}
