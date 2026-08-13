package com.mugunghwa.goodquestion.user.auth;

import com.mugunghwa.goodquestion.user.parent.Parent;
import com.mugunghwa.goodquestion.user.parent.ParentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 로그인 실패 기록.
 *
 * <p>별도 트랜잭션으로 커밋한다 — 인증 실패는 예외로 알리는데, 같은 트랜잭션에서 던지면
 * 롤백되면서 실패 횟수까지 함께 사라져 계정이 영영 잠기지 않는다.
 *
 * <p>같은 클래스 안에서 호출하면 프록시를 타지 않아 REQUIRES_NEW가 무시되므로
 * AuthService와 클래스를 분리했다.
 */
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private final ParentRepository parentRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(UUID parentId) {
        parentRepository.findById(parentId).ifPresent(Parent::recordLoginFailure);
    }
}