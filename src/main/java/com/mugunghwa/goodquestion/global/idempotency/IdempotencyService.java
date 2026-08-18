package com.mugunghwa.goodquestion.global.idempotency;

import com.mugunghwa.goodquestion.global.error.BusinessException;
import com.mugunghwa.goodquestion.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * 클라이언트 멱등키(Idempotency-Key 헤더) 처리.
 *
 * <p>락은 동시 중복(겹침)만 막는다. 첫 요청이 커밋된 뒤의 재시도는 락도 유니크도
 * 걸 데가 없고, 재전송인지 새 작업인지는 클라이언트만 안다 - 그 구분을 키로 받아
 * 기록하고, 같은 키의 재시도에는 저장한 응답을 재생한다.
 *
 * <p>키가 없으면 기존 동작 그대로다(하위 호환). 판정과 기록은 서버가 한다.
 */
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private static final int MAX_KEY_LENGTH = 64;

    private final IdempotencyRecordStore store;
    private final ObjectMapper objectMapper;

    /**
     * <b>트랜잭션 밖에서 부른다</b> - 선점 기록이 본 작업과 독립적으로 커밋되어야 한다.
     *
     * @param scopeId 키의 유효 범위 (발화는 세션, 구매는 아이)
     * @param key     Idempotency-Key 헤더 값. null이면 멱등 처리 없이 그대로 실행
     */
    public <T> T execute(IdempotentEndpoint endpoint, UUID scopeId, UUID parentId, String key,
                         Class<T> responseType, Supplier<T> work) {
        if (key == null || key.isBlank()) {
            return work.get();
        }
        if (key.length() > MAX_KEY_LENGTH) {
            throw new BusinessException(ErrorCode.INVALID_IDEMPOTENCY_KEY);
        }

        IdempotentRequest record = store.begin(endpoint, scopeId, parentId, key.trim());
        if (record.isCompleted()) {
            return objectMapper.readValue(record.getResponse(), responseType);
        }
        try {
            T result = work.get();
            store.complete(record.getId(), objectMapper.writeValueAsString(result));
            return result;
        } catch (RuntimeException e) {
            // 재실행이 안전한 작업만 키를 비운다. 여러 트랜잭션에 걸친 작업은 앞부분이
            // 이미 커밋됐을 수 있어, 키를 비우면 같은 키의 재시도가 그 앞부분을 한 번 더
            // 실행한다 - 멱등키가 막겠다고 한 중복이 멱등키 때문에 생기는 셈이다.
            //
            // complete()가 던진 경우도 여기로 온다. 그때는 작업이 이미 끝난 뒤라 더더욱
            // 다시 실행하면 안 된다.
            if (endpoint.isReexecutable()) {
                store.abandon(record.getId());
            } else {
                store.fail(record.getId(), e.getClass().getSimpleName() + ": " + e.getMessage());
            }
            throw e;
        }
    }
}
