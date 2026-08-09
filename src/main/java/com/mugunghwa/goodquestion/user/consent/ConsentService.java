package com.mugunghwa.goodquestion.user.consent;

import com.mugunghwa.goodquestion.global.error.BusinessException;
import com.mugunghwa.goodquestion.global.error.ErrorCode;
import com.mugunghwa.goodquestion.user.child.Child;
import com.mugunghwa.goodquestion.user.consent.dto.ConsentCreateRequest;
import com.mugunghwa.goodquestion.user.consent.dto.ConsentResponse;
import com.mugunghwa.goodquestion.user.consent.dto.ConsentStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 아동 개인정보 처리 동의. ChildService가 이 서비스를 의존하므로(getStatus),
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

    /** 현재 유효 동의 + 전체 이력(계정-10). */
    public ConsentStatusResponse getStatusDetail(UUID childId) {
        List<ChildConsent> history = consentRepository.findAllByChildIdOrderByConsentedAtDesc(childId);
        ConsentResponse current = history.stream()
                .filter(c -> c.getWithdrawnAt() == null)
                .findFirst()
                .map(ConsentResponse::from)
                .orElse(null);
        return new ConsentStatusResponse(current, history.stream().map(ConsentResponse::from).toList());
    }

    /** 아이 목록의 동의 뱃지용 파생 상태. */
    public ConsentStatus getStatus(UUID childId) {
        if (consentRepository.existsByChildIdAndWithdrawnAtIsNull(childId)) {
            return ConsentStatus.VALID;
        }
        return consentRepository.existsByChildId(childId) ? ConsentStatus.WITHDRAWN : ConsentStatus.NONE;
    }

    /** 현재 유효한 동의를 철회한다. 이후 신규 세션은 차단된다(계정-13). */
    @Transactional
    public ConsentResponse withdrawCurrent(UUID childId) {
        ChildConsent consent = consentRepository
                .findFirstByChildIdAndWithdrawnAtIsNullOrderByConsentedAtDesc(childId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "철회할 유효한 동의가 없습니다."));
        consent.withdraw();
        return ConsentResponse.from(consent);
    }

    /** 세션 시작 전 검증에 사용 */
    public boolean hasActiveConsent(UUID childId) {
        return consentRepository.existsByChildIdAndWithdrawnAtIsNull(childId);
    }
}
