package com.mugunghwa.goodquestion.learning.reward.stardust;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StardustTransactionRepository extends JpaRepository<StardustTransaction, UUID> {

    /** 행성 진입 시 떨어지는 연출 대상(보상-08) */
    List<StardustTransaction> findAllByWalletIdAndAcknowledgedFalseOrderByCreatedAtAsc(UUID walletId);

    /** 지급 멱등 확인 — DB 유니크와 함께 이중으로 막는다(데이터-06) */
    boolean existsBySessionIdAndReason(UUID sessionId, StardustReason reason);

    /** 장면 보너스는 장면마다 최대 1회라 멱등 판정에 장면까지 필요하다 */
    boolean existsBySessionIdAndSceneIdAndReason(UUID sessionId, UUID sceneId, StardustReason reason);

    /** "한 세션에 최대 2회" 상한은 유니크로 표현할 수 없어 세어서 막는다 */
    int countBySessionIdAndReason(UUID sessionId, StardustReason reason);

    /** 완주 화면에 세션에서 받은 별가루를 모두 보여 준다 — 장면 보너스는 놀이 중에 쌓인다 */
    List<StardustTransaction> findAllBySessionIdOrderByCreatedAtAsc(UUID sessionId);
}
