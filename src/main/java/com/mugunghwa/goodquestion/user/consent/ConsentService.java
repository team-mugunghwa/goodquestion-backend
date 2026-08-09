package com.mugunghwa.goodquestion.user.consent;

import com.mugunghwa.goodquestion.global.error.BusinessException;
import com.mugunghwa.goodquestion.global.error.ErrorCode;
import com.mugunghwa.goodquestion.user.child.Child;
import com.mugunghwa.goodquestion.user.consent.dto.ConsentCreateRequest;
import com.mugunghwa.goodquestion.user.consent.dto.ConsentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 아동 개인정보 처리 동의. ChildService가 이 서비스를 의존하므로(hasActiveConsent),
 * 순환 의존을 피하기 위해 소유권 검증(ChildService.getOwnedChild)은 컨트롤러에서 선행하고
 * 검증된 Child를 그대로 전달받는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConsentService {

    private final ChildConsentRepository consentRepository;

    @Transactional
    public ConsentResponse create(Child child, ConsentCreateRequest request) {
        ChildConsent consent = consentRepository.save(ChildConsent.builder()
                .child(child)
                .consentVersion(request.consentVersion())
                .verificationMethod(request.verificationMethod())
                .build());
        return ConsentResponse.from(consent);
    }

    @Transactional
    public ConsentResponse withdraw(UUID childId, UUID consentId) {
        ChildConsent consent = consentRepository.findById(consentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "동의 내역을 찾을 수 없습니다."));
        if (!consent.getChild().getId().equals(childId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        consent.withdraw();
        return ConsentResponse.from(consent);
    }

    /** 세션 시작 전 검증에 사용 */
    public boolean hasActiveConsent(UUID childId) {
        return consentRepository.existsByChildIdAndWithdrawnAtIsNull(childId);
    }
}
