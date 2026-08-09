package com.mugunghwa.goodquestion.session.session;

import com.mugunghwa.goodquestion.global.error.BusinessException;
import com.mugunghwa.goodquestion.session.session.dto.SceneAdvanceResponse;
import com.mugunghwa.goodquestion.session.session.dto.SessionResponse;
import com.mugunghwa.goodquestion.session.session.dto.SessionStartRequest;
import io.github.cdimascio.dotenv.Dotenv;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@SpringBootTest
@Transactional
class SessionServiceTest {

    /**
     * 테스트는 main()을 거치지 않아 .env가 로딩되지 않는다.
     * 스프링 컨텍스트가 뜨기 전에 직접 읽어 시스템 속성으로 넣는다.
     */
    static {
        Dotenv.configure().ignoreIfMissing().load()
                .entries().forEach(e -> System.setProperty(e.getKey(), e.getValue()));
    }

    // 로컬 DB에 직접 넣어둔 테스트 데이터
    private static final UUID PARENT_ID = UUID.fromString("aaaaaaaa-1111-1111-1111-111111111111");
    private static final UUID CHILD_ID = UUID.fromString("bbbbbbbb-2222-2222-2222-222222222222");
    private static final UUID STORY_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Autowired
    private SessionService sessionService;

    @Test
    void 세션을_시작하면_첫_장면이_반환된다() {
        SessionResponse response = sessionService.start(
                PARENT_ID, CHILD_ID, new SessionStartRequest(STORY_ID));

        assertThat(response.sessionId()).isNotNull();
        assertThat(response.status()).isEqualTo(SessionStatus.IN_PROGRESS);
        assertThat(response.currentScene().sceneOrder()).isEqualTo((short) 1);
        assertThat(response.currentChildTurnCount()).isZero();
    }

    @Test
    void 첫_장면이_STORY면_openingMessage가_없다() {
        SessionResponse response = sessionService.start(
                PARENT_ID, CHILD_ID, new SessionStartRequest(STORY_ID));

        assertThat(response.openingMessage()).isNull();
    }

    @Test
    void STORY_장면을_완료하면_다음_장면으로_이동한다() {
        SessionResponse started = sessionService.start(
                PARENT_ID, CHILD_ID, new SessionStartRequest(STORY_ID));

        SceneAdvanceResponse response =
                sessionService.completeStoryScene(PARENT_ID, started.sessionId());

        assertThat(response.postActivity()).isFalse();
        assertThat(response.nextScene().sceneOrder()).isEqualTo((short) 2);
        assertThat(response.openingMessage()).isNull();   // 2번도 STORY 장면
    }

    @Test
    void 다음_장면이_DIALOGUE면_캐릭터_첫_대사가_반환된다() {
        SessionResponse started = sessionService.start(
                PARENT_ID, CHILD_ID, new SessionStartRequest(STORY_ID));

        // 1번(STORY) → 2번(STORY) → 3번(DIALOGUE)
        sessionService.completeStoryScene(PARENT_ID, started.sessionId());
        SceneAdvanceResponse response =
                sessionService.completeStoryScene(PARENT_ID, started.sessionId());

        assertThat(response.nextScene().sceneOrder()).isEqualTo((short) 3);
        assertThat(response.openingMessage()).isNotNull();
        assertThat(response.openingMessage().text()).isNotBlank();
    }

    @Test
    void DIALOGUE_장면에서는_STORY_완료를_호출할_수_없다() {
        SessionResponse started = sessionService.start(
                PARENT_ID, CHILD_ID, new SessionStartRequest(STORY_ID));

        sessionService.completeStoryScene(PARENT_ID, started.sessionId());
        sessionService.completeStoryScene(PARENT_ID, started.sessionId());   // 3번 DIALOGUE 도착

        assertThatThrownBy(() ->
                sessionService.completeStoryScene(PARENT_ID, started.sessionId()))
                .isInstanceOf(BusinessException.class);
    }
}