package com.mugunghwa.goodquestion.story.session;

import com.mugunghwa.goodquestion.story.session.dto.SceneAdvanceResponse;
import com.mugunghwa.goodquestion.story.session.dto.SessionStartRequest;
import com.mugunghwa.goodquestion.support.IntegrationTest;
import com.mugunghwa.goodquestion.support.TestSessions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 스토리 재생 완료 보고의 중복 호출을 다 본 장면 id로 가려낸다.
 *
 * <p>재전송이나 두 번 누름으로 같은 보고가 두 번 오면 장면 하나를 통째로 건너뛴다.
 * 화면은 다음 장면을 정상적으로 그리므로 아무도 눈치채지 못하고, 아이만 그 대목을
 * 못 본 채 이야기가 이어진다.
 *
 * <p>멱등키가 아니라 서버 상태로 판별한다 - 클라이언트가 헤더를 챙기지 않아도 동작하고,
 * 이미 보낸 요청인지를 기억할 필요도 없다. 상태로 알 수 있는 것은 상태로 푼다.
 */
@IntegrationTest
@Transactional
class SceneAdvanceIdempotencyTest {

    /** R__2_seed_demo_data.sql의 데모 계정. 보호자 "김보호" / 아이 "지우". */
    private static final UUID PARENT_ID = UUID.fromString("99999999-9999-9999-9999-000000000001");
    private static final UUID CHILD_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-000000000001");
    private static final UUID STORY_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Autowired
    private SessionService sessionService;

    @Autowired
    private StorySessionRepository sessionRepository;

    private UUID sessionId;
    private UUID firstSceneId;

    @BeforeEach
    void 장면_1에서_시작한다() {
        TestSessions.stopAllInProgress(sessionRepository);
        var started = sessionService.start(PARENT_ID, CHILD_ID, new SessionStartRequest(STORY_ID));
        sessionId = started.sessionId();
        firstSceneId = started.currentScene().sceneId();
    }

    @Test
    void 같은_장면을_두_번_보고해도_한_번만_전진한다() {
        SceneAdvanceResponse first = sessionService.completeStoryScene(
                PARENT_ID, sessionId, firstSceneId);
        assertThat(first.currentScene().sceneOrder()).isEqualTo((short) 2);

        // 같은 보고가 한 번 더 왔다. 이미 장면 2에 있으므로 전진하지 않는다.
        SceneAdvanceResponse duplicated = sessionService.completeStoryScene(
                PARENT_ID, sessionId, firstSceneId);

        assertThat(duplicated.currentScene().sceneOrder()).isEqualTo((short) 2);
    }

    @Test
    void 현재_장면을_보고하면_정상적으로_전진한다() {
        SceneAdvanceResponse first = sessionService.completeStoryScene(
                PARENT_ID, sessionId, firstSceneId);
        UUID secondSceneId = first.currentScene().sceneId();

        SceneAdvanceResponse second = sessionService.completeStoryScene(
                PARENT_ID, sessionId, secondSceneId);

        assertThat(second.currentScene().sceneOrder()).isEqualTo((short) 3);
    }

    /** 장면 id를 싣지 않는 구버전 클라이언트는 지금까지처럼 무조건 전진한다. */
    @Test
    void 장면_id가_없으면_전과_같이_전진한다() {
        sessionService.completeStoryScene(PARENT_ID, sessionId, null);
        SceneAdvanceResponse second = sessionService.completeStoryScene(PARENT_ID, sessionId, null);

        assertThat(second.currentScene().sceneOrder()).isEqualTo((short) 3);
    }

    /**
     * 완주한 세션에 중단이 들어와도 완료 기록을 덮지 않는다.
     *
     * <p>가드가 없으면 COMPLETED가 STOPPED로 바뀌는데 completedAt은 남아
     * "끝냈는데 중단됨"이라는 모순 상태가 된다. 화면을 닫으며 보낸 중단 요청이
     * 완주 직후에 도착하는 것은 드문 일이 아니다.
     */
    @Test
    void 완주한_세션은_중단으로_덮이지_않는다() {
        StorySession session = sessionRepository.findById(sessionId).orElseThrow();
        session.complete();

        sessionService.stop(PARENT_ID, sessionId);

        assertThat(sessionRepository.findById(sessionId).orElseThrow().getStatus())
                .isEqualTo(SessionStatus.COMPLETED);
    }

    @Test
    void 진행_중_세션은_정상적으로_중단된다() {
        sessionService.stop(PARENT_ID, sessionId);

        assertThat(sessionRepository.findById(sessionId).orElseThrow().getStatus())
                .isEqualTo(SessionStatus.STOPPED);
    }
}
