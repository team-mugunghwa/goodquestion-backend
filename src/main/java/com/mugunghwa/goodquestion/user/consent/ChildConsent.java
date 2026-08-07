package com.mugunghwa.goodquestion.user.consent;

import com.mugunghwa.goodquestion.user.child.Child;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/** 아동 개인정보 처리 동의. 유효 동의 없으면 새 세션 시작 불가. */
@Entity
@Table(name = "child_consents")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChildConsent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "child_id", nullable = false)
    private Child child;

    @Column(name = "consent_version", nullable = false, length = 30)
    private String consentVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_method", nullable = false, length = 30)
    private VerificationMethod verificationMethod;

    @Column(name = "consented_at", nullable = false)
    private OffsetDateTime consentedAt;

    @Column(name = "withdrawn_at")
    private OffsetDateTime withdrawnAt;

    @Builder
    public ChildConsent(Child child, String consentVersion, VerificationMethod verificationMethod) {
        this.child = child;
        this.consentVersion = consentVersion;
        this.verificationMethod = verificationMethod;
        this.consentedAt = OffsetDateTime.now();
    }

    public void withdraw() {
        this.withdrawnAt = OffsetDateTime.now();
    }
}
