package com.mugunghwa.goodquestion.user.child;

import com.mugunghwa.goodquestion.global.error.BusinessException;
import com.mugunghwa.goodquestion.global.error.ErrorCode;
import com.mugunghwa.goodquestion.user.child.dto.ChildCreateRequest;
import com.mugunghwa.goodquestion.user.child.dto.ChildResponse;
import com.mugunghwa.goodquestion.user.child.dto.ChildUpdateRequest;
import com.mugunghwa.goodquestion.user.consent.ConsentService;
import com.mugunghwa.goodquestion.user.parent.Parent;
import com.mugunghwa.goodquestion.user.parent.ParentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChildService {

    private final ChildRepository childRepository;
    private final ParentRepository parentRepository;
    private final ConsentService consentService;

    @Transactional
    public ChildResponse create(UUID parentId, ChildCreateRequest request) {
        Parent parent = parentRepository.getReferenceById(parentId);
        Child child = childRepository.save(Child.builder()
                .parent(parent).name(request.name()).birthYear(request.birthYear()).build());
        return ChildResponse.of(child, false);
    }

    public List<ChildResponse> getMyChildren(UUID parentId) {
        return childRepository.findAllByParentId(parentId).stream()
                .map(c -> ChildResponse.of(c, consentService.hasActiveConsent(c.getId())))
                .toList();
    }

    public ChildResponse getChild(UUID parentId, UUID childId) {
        Child child = getOwnedChild(parentId, childId);
        return ChildResponse.of(child, consentService.hasActiveConsent(childId));
    }

    @Transactional
    public ChildResponse update(UUID parentId, UUID childId, ChildUpdateRequest request) {
        Child child = getOwnedChild(parentId, childId);
        child.update(request.name(), request.birthYear());
        return ChildResponse.of(child, consentService.hasActiveConsent(childId));
    }

    /** 아동 개인정보 삭제권 대응 — 하위 데이터는 FK cascade로 함께 삭제 */
    @Transactional
    public void delete(UUID parentId, UUID childId) {
        childRepository.delete(getOwnedChild(parentId, childId));
    }

    /** 소유권 검증 공용 메서드 — 다른 도메인 서비스에서도 사용 */
    public Child getOwnedChild(UUID parentId, UUID childId) {
        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "아이를 찾을 수 없습니다."));
        if (!child.isOwnedBy(parentId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return child;
    }
}
