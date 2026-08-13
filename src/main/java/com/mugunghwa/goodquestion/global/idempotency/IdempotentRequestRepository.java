package com.mugunghwa.goodquestion.global.idempotency;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface IdempotentRequestRepository extends JpaRepository<IdempotentRequest, UUID> {

    Optional<IdempotentRequest> findByEndpointAndScopeIdAndIdempotencyKey(
            IdempotentEndpoint endpoint, UUID scopeId, String idempotencyKey);

    /**
     * 예외 없는 선점. PostgreSQL은 유니크 위반이 나면 트랜잭션 전체가 중단돼 같은
     * 트랜잭션에서 기존 행을 조회할 수 없다 - ON CONFLICT DO NOTHING으로 위반 자체를
     * 피하고 삽입 여부(1/0)로 승패를 가린다.
     *
     * @return 1이면 이 요청이 선점, 0이면 같은 키의 기록이 이미 있음
     */
    @Modifying
    @Query(value = """
            insert into idempotent_requests
                (endpoint, scope_id, parent_id, idempotency_key, status, created_at)
            values (:endpoint, :scopeId, :parentId, :key, 'IN_PROGRESS', now())
            on conflict on constraint uq_idempotent_requests do nothing
            """, nativeQuery = true)
    int insertIfAbsent(@Param("endpoint") String endpoint, @Param("scopeId") UUID scopeId,
                       @Param("parentId") UUID parentId, @Param("key") String key);

    /** 보관 기간(24시간)이 지난 기록 청소 */
    @Modifying
    @Query("delete from IdempotentRequest r where r.createdAt < :cutoff")
    int deleteAllCreatedBefore(@Param("cutoff") OffsetDateTime cutoff);
}
