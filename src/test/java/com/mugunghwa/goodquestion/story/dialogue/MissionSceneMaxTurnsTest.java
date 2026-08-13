package com.mugunghwa.goodquestion.story.dialogue;

import com.mugunghwa.goodquestion.global.vocab.ResponseMode;
import com.mugunghwa.goodquestion.global.vocab.ThinkingElement;
import com.mugunghwa.goodquestion.global.vocab.UtteranceValidity;
import com.mugunghwa.goodquestion.story.dialogue.TurnOrchestratorTest.ScriptedAnalysisLlmClient;
import com.mugunghwa.goodquestion.story.dialogue.dto.UtteranceRequest;
import com.mugunghwa.goodquestion.story.dialogue.dto.UtteranceResponse;
import com.mugunghwa.goodquestion.story.session.SceneClosedEvent;
import com.mugunghwa.goodquestion.story.session.SceneEndReason;
import com.mugunghwa.goodquestion.story.session.SessionService;
import com.mugunghwa.goodquestion.story.session.dto.SessionStartRequest;
import com.mugunghwa.goodquestion.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 미션 필수 장면(대화3)의 종료 보류와 최대 턴의 관계를 확인한다.
 *
 * <p>미션 장면의 종료 조건은 "요소 충족 && 미션 완료"라 GOAL_MET 종료가 보류되는데,
 * 판정 순서상 요소를 다 채운 턴은 최대 턴에 닿아도 GOAL_MET으로 나온다. 마지막 턴까지
 * 보류하면 장면이 열린 채 턴 수만 최대치가 되고, 이후 발화는 전부 진입 검사의
 * 409 MAX_TURNS_EXCEEDED에 걸려 미션 수행 발화조차 보낼 수 없다(stop 외 탈출 불가).
 * 그 갇힘의 회귀 테스트다.
 */
@IntegrationTest
@Transactional
@RecordApplicationEvents
@Import(TurnOrchestratorTest.StubLlmConfig.class)
class MissionSceneMaxTurnsTest {

    /** R__2_seed_demo_data.sql의 데모 계정. 보호자 "김보호" / 아이 "지우". */
    private static final UUID PARENT_ID = UUID.fromString("99999999-9999-9999-9999-000000000001");
    private static final UUID CHILD_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-000000000001");
    private static final UUID STORY_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    /** R__1_seed_content.sql의 대화3(장면 7). 필수 요소 4개, 최소 3턴, 최대 5턴, 미션1. */
    private static final UUID MISSION_SCENE_ID = UUID.fromString("33333333-3333-3333-3333-000000000007");
    private static final String MISSION_ID = "mission_1";

    @Autowired
    private TurnOrchestrator orchestrator;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private ScriptedAnalysisLlmClient analysisLlmClient;

    @Autowired
    private ApplicationEvents events;

    private UUID sessionId;

    /** 장면 7(대화3)까지 진행해 둔다. 앞의 대화 장면 둘은 목표 달성으로 통과한다. */
    @BeforeEach
    void 미션_장면까지_진행한다() {
        analysisLlmClient.reset();
        sessionId = sessionService.start(PARENT_ID, CHILD_ID, new SessionStartRequest(STORY_ID))
                .sessionId();
        sessionService.completeStoryScene(PARENT_ID, sessionId);   // 1 도입
        sessionService.completeStoryScene(PARENT_ID, sessionId);   // 2 전개1

        // 3 대화1 (PERSPECTIVE, EMOTION, REASON, SOLUTION / 최소 2턴)
        analysisLlmClient.willDetect(UtteranceValidity.VALID, ThinkingElement.EMOTION);
        submit("며느리가 많이 힘들 것 같아요");
        analysisLlmClient.willDetect(UtteranceValidity.VALID,
                ThinkingElement.PERSPECTIVE, ThinkingElement.REASON, ThinkingElement.SOLUTION);
        submit("가족들도 이해해 줄 테니, 계속 참으면 아프니까 솔직하게 말해 보세요");

        sessionService.completeStoryScene(PARENT_ID, sessionId);   // 4 전개2

        // 5 대화2 (PERSPECTIVE·EMPATHY·REASON·REQUEST, 최소 3턴)
        analysisLlmClient.willDetect(UtteranceValidity.VALID,
                ThinkingElement.PERSPECTIVE, ThinkingElement.EMPATHY);
        submit("며느리 입장에서는 참기 힘들었을 거예요");
        analysisLlmClient.willDetect(UtteranceValidity.VALID, ThinkingElement.REASON);
        submit("방귀는 참으면 병이 나니까요");
        analysisLlmClient.willDetect(UtteranceValidity.VALID, ThinkingElement.REQUEST);
        submit("며느리를 이해해 주세요");

        sessionService.completeStoryScene(PARENT_ID, sessionId);   // 6 전개3

        assertThat(sessionService.getSession(PARENT_ID, sessionId).currentScene().sceneOrder())
                .isEqualTo((short) 7);
    }

