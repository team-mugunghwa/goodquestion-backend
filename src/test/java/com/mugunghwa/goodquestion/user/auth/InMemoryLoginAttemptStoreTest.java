package com.mugunghwa.goodquestion.user.auth;

import com.mugunghwa.goodquestion.global.error.BusinessException;
import com.mugunghwa.goodquestion.global.error.ErrorCode;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryLoginAttemptStoreTest {

    @Test
    void 열번째_실패부터_계정을_잠근다() {
        InMemoryLoginAttemptStore store = new InMemoryLoginAttemptStore(
                10,
                Duration.ofHours(24),
                Clock.fixed(Instant.parse("2026-08-12T00:00:00Z"), ZoneOffset.UTC));

        for (int attempt = 1; attempt < 10; attempt++) {
            store.recordFailure("parent@example.com");
        }

        assertThatThrownBy(() -> store.recordFailure("parent@example.com"))
                .isInstanceOfSatisfying(BusinessException.class, error ->
                        org.assertj.core.api.Assertions.assertThat(error.getErrorCode())
                                .isEqualTo(ErrorCode.ACCOUNT_LOCKED));
        assertThatThrownBy(() -> store.assertLoginAllowed("PARENT@example.com"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void 성공하면_실패횟수를_초기화한다() {
        InMemoryLoginAttemptStore store = new InMemoryLoginAttemptStore(
                2, Duration.ofHours(24), Clock.systemUTC());

        store.recordFailure("parent@example.com");
        store.reset("parent@example.com");
        store.recordFailure("parent@example.com");
        store.assertLoginAllowed("parent@example.com");
    }
}
