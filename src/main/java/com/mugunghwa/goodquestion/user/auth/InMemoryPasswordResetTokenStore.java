package com.mugunghwa.goodquestion.user.auth;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryPasswordResetTokenStore implements PasswordResetTokenStore {

    private final ConcurrentHashMap<String, Entry> tokens = new ConcurrentHashMap<>();

    @Override
    public void save(String tokenHash, UUID parentId, Instant expiresAt) {
        tokens.put(tokenHash, new Entry(parentId, expiresAt));
    }

    @Override
    public Optional<UUID> consume(String tokenHash, Instant now) {
        Entry entry = tokens.remove(tokenHash);
        if (entry == null || !entry.expiresAt().isAfter(now)) return Optional.empty();
        return Optional.of(entry.parentId());
    }

    private record Entry(UUID parentId, Instant expiresAt) {}
}