    private UtteranceResponse submit(String text) {
        return orchestrator.processUtterance(PARENT_ID, sessionId,
                new UtteranceRequest(text, null, null, null, null));
    }

    private UtteranceResponse submitMission(String text) {
        return orchestrator.processUtterance(PARENT_ID, sessionId,
                new UtteranceRequest(text, null, null, null, MISSION_ID));
    }

    /** 2턴 만에 필수 요소 4개를 다 채운다. 미션은 노출만 되고 수행 전이다. */
    private void 요소를_다_채운다() {
        analysisLlmClient.willDetect(UtteranceValidity.VALID, ThinkingElement.SOLUTION);
        submit("며느리 방귀로 배나무를 흔들어요");
        analysisLlmClient.willDetect(UtteranceValidity.VALID,
                ThinkingElement.REASON, ThinkingElement.REQUEST, ThinkingElement.RESULT);
        submit("방귀 힘이 세니까 며느리에게 부탁하고 사람들은 피하면 배가 우수수 떨어져요");
    }

    @Test
    void 요소를_다_채워도_미션이_미완료면_최대_턴_전에는_닫지_않는다() {
        요소를_다_채운다();

        analysisLlmClient.willDetect(UtteranceValidity.SHORT);
        UtteranceResponse response = submit("음");   // 3턴 - 최소 대화량 충족, GOAL_MET 보류

        assertThat(response.progress().mode()).isEqualTo(ResponseMode.NORMAL);
        assertThat(response.sceneTransition()).isNull();
    }

    /** 갇힘 재현: 요소 전부 충족 + 미션 미완료 + 마지막 턴이면 보류 대신 최대 턴 종료로 닫아야 한다. */
    @Test
    void 미션이_미완료라도_최대_턴에_닿으면_보류하지_않고_닫는다() {
        요소를_다_채운다();
        analysisLlmClient.willDetect(UtteranceValidity.SHORT);
        submit("음");        // 3턴 - 보류
        analysisLlmClient.willDetect(UtteranceValidity.SHORT);
        submit("몰라요");     // 4턴 - 보류

        analysisLlmClient.willDetect(UtteranceValidity.SHORT);
        UtteranceResponse response = submit("그냥요");   // 5턴 == max_turns

        assertThat(response.progress().mode()).isEqualTo(ResponseMode.CLOSING);
        assertThat(response.sceneTransition().closingReason()).isEqualTo(SceneEndReason.MAX_TURNS);
        assertThat(response.sceneTransition().nextSceneOrder()).isEqualTo(8);
        assertThat(sessionService.getSession(PARENT_ID, sessionId).currentScene().sceneOrder())
                .isEqualTo((short) 8);

        // 요소는 다 채웠지만 미션이 미완료다 - 목표 달성이 아니므로 장면 보너스 자격도 없다.
        assertThat(events.stream(SceneClosedEvent.class)
                .filter(event -> event.sceneId().equals(MISSION_SCENE_ID)))
                .singleElement()
                .satisfies(event -> assertThat(event.bonusEligible()).isFalse());
    }

    @Test
    void 마지막_턴의_미션_수행_발화는_목표_달성으로_닫는다() {
        요소를_다_채운다();
        analysisLlmClient.willDetect(UtteranceValidity.SHORT);
        submit("음");        // 3턴 - 보류
        analysisLlmClient.willDetect(UtteranceValidity.SHORT);
        submit("몰라요");     // 4턴 - 보류

        analysisLlmClient.willDetect(UtteranceValidity.VALID);
        UtteranceResponse response =
                submitMission("며느리에게 방귀로 배를 떨어뜨려 달라고 부탁해요");   // 5턴, 미션 수행

        assertThat(response.progress().mode()).isEqualTo(ResponseMode.CLOSING);
        assertThat(response.sceneTransition().closingReason()).isEqualTo(SceneEndReason.GOAL_MET);
        assertThat(response.sceneTransition().nextSceneOrder()).isEqualTo(8);
    }
}
