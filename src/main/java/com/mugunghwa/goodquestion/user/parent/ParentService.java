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

    /** 최초 로그인 후 프로필 등록. 이미 있으면 그대로 반환(멱등). */
    @Transactional
    public ParentResponse register(UUID parentId, ParentCreateRequest request) {
        return ParentResponse.from(parentRepository.findById(parentId)
                .orElseGet(() -> parentRepository.save(
                        Parent.builder().id(parentId).name(request.name()).build())));
    }

    public ParentResponse getMe(UUID parentId) {
        return parentRepository.findById(parentId)
                .map(ParentResponse::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "보호자 프로필이 없습니다."));
    }
}
