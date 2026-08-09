package com.mugunghwa.goodquestion.user.parent;

import com.mugunghwa.goodquestion.user.auth.AuthProvider;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 보호자 계정. Supabase Auth를 사용하지 않아 서버가 직접 발급·관리한다.
 * provider=LOCAL은 email+passwordHash로, provider=KAKAO는 providerId(카카오 회원번호)로 식별한다.
 */
@Entity
@Table(name = "parents")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Parent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(length = 255)
    private String email;

    @Column(name = "password_hash", length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuthProvider provider;

    @Column(name = "provider_id", length = 100)
    private String providerId;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private OffsetDateTime createdAt;

    @Builder
    public Parent(String email, String passwordHash, AuthProvider provider, String providerId, String name) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.provider = provider;
        this.providerId = providerId;
        this.name = name;
    }

    public static Parent ofLocal(String email, String passwordHash, String name) {
        return Parent.builder()
                .email(email).passwordHash(passwordHash).provider(AuthProvider.LOCAL).name(name).build();
    }

    public static Parent ofKakao(String providerId, String email, String name) {
        return Parent.builder()
                .providerId(providerId).email(email).provider(AuthProvider.KAKAO).name(name).build();
    }

    public void updateName(String name) {
        this.name = name;
    }

    public boolean isLocal() {
        return provider == AuthProvider.LOCAL;
    }
}
