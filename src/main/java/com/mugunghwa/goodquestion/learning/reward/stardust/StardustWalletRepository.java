package com.mugunghwa.goodquestion.learning.reward.stardust;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface StardustWalletRepository extends JpaRepository<StardustWallet, UUID> {

    Optional<StardustWallet> findByChildId(UUID childId);

    /** 지급·구매처럼 잔액을 바꾸는 경로는 동시 갱신을 막기 위해 잠그고 읽는다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from StardustWallet w where w.child.id = :childId")
    Optional<StardustWallet> findByChildIdForUpdate(@Param("childId") UUID childId);
}
