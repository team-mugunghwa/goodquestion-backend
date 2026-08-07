package com.mugunghwa.goodquestion.user.consent;

import com.mugunghwa.goodquestion.global.error.BusinessException;
import com.mugunghwa.goodquestion.global.error.ErrorCode;
import com.mugunghwa.goodquestion.user.consent.dto.ConsentCreateRequest;
import com.mugunghwa.goodquestion.user.consent.dto.ConsentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConsentService {

    private final ChildConsentRepository consentRepository;

    @Transactional
    public ConsentResponse create(UUID childId, ConsentCreateRequest request) {
        // NOTE: 컨트롤러에서 ChildService.getOwnedChild로 소유권 검증 후 호출
        // TODO: Child 참조를 파라미터로 받도록 정리
        throw new UnsupportedOperationException("TODO: ChildService와 연결해 구현");
    }

    @Transactional
    public ConsentResponse withdraw(UUID childId, UUID consentId) {
        ChildConsent consent = consentRepository.findById(consentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        // TODO: consent.child == childId 검증
        consent.withdraw();
        return ConsentResponse.from(consent);
    }

    /** 세션 시작 전 검증에 사용 */
    public boolean hasActiveConsent(UUID childId) {
        return consentRepository.existsByChildIdAndWithdrawnAtIsNull(childId);
    }
}
