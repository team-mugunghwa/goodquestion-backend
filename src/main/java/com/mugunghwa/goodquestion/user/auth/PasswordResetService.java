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
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PasswordResetService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final ParentRepository parentRepository;
    private final PasswordResetTokenStore tokenStore;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;

    @Value("${app.frontend-base-url:http://localhost:7357}")
    private String frontendBaseUrl;

    @Value("${app.mail-from:no-reply@goodquestion.local}")
    private String mailFrom;

    @Value("${app.auth.password-reset-token-ttl:30m}")
    private Duration tokenTtl;

    /** 계정 존재 여부를 응답으로 노출하지 않는다. */
    public void request(String email) {
        Parent parent = parentRepository.findByEmail(email.trim().toLowerCase())
                .filter(Parent::isLocal)
                .orElse(null);
        if (parent == null) return;

        String rawToken = newToken();
        tokenStore.save(hash(rawToken), parent.getId(), Instant.now().plus(tokenTtl));

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(parent.getEmail());
        message.setSubject("[GoodQuestion] 비밀번호 재설정");
        message.setText("아래 링크에서 30분 이내에 새 비밀번호를 설정해 주세요.\n\n"
                + frontendBaseUrl + "/auth/reset-password?token=" + rawToken
                + "\n\n요청하지 않았다면 이 메일을 무시해 주세요.");
        try {
            mailSender.send(message);
        } catch (MailException error) {
            throw new BusinessException(ErrorCode.EMAIL_DELIVERY_FAILED);
        }
    }

    @Transactional
    public void confirm(String rawToken, String newPassword) {
        UUID parentId = tokenStore.consume(hash(rawToken), Instant.now())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_PASSWORD_RESET_TOKEN));
        Parent parent = parentRepository.findById(parentId)
                .filter(Parent::isLocal)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_PASSWORD_RESET_TOKEN));
        parent.updatePassword(passwordEncoder.encode(newPassword));
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
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }
}
