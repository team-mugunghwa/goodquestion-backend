package com.mugunghwa.goodquestion.user.parent;

import com.mugunghwa.goodquestion.global.error.BusinessException;
import com.mugunghwa.goodquestion.global.error.ErrorCode;
import com.mugunghwa.goodquestion.user.parent.dto.ParentCreateRequest;
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

    /**
     * 프로필 이름 등록/수정.
     * NOTE: Supabase Auth 제거 이후 계정 자체는 user/auth 회원가입에서 생성되므로,
     * 이 메서드는 더 이상 "최초 생성"이 아니라 already-존재하는 계정의 이름을 갱신하는 역할로 조정했다.
     * (원래 계약: 이미 존재하면 그대로 반환(멱등) — 이름이 바뀌는 부분은 팀 확인 필요)
     */
    @Transactional
    public ParentResponse register(UUID parentId, ParentCreateRequest request) {
        Parent parent = parentRepository.findById(parentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "보호자 계정을 찾을 수 없습니다."));
        parent.updateName(request.name());
        return ParentResponse.from(parent);
    }

    public ParentResponse getMe(UUID parentId) {
        return parentRepository.findById(parentId)
                .map(ParentResponse::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "보호자 프로필이 없습니다."));
    }
}
