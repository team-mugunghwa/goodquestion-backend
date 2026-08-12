package com.mugunghwa.goodquestion.user.auth;

import com.mugunghwa.goodquestion.global.error.BusinessException;
import com.mugunghwa.goodquestion.global.error.ErrorCode;
import com.mugunghwa.goodquestion.user.parent.Parent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

/**
 * 리프레시 토큰 발급·회전·무효화 (계정-05).
 *
 * <p>Access 토큰과 달리 JWT가 아니다. 어차피 매 재발급마다 DB에서 유효성을 확인해야 하므로
 * 서명으로 자기 검증할 이유가 없고, 랜덤 문자열이면 추측도 불가능하다.
 *
 * <p>원문은 저장하지 않고 해시만 남긴다 — DB가 유출돼도 그 자체로는 재발급에 쓸 수 없다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RefreshTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    /** 로그인·가입·회전 시 새 리프레시 토큰을 발급하고 해시를 저장한다. @return 클라이언트에 내려줄 원문 */
    @Transactional
    public String issue(Parent parent) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        refreshTokenRepository.save(RefreshToken.builder()
                .parent(parent)
                .tokenHash(hash(token))
                .expiresAt(OffsetDateTime.now().plusNanos(refreshExpirationMs * 1_000_000))
                .build());
        return token;
    }

    /**
     * 회전 — 기존 토큰을 폐기하고 새로 발급한다.
     *
     * <p>한 번 쓴 토큰을 계속 살려두면 탈취본과 정상 사용자가 같은 토큰을 나눠 쓰게 되고,
     * 그 상태를 서버가 구분할 수 없다.
     */
    @Transactional
    public RotationResult rotate(String rawToken) {
        RefreshToken saved = refreshTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));
        if (!saved.isUsable()) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        saved.revoke();

        Parent parent = saved.getParent();
        return new RotationResult(parent, issue(parent));
    }

    /** 로그아웃 — 해당 토큰만 무효화한다. 없는 토큰이어도 조용히 넘긴다(이미 끊긴 상태이므로). */
    @Transactional
    public void revoke(String rawToken) {
        refreshTokenRepository.findByTokenHash(hash(rawToken))
                .filter(RefreshToken::isUsable)
                .ifPresent(RefreshToken::revoke);
    }

    /**
     * 해당 보호자의 유효 토큰을 모두 무효화한다.
     * 비밀번호 변경처럼 계정이 털렸다고 판단한 시점에 기존 세션을 끊기 위해 쓴다.
     */
    @Transactional
    public void revokeAll(UUID parentId) {
        refreshTokenRepository.findAllByParentIdAndRevokedAtIsNull(parentId)
                .forEach(RefreshToken::revoke);
    }

    private String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", e);
        }
    }

    public record RotationResult(Parent parent, String refreshToken) {
    }
}