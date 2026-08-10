package com.mugunghwa.goodquestion.story.session;

import com.mugunghwa.goodquestion.global.error.BusinessException;
import com.mugunghwa.goodquestion.story.session.dto.SceneAdvanceResponse;
import com.mugunghwa.goodquestion.story.session.dto.SessionStartRequest;
import com.mugunghwa.goodquestion.story.session.dto.SessionStartResponse;
import com.mugunghwa.goodquestion.support.IntegrationTest;
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
     * Flyway가 적용하는 R__2_seed_demo_data.sql의 데모 계정을 전제한다 - 빈 DB로 시작해도 자동으로 들어간다.
     * 보호자 "김보호" / 아이 "지우"(유효 동의) / 이야기 "방귀 뀌는 며느리"(PUBLISHED).
     */
    private static final UUID PARENT_ID = UUID.fromString("99999999-9999-9999-9999-000000000001");
    private static final UUID CHILD_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-000000000001");
    private static final UUID STORY_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Autowired
    private SessionService sessionService;

    @Autowired
    private MessageService messageService;

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

        // 고정 첫 대사는 DIALOGUE 장면에서만 messages에 남는다(캐릭터-14)
        assertThat(messageService.getMessages(response.sessionId(), null)).isEmpty();
    }

    @Test
    void STORY_장면을_완료하면_다음_장면으로_이동한다() {
        SessionStartResponse started = sessionService.start(
                PARENT_ID, CHILD_ID, new SessionStartRequest(STORY_ID));

        SceneAdvanceResponse response =
                sessionService.completeStoryScene(PARENT_ID, started.sessionId());

        assertThat(response.phase()).isEqualTo(PlayPhase.STORY);
        assertThat(response.currentScene().sceneOrder()).isEqualTo((short) 2);
        assertThat(response.openingMessage()).isNull();   // 2번도 STORY 장면
    }

    @Test
    void 다음_장면이_DIALOGUE면_캐릭터_첫_대사가_반환된다() {
        SessionStartResponse started = sessionService.start(
                PARENT_ID, CHILD_ID, new SessionStartRequest(STORY_ID));

        // 1번(STORY) -> 2번(STORY) -> 3번(DIALOGUE)
        sessionService.completeStoryScene(PARENT_ID, started.sessionId());
        SceneAdvanceResponse response =
                sessionService.completeStoryScene(PARENT_ID, started.sessionId());

        assertThat(response.phase()).isEqualTo(PlayPhase.DIALOGUE);
        assertThat(response.currentScene().sceneOrder()).isEqualTo((short) 3);
        assertThat(response.openingMessage()).isNotNull();
        assertThat(response.openingMessage().text()).isNotBlank();
    }

    @Test
    void DIALOGUE_장면에서는_STORY_완료를_호출할_수_없다() {
        SessionStartResponse started = sessionService.start(
                PARENT_ID, CHILD_ID, new SessionStartRequest(STORY_ID));

        sessionService.completeStoryScene(PARENT_ID, started.sessionId());
        sessionService.completeStoryScene(PARENT_ID, started.sessionId());   // 3번 DIALOGUE 도착

        assertThatThrownBy(() ->
                sessionService.completeStoryScene(PARENT_ID, started.sessionId()))
                .isInstanceOf(BusinessException.class);
    }
}
