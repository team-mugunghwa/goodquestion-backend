package com.mugunghwa.goodquestion.learning.reward;

import com.mugunghwa.goodquestion.user.auth.ParentLoggedInEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 최초 로그인 별가루 지급 (ParentLoggedInEvent 참고).
 *
 * <p>환영 별가루와 달리 커밋 뒤에 받는다. 보너스를 넣다 실패했다고 로그인이 실패하면
 * 안 된다 — 로그인은 이 앱에서 가장 막히면 안 되는 경로다.
 *
 * <p>같은 이유로 예외를 여기서 먹는다. 커밋 후 콜백에서 던진 예외는 커밋을 부른 쪽까지
 * 올라가, 이미 커밋된 로그인이 500으로 나가게 된다. 선점 기록은 지급과 같은 트랜잭션이라
 * 함께 롤백되므로 다음 로그인에 다시 시도된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FirstLoginBonusListener {

    private final FirstLoginBonusService firstLoginBonusService;

    @TransactionalEventListener
    public void award(ParentLoggedInEvent event) {
        try {
            int awarded = firstLoginBonusService.grant(event.parentId());
            if (awarded > 0) {
                log.info("최초 로그인 별가루 지급: parentId={}, 아이 {}명", event.parentId(), awarded);
            }
        } catch (RuntimeException e) {
            log.error("최초 로그인 별가루 지급 실패: parentId={}", event.parentId(), e);
        }
    }
}
