package com.mugunghwa.goodquestion.global.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * 자체 발급 JWT(HS256)의 발급·검증을 모두 담당한다.
 * Supabase Auth 미사용 결정에 따라 SupabaseJwtVerifier를 대체한다.
 * Access 토큰만 사용 — 만료 시 재로그인으로 재발급 (MVP 범위).
 */
@Component
public class JwtProvider {

    private final SecretKey key;
    private final long expirationMs;

    public JwtProvider(@Value("${jwt.secret}") String secret,
                       @Value("${jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    /** 로그인 성공 시 Access 토큰 발급. sub = parents.id */
    public String issue(UUID parentId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(parentId.toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMs)))
                .signWith(key)
                .compact();
    }

    /** 응답의 accessTokenExpiresIn(초)에 사용한다. */
    public long getExpiresInSeconds() {
        return expirationMs / 1000;
    }

    /** @return 검증된 보호자 ID — 실패 시 JwtException */
    public UUID verifyAndGetParentId(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload();
        return UUID.fromString(claims.getSubject());
    }
}
