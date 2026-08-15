package com.mugunghwa.goodquestion.support;

import com.mugunghwa.goodquestion.story.session.SessionStatus;
import com.mugunghwa.goodquestion.story.session.StorySessionRepository;

/**
 * 세션을 만드는 통합 테스트의 격리 도우미.
 *
 * <p>세션 시작이 진행 중 세션을 이어받게 되면서(#70) 두 가지가 테스트를 오염시킨다.
 * 데모 시드의 진행 중 세션(대화 장면에 멈춰 있음)을 start()가 그대로 돌려주고,
 * 트랜잭션 없는 테스트가 남긴 진행 중 세션을 다음 테스트가 이어받는다. 어느 쪽이든
 * "장면 1부터 시작"을 전제한 셋업이 대화 장면에서 completeStoryScene을 부르다
 * 깨진다. 셋업 첫머리에서 진행 중 세션을 전부 정지시켜 새 세션이 만들어지게 한다.
 */
public final class TestSessions {

    private TestSessions() {
    }

    /** 진행 중 세션을 전부 STOPPED로 바꾼다. 시드와 앞선 테스트의 잔여물을 함께 치운다. */
    public static void stopAllInProgress(StorySessionRepository repository) {
        repository.findAll().stream()
                .filter(session -> session.getStatus() == SessionStatus.IN_PROGRESS)
                .forEach(session -> {
                    session.stop();
                    repository.save(session);
                });
    }
}
