package com.mugunghwa.goodquestion.user.auth;

import com.mugunghwa.goodquestion.global.error.BusinessException;
import com.mugunghwa.goodquestion.global.error.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryLoginAttemptStore implements LoginAttemptStore {

    private final ConcurrentHashMap<String, Attempt> attempts = new ConcurrentHashMap<>();
    private final int maxAttempts;
    private final Duration lockDuration;
    private final Clock clock;

    @Autowired
    public InMemoryLoginAttemptStore(
            @Value("${app.auth.max-login-attempts:10}") int maxAttempts,
            @Value("${app.auth.lock-duration:24h}") Duration lockDuration) {
        this(maxAttempts, lockDuration, Clock.systemUTC());
    }

    InMemoryLoginAttemptStore(int maxAttempts, Duration lockDuration, Clock clock) {
        this.maxAttempts = maxAttempts;
        this.lockDuration = lockDuration;
        this.clock = clock;
    }

    @Override
    public void assertLoginAllowed(String email) {
        String key = normalize(email);
        Attempt attempt = attempts.get(key);
        if (attempt == null) return;
        Instant now = clock.instant();
        if (attempt.lockedUntil() != null && attempt.lockedUntil().isAfter(now)) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        }
        if (attempt.lockedUntil() != null) attempts.remove(key, attempt);
    }

    @Override
    public void recordFailure(String email) {
        String key = normalize(email);
        Instant now = clock.instant();
        Attempt updated = attempts.compute(key, (ignored, previous) -> {
            if (previous != null && previous.lockedUntil() != null
                    && previous.lockedUntil().isAfter(now)) return previous;
            int failures = previous == null ? 1 : previous.failures() + 1;
            Instant lockedUntil = failures >= maxAttempts ? now.plus(lockDuration) : null;
            return new Attempt(failures, lockedUntil);
        });
        if (updated.lockedUntil() != null) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        }
    }

    @Override
    public void reset(String email) {
        attempts.remove(normalize(email));
    }

    private String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private record Attempt(int failures, Instant lockedUntil) {}
}
