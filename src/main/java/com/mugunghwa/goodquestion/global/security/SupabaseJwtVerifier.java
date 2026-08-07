package com.mugunghwa.goodquestion.global.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Supabase가 발급한 JWT(HS256)를 검증하고 sub(= parents.id)를 추출한다. */
@Component
public class SupabaseJwtVerifier {

    private final SecretKey key;

    public SupabaseJwtVerifier(@Value("${supabase.jwt-secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /** @return 검증된 보호자 ID (auth.users.id) — 실패 시 JwtException */
    public UUID verifyAndGetParentId(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload();
        return UUID.fromString(claims.getSubject());
    }
}
