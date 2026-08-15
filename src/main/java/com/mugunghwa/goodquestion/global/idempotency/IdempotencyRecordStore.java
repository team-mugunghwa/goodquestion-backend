package com.mugunghwa.goodquestion.global.idempotency;

import com.mugunghwa.goodquestion.global.error.BusinessException;
import com.mugunghwa.goodquestion.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 멱등 기록의 트랜잭션 경계. IdempotencyService가 트랜잭션 밖에서 부른다 -
 * 선점(begin)이 본 작업과 별개로 즉시 커밋되어야 동시 재시도가 IN_PROGRESS를 본다.
 */
@Component
@RequiredArgsConstructor
public class IdempotencyRecordStore {

    /** 보관 24시간(규약). 재시도 창은 초 단위라 충분하고, 응답 본문을 오래 쥘 이유가 없다. */
    private static final Duration RETENTION = Duration.ofHours(24);

    private final IdempotentRequestRepository repository;

    /**
     * 키를 선점하거나 기존 기록을 돌려준다.
     *
     * <p>선점 경쟁은 유니크 제약이 심판한다 - 두 요청이 동시에 들어와도 삽입은
     * 한쪽만 성공하고(ON CONFLICT DO NOTHING), 진 쪽은 기존 기록을 읽어 상태에 따라
     * 처리된다.
     *
     * @return 새로 선점한 기록(IN_PROGRESS) 또는 완료된 기존 기록(COMPLETED)
     * @throws BusinessException REQUEST_IN_PROGRESS - 같은 키가 아직 처리 중
     */
    @Transactional
    public IdempotentRequest begin(IdempotentEndpoint endpoint, UUID scopeId, UUID parentId,
                                   String key) {
        int inserted = repository.insertIfAbsent(endpoint.name(), scopeId, parentId, key);
        IdempotentRequest record = repository
                .findByEndpointAndScopeIdAndIdempotencyKey(endpoint, scopeId, key)
                // 선점 직후 삭제(다른 요청의 실패 정리)와 겹친 극단 케이스. 재시도로 풀린다
                .orElseThrow(() -> new BusinessException(ErrorCode.REQUEST_IN_PROGRESS));
        if (inserted == 1) {
            return record;
        }
        // 기록한 보호자와 다르면 남의 응답을 재생하게 된다. 소유권 불일치는 없는 것으로 취급.
        if (!record.getParentId().equals(parentId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (!record.isCompleted()) {
            // 처리 중이면 기다리게 한다. 완료 기록 전에 서버가 죽은 극단 케이스는 이 키가
            // 청소 전까지 409로 남는데, 그때의 복구는 기존 절차(turn-state 확인)를 쓴다 -
            // 오래된 기록을 뺏어 다시 실행하면 막으려던 중복 실행이 그대로 재발한다.
            throw new BusinessException(ErrorCode.REQUEST_IN_PROGRESS);
        }
        return record;
    }

    @Transactional
    public void complete(UUID recordId, String responseJson) {
        repository.findById(recordId).ifPresent(record -> record.complete(responseJson));
    }

    /** 작업 실패 시 키를 비운다 - 정직한 재시도가 다시 실행될 수 있어야 한다. */
    @Transactional
    public void abandon(UUID recordId) {
        repository.deleteById(recordId);
    }

    @Scheduled(fixedDelayString = "PT1H")
    @Transactional
    public void purgeExpired() {
        repository.deleteAllCreatedBefore(OffsetDateTime.now().minus(RETENTION));
    }
}
