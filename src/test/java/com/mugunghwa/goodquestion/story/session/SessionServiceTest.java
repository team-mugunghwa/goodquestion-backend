package com.mugunghwa.goodquestion.story.session;

import com.mugunghwa.goodquestion.global.error.BusinessException;
import com.mugunghwa.goodquestion.story.session.dto.SceneAdvanceResponse;
import com.mugunghwa.goodquestion.story.session.dto.SceneOpeningResponse;
import com.mugunghwa.goodquestion.story.session.dto.SessionResumeResponse;
import com.mugunghwa.goodquestion.story.session.dto.SessionStartRequest;
import com.mugunghwa.goodquestion.story.session.dto.SessionStartResponse;
import com.mugunghwa.goodquestion.support.IntegrationTest;
import com.mugunghwa.goodquestion.support.TestSessions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@IntegrationTest
@Transactional
class SessionServiceTest {

    /**
     * Flyway가 적용하는 R__2_seed_demo_data.sql의 데모 계정을 전제한다 — 빈 DB로 시작해도 자동으로 들어간다.
     * 보호자 "김보호" / 아이 "지우"(유효 동의) / 이야기 "방귀 뀌는 며느리"(PUBLISHED).
     */
    private static final UUID PARENT_ID = UUID.fromString("99999999-9999-9999-9999-000000000001");
    private static final UUID CHILD_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-000000000001");
    private static final UUID STORY_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Autowired
    private SessionService sessionService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private StorySessionRepository sessionRepository;

    /**
     * 데모 시드에 대화 장면에 멈춘 진행 중 세션이 있고, start()는 진행 중 세션을
     * 이어받는다(#70). 치우지 않으면 "장면 1부터 시작"을 전제한 이 테스트들이
     * 그 세션을 받아 깨진다.
     *
     * <p>지금까지는 앞서 실행된 다른 테스트가 우연히 치워 준 덕에 통과했다.
     * 그래서 단독 실행하면 실패했고, 테스트를 하나 추가해 순서가 바뀌면 함께 깨졌다.
     * 자기 전제는 자기가 세운다.
     */
    @BeforeEach
    void 진행_중_세션을_치운다() {
        TestSessions.stopAllInProgress(sessionRepository);
    }

    @Test
    void 세션을_시작하면_첫_장면이_반환된다() {
        SessionStartResponse response = sessionService.start(
                PARENT_ID, CHILD_ID, new SessionStartRequest(STORY_ID));

        assertThat(response.sessionId()).isNotNull();
        assertThat(response.status()).isEqualTo(SessionStatus.IN_PROGRESS);
        assertThat(response.currentScene().sceneOrder()).isEqualTo((short) 1);
        assertThat(response.phase()).isEqualTo(PlayPhase.STORY);
    }

    @Test
    void 첫_장면이_STORY면_대화_기록이_남지_않는다() {
        SessionStartResponse response = sessionService.start(
                PARENT_ID, CHILD_ID, new SessionStartRequest(STORY_ID));

        assertThat(messageService.getMessages(response.sessionId(), null)).isEmpty();
    }

    @Test
    void STORY_장면을_완료하면_다음_장면으로_이동한다() {
        SessionStartResponse started = sessionService.start(
                PARENT_ID, CHILD_ID, new SessionStartRequest(STORY_ID));

        SceneAdvanceResponse response = sessionService.completeStoryScene(PARENT_ID, started.sessionId());

        assertThat(response.currentScene().sceneOrder()).isEqualTo((short) 2);
        assertThat(response.openingMessage()).isNull();   // 2번도 STORY
    }

    @Test
    void 다음_장면이_DIALOGUE면_캐릭터_첫_대사가_반환된다() {
        SessionStartResponse started = sessionService.start(
                PARENT_ID, CHILD_ID, new SessionStartRequest(STORY_ID));

        sessionService.completeStoryScene(PARENT_ID, started.sessionId());   // 1 -> 2 (STORY)
        SceneAdvanceResponse response =
                sessionService.completeStoryScene(PARENT_ID, started.sessionId());   // 2 -> 3 (DIALOGUE)

        assertThat(response.currentScene().sceneOrder()).isEqualTo((short) 3);
        assertThat(response.openingMessage()).isNotNull();
        assertThat(response.openingMessage().text()).isNotBlank();
    }

    @Test
    void DIALOGUE_장면에서는_STORY_완료를_호출할_수_없다() {
        SessionStartResponse started = sessionService.start(
                PARENT_ID, CHILD_ID, new SessionStartRequest(STORY_ID));

        sessionService.completeStoryScene(PARENT_ID, started.sessionId());
        sessionService.completeStoryScene(PARENT_ID, started.sessionId());   // 3번(DIALOGUE) 도착

        assertThatThrownBy(() -> sessionService.completeStoryScene(PARENT_ID, started.sessionId()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void 고정_첫_대사의_이름_자리표시자가_아이_이름으로_치환된다() {
        UUID sessionId = 대화_장면까지_진행한다();

        SceneOpeningResponse response = sessionService.playOpening(PARENT_ID, sessionId);

        assertThat(response.message().text()).doesNotContain("ㅇㅇ");
    }

    @Test
    void 첫_대사를_다시_요청해도_새로_저장하지_않는다() {
        UUID sessionId = 대화_장면까지_진행한다();   // 장면 전환에서 이미 저장됨

        SceneOpeningResponse response = sessionService.playOpening(PARENT_ID, sessionId);

        assertThat(response.alreadyOpened()).isTrue();
        assertThat(messageService.getMessages(sessionId, null)).hasSize(1);
    }

    @Test
    void STORY_장면에서는_첫_대사를_재생할_수_없다() {
        SessionStartResponse started = sessionService.start(
                PARENT_ID, CHILD_ID, new SessionStartRequest(STORY_ID));   // 1번(STORY)

        assertThatThrownBy(() -> sessionService.playOpening(PARENT_ID, started.sessionId()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void 이어하기는_현재_장면과_대화_내역을_함께_돌려준다() {
        UUID sessionId = 대화_장면까지_진행한다();

        SessionResumeResponse response = sessionService.resume(PARENT_ID, sessionId);

        assertThat(response.session().sessionId()).isEqualTo(sessionId);
        assertThat(response.currentScene().sceneOrder()).isEqualTo((short) 3);
        assertThat(response.messages()).isNotEmpty();
        assertThat(response.lastCharacterMessage()).isNotNull();
        assertThat(response.exposedMission()).isNull();   // story.mission 미구현
    }

    /** 시드 기준 1번(STORY) → 2번(STORY) → 3번(DIALOGUE)까지 진행한다. */
    private UUID 대화_장면까지_진행한다() {
        SessionStartResponse started = sessionService.start(
                PARENT_ID, CHILD_ID, new SessionStartRequest(STORY_ID));
        sessionService.completeStoryScene(PARENT_ID, started.sessionId());
        sessionService.completeStoryScene(PARENT_ID, started.sessionId());
        return started.sessionId();
    }
}