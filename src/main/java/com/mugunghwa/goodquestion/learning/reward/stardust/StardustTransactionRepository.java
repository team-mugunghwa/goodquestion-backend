package com.mugunghwa.goodquestion.learning.reward.stardust;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StardustTransactionRepository extends JpaRepository<StardustTransaction, UUID> {

    /** 섬 진입 시 떨어지는 연출 대상(보상-08) */
    List<StardustTransaction> findAllByWalletIdAndAcknowledgedFalseOrderByCreatedAtAsc(UUID walletId);

    /** 지급 멱등 확인 — DB 유니크와 함께 이중으로 막는다(데이터-06) */
    boolean existsBySessionIdAndReason(UUID sessionId, StardustReason reason);
}
