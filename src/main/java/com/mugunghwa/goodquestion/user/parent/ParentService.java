package com.mugunghwa.goodquestion.user.parent;

import com.mugunghwa.goodquestion.global.error.BusinessException;
import com.mugunghwa.goodquestion.global.error.ErrorCode;
import com.mugunghwa.goodquestion.user.parent.dto.ParentUpdateRequest;
import com.mugunghwa.goodquestion.user.parent.dto.ParentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ParentService {

    private final ParentRepository parentRepository;

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
}
