package com.mugunghwa.goodquestion.user.auth;

import com.mugunghwa.goodquestion.global.error.BusinessException;
import com.mugunghwa.goodquestion.global.error.ErrorCode;
import com.mugunghwa.goodquestion.user.parent.Parent;
import com.mugunghwa.goodquestion.user.parent.ParentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;

/**
 * 비밀번호 재설정 (계정-06).
 *
 * <p>RefreshTokenService와 같은 방식이다 — 원문은 저장하지 않고 해시만 남기고,
 * 소비하면 consumed_at을 찍어 재사용을 막는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PasswordResetService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final ParentRepository parentRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;

    @Value("${app.frontend-base-url}")
    private String frontendBaseUrl;

    @Value("${app.mail-from}")
    private String mailFrom;

    @Value("${app.auth.password-reset-token-ttl}")
    private Duration tokenTtl;

    /** 계정 존재 여부를 응답으로 노출하지 않는다 — 없는 이메일이거나 소셜 계정이면 조용히 넘긴다. */
    @Transactional
    public void request(String email) {
        Parent parent = parentRepository.findByEmail(email.trim().toLowerCase())
                .filter(Parent::isLocal)
                .orElse(null);
        if (parent == null) {
            return;
        }

        String rawToken = newToken();
        tokenRepository.save(PasswordResetToken.builder()
                .parent(parent)
                .tokenHash(hash(rawToken))
                .expiresAt(OffsetDateTime.now().plus(tokenTtl))
                .build());

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(parent.getEmail());
        message.setSubject("[GoodQuestion] 비밀번호 재설정");
        message.setText("아래 링크에서 " + tokenTtl.toMinutes() + "분 이내에 새 비밀번호를 설정해 주세요.\n\n"
                + frontendBaseUrl + "/auth/reset-password?token=" + rawToken
                + "\n\n요청하지 않았다면 이 메일을 무시해 주세요.");
        try {
            mailSender.send(message);
        } catch (MailException e) {
            throw new BusinessException(ErrorCode.EMAIL_DELIVERY_FAILED);
        }
    }

    /** 토큰을 소비하고 비밀번호를 바꾼다. 없거나, 만료됐거나, 이미 쓴 토큰이면 예외. */
    @Transactional
    public void confirm(String rawToken, String newPassword) {
        PasswordResetToken token = tokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_PASSWORD_RESET_TOKEN));
        if (!token.isUsable()) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD_RESET_TOKEN);
        }
        token.consume();
        token.getParent().updatePassword(passwordEncoder.encode(newPassword));
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", e);
        }
    }
}
