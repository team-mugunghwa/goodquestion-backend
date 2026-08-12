package com.mugunghwa.goodquestion.user.auth;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** 비밀번호 재설정용 일회성 토큰 저장소 계약. 원문 토큰이 아닌 SHA-256 해시를 저장한다. */
public interface PasswordResetTokenStore {

    void save(String tokenHash, UUID parentId, Instant expiresAt);

    Optional<UUID> consume(String tokenHash, Instant now);
}
