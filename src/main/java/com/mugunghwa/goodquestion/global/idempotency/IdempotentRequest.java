package com.mugunghwa.goodquestion.global.idempotency;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 멱등 요청 기록. 규약과 동작은 V6 마이그레이션 주석 참고.
 */
@Entity
@Table(name = "idempotent_requests",
        uniqueConstraints = @UniqueConstraint(name = "uq_idempotent_requests",
                columnNames = {"endpoint", "scope_id", "idempotency_key"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IdempotentRequest {

    /**
     * FAILED는 "실패했고 다시 실행하면 안 된다"는 뜻이다. 다시 해도 되는 작업은
     * 행을 남기지 않고 지운다(IdempotentEndpoint.isReexecutable).
     */
    public enum Status { IN_PROGRESS, COMPLETED, FAILED }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private IdempotentEndpoint endpoint;

    @Column(name = "scope_id", nullable = false)
    private UUID scopeId;

    @Column(name = "parent_id", nullable = false)
    private UUID parentId;

    @Column(name = "idempotency_key", nullable = false, length = 64)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    /** 완료 응답 본문(JSON). 재생 시 이 값을 그대로 역직렬화해 돌려준다. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String response;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    /** 실패 사유. 클라이언트에 내리지 않고 운영자가 로그와 대조할 때 쓴다. */
    @Column(name = "failure_reason", length = 200)
    private String failureReason;

    @Builder
    public IdempotentRequest(IdempotentEndpoint endpoint, UUID scopeId, UUID parentId,
                             String idempotencyKey) {
        this.endpoint = endpoint;
        this.scopeId = scopeId;
        this.parentId = parentId;
        this.idempotencyKey = idempotencyKey;
        this.status = Status.IN_PROGRESS;
        this.createdAt = OffsetDateTime.now();
    }

    public boolean isCompleted() {
        return status == Status.COMPLETED;
    }

    public void complete(String responseJson) {
        this.status = Status.COMPLETED;
        this.response = responseJson;
        this.completedAt = OffsetDateTime.now();
    }

    /** 실패로 닫는다. 같은 키의 재시도는 재실행되지 않고 상태 확인 안내를 받는다. */
    public void fail(String reason) {
        this.status = Status.FAILED;
        this.failureReason = reason == null ? null
                : reason.substring(0, Math.min(reason.length(), 200));
        this.completedAt = OffsetDateTime.now();
    }
}
