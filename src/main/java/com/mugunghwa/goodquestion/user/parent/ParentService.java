package com.mugunghwa.goodquestion.user.parent;

import com.mugunghwa.goodquestion.global.error.BusinessException;
import com.mugunghwa.goodquestion.global.error.ErrorCode;
import com.mugunghwa.goodquestion.user.parent.dto.ParentUpdateRequest;
import com.mugunghwa.goodquestion.user.parent.dto.ParentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ParentService {

    private final ParentRepository parentRepository;
    private final PasswordEncoder passwordEncoder;

    /** 전달한 필드만 반영한다(명세 4-2). 계정 생성은 user/auth 회원가입이 담당한다. */
    @Transactional
    public ParentResponse update(UUID parentId, ParentUpdateRequest request) {
        Parent parent = parentRepository.findById(parentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "보호자 계정을 찾을 수 없습니다."));

        if (request.name() != null) {
            parent.updateName(request.name());
        }
        if (request.newPassword() != null) {
            // TODO: currentPassword 검증(불일치 401) 후 PasswordEncoder로 해시해 교체
            throw new UnsupportedOperationException("미구현: 비밀번호 변경");
        }
        return ParentResponse.from(parent);
    }

    public ParentResponse getMe(UUID parentId) {
        return parentRepository.findById(parentId)
                .map(ParentResponse::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "보호자 프로필이 없습니다."));
    }

    /**
     * 보호자 확인 게이트용 비밀번호 검증(F-09).
     *
     * <p>소셜 계정은 passwordHash가 없어 검증할 수 없다. 프론트가 provider를 보고
     * 게이트를 건너뛰지만, 그래도 여기 닿으면 명확히 거절한다.
     */
    public void verifyPassword(UUID parentId, String password) {
        Parent parent = parentRepository.findById(parentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "보호자를 찾을 수 없습니다."));
        if (!parent.isLocal() || parent.getPasswordHash() == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "소셜 계정은 비밀번호 확인을 쓰지 않습니다.");
        }
        if (!passwordEncoder.matches(password, parent.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
    }
}
