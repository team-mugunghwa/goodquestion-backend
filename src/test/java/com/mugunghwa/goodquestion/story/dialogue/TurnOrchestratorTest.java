package com.mugunghwa.goodquestion.story.dialogue;

import com.mugunghwa.goodquestion.ai.analysis.AnalysisLlmClient;
import com.mugunghwa.goodquestion.ai.character.CharacterLlmClient;
import com.mugunghwa.goodquestion.global.vocab.ResponseMode;
import com.mugunghwa.goodquestion.global.vocab.ThinkingElement;
import com.mugunghwa.goodquestion.global.vocab.UtteranceValidity;
import com.mugunghwa.goodquestion.story.content.SceneType;
import com.mugunghwa.goodquestion.story.dialogue.dto.UtteranceRequest;
import com.mugunghwa.goodquestion.story.dialogue.dto.UtteranceResponse;
import com.mugunghwa.goodquestion.story.session.SceneEndReason;
import com.mugunghwa.goodquestion.story.session.SceneTransitionTarget;
import com.mugunghwa.goodquestion.story.session.SessionService;
import com.mugunghwa.goodquestion.story.session.dto.SessionStartRequest;
import com.mugunghwa.goodquestion.support.IntegrationTest;
import com.mugunghwa.goodquestion.support.TestSessions;
import com.mugunghwa.goodquestion.story.session.StorySessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 턴 처리 파이프라인을 처음부터 끝까지 확인한다.
 *
 * <p>LLM 두 곳만 정해진 답을 돌려주는 대역으로 바꾸고 나머지는 실제 경로를 탄다 -
 * 이 파이프라인에서 틀리기 쉬운 것은 판단 하나하나가 아니라 순서라서, 저장·누적·판단·이동이
 * 실제 순서대로 맞물리는지를 봐야 한다.
 */
@IntegrationTest
@Transactional
public class TurnOrchestratorTest {

    /** R__2_seed_demo_data.sql의 데모 계정. 보호자 "김보호" / 아이 "지우". */
    private static final UUID PARENT_ID = UUID.fromString("99999999-9999-9999-9999-000000000001");
    private static final UUID CHILD_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-000000000001");
    private static final UUID STORY_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Autowired
    private StorySessionRepository storySessionRepositoryForIsolation;

    @Autowired
    private TurnOrchestrator orchestrator;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private ScriptedAnalysisLlmClient analysisLlmClient;

    private UUID sessionId;

    /** 장면 3(대화1)까지 진행해 둔다. 필수 요소 PERSPECTIVE, EMOTION, SOLUTION(충족조건 확정, #45) / 최대 4턴. */
    @BeforeEach
    void 대화_장면까지_진행한다() {
        // 세션 시작이 진행 중 세션을 이어받으므로(#70) 시드/앞 테스트의 잔여 세션을 치운다.
        TestSessions.stopAllInProgress(storySessionRepositoryForIsolation);
        analysisLlmClient.reset();
        sessionId = sessionService.start(PARENT_ID, CHILD_ID, new SessionStartRequest(STORY_ID))
                .sessionId();
        sessionService.completeStoryScene(PARENT_ID, sessionId);
        sessionService.completeStoryScene(PARENT_ID, sessionId);
    }

    private UtteranceResponse submit(String text) {
        return orchestrator.processUtterance(PARENT_ID, sessionId,
                new UtteranceRequest(text, null, null, null, null));
    }

    @Test
    void 요소가_남아_있으면_대화를_이어간다() {
        analysisLlmClient.willDetect(UtteranceValidity.VALID, ThinkingElement.EMOTION);

        UtteranceResponse response = submit("며느리가 많이 힘들 것 같아요");

        assertThat(response.progress().mode()).isEqualTo(ResponseMode.NORMAL);
        assertThat(response.progress().turnCount()).isEqualTo(1);
        assertThat(response.progress().accumulatedElements()).containsExactly(ThinkingElement.EMOTION);
        assertThat(response.progress().missingElements())
                .containsExactly(ThinkingElement.PERSPECTIVE, ThinkingElement.SOLUTION);
        assertThat(response.characterMessage().text()).isNotBlank();
        assertThat(response.sceneTransition()).isNull();
        assertThat(response.mission()).isNull();
    }

    @Test
    void 요소를_다_채우고_최소_대화량을_넘기면_장면이_닫히고_다음_장면으로_넘어간다() {
        analysisLlmClient.willDetect(UtteranceValidity.VALID, ThinkingElement.EMOTION);
        submit("며느리가 많이 힘들 것 같아요");

        analysisLlmClient.willDetect(UtteranceValidity.VALID,
                ThinkingElement.PERSPECTIVE, ThinkingElement.SOLUTION);
        UtteranceResponse response = submit("가족들도 이해해 줄 테니 솔직하게 말해 보세요");

        assertThat(response.progress().mode()).isEqualTo(ResponseMode.CLOSING);
        assertThat(response.progress().missingElements()).isEmpty();
        // 마무리 대사는 콘텐츠의 고정 대사다. 어떻게 끝났든 이야기는 같은 자리로 돌아온다.
        assertThat(response.characterMessage().text()).isEqualTo("그래도 아직은 못 말하겠어. 조금만 더 참아 볼게.");
        // 목표 달성 종료에는 짧은 반응이 없다 - 마무리 대사가 자연스러운 연결이다.
        assertThat(response.closingReaction()).isNull();

        assertThat(response.sceneTransition().next()).isEqualTo(SceneTransitionTarget.SCENE);
        assertThat(response.sceneTransition().closingReason()).isEqualTo(SceneEndReason.GOAL_MET);
        assertThat(response.sceneTransition().nextSceneOrder()).isEqualTo(4);
        assertThat(response.sceneTransition().nextSceneType()).isEqualTo(SceneType.STORY);
        // 결과 연출은 대화3에만 있다. 대화1이 닫힐 때는 내려가지 않는다.
        assertThat(response.sceneTransition().resultImageUrl()).isNull();

        // 세션은 이미 다음 장면에 있고 장면 단위 누적은 초기화됐다.
        assertThat(sessionService.getSession(PARENT_ID, sessionId).currentScene().sceneOrder())
                .isEqualTo((short) 4);
        assertThat(sessionService.getTurnState(PARENT_ID, sessionId).progress().turnCount()).isZero();
    }

    @Test
    void 대화가_멈추면_남은_요소를_유도한다() {
        analysisLlmClient.willDetect(UtteranceValidity.SHORT);
        submit("몰라요");

        analysisLlmClient.willDetect(UtteranceValidity.SHORT);
        UtteranceResponse response = submit("음");

        assertThat(response.progress().mode()).isEqualTo(ResponseMode.GUIDED);
        assertThat(response.progress().guidanceTarget()).isEqualTo(ThinkingElement.PERSPECTIVE);
    }

    @Test
    void 요소가_남아도_최대_턴에_닿으면_장면을_닫는다() {
        for (int i = 0; i < 4; i++) {
            analysisLlmClient.willDetect(UtteranceValidity.SHORT);
        }
        submit("몰라요");
        submit("음");
        submit("그냥요");
        UtteranceResponse response = submit("모르겠어요");

        assertThat(response.progress().mode()).isEqualTo(ResponseMode.CLOSING);
        assertThat(response.sceneTransition().closingReason()).isEqualTo(SceneEndReason.MAX_TURNS);
        // 최대 턴 종료는 아이의 마지막 말에 대한 짧은 반응이 마무리 대사 앞에 붙는다
        // (콘텐츠 문서 "짧게 반응 -> 마지막 대사", 2026-08 확정).
        assertThat(response.closingReaction()).isNotNull();
        assertThat(response.closingReaction().text()).isEqualTo("그렇구나, 조금 더 듣고 싶어.");
        assertThat(response.characterMessage().text()).isEqualTo("그래도 아직은 못 말하겠어. 조금만 더 참아 볼게.");
    }

    @Test
    void 아이_발화와_캐릭터_대사가_대화_기록에_남는다() {
        analysisLlmClient.willDetect(UtteranceValidity.VALID, ThinkingElement.EMOTION);
        UtteranceResponse response = submit("많이 답답하겠어요");

        assertThat(response.childMessage().text()).isEqualTo("많이 답답하겠어요");
        assertThat(response.analysis().detectedElements()).hasSize(1);
        assertThat(response.analysis().detectedElements().getFirst().type())
                .isEqualTo(ThinkingElement.EMOTION);
    }

    // ----- LLM 대역 -----

    @TestConfiguration
    public static class StubLlmConfig {

        @Bean
        @Primary
        ScriptedAnalysisLlmClient scriptedAnalysisLlmClient() {
            return new ScriptedAnalysisLlmClient();
        }

        @Bean
        @Primary
        CharacterLlmClient stubCharacterLlmClient() {
            return new CharacterLlmClient(null, null) {
                @Override
                public CharacterLlmResult reply(CharacterLlmInput input) {
                    return new CharacterLlmResult("그렇구나, 조금 더 듣고 싶어.", "WORRIED");
                }
            };
        }
    }

    /**
     * 턴마다 어떤 요소가 확인될지 테스트가 미리 정해 두는 분석 대역.
     * 근거는 아이 발화 원문을 그대로 쓴다 - 후처리의 근거 검증을 통과시키기 위해서다.
     */
    public static class ScriptedAnalysisLlmClient extends AnalysisLlmClient {

        ScriptedAnalysisLlmClient() {
            super(null, null);
        }

        private record Script(UtteranceValidity validity, List<ThinkingElement> elements) {}

        private final Deque<Script> scripts = new ArrayDeque<>();

        public void reset() {
            scripts.clear();
        }

        public void willDetect(UtteranceValidity validity, ThinkingElement... elements) {
            scripts.add(new Script(validity, List.of(elements)));
        }

        @Override
        public AnalysisLlmResult analyze(AnalysisLlmInput input) {
            Script script = scripts.poll();
            if (script == null) {
                throw new IllegalStateException("대역에 준비된 분석 결과가 없다: " + input.childUtterance());
            }
            return new AnalysisLlmResult(
                    "OPINION",
                    input.childUtterance(),
                    script.elements().stream()
                            .map(type -> new AnalysisLlmResult.DetectedElementDto(
                                    type.name(), input.childUtterance()))
                            .toList(),
                    script.validity().name(),
                    "stub-model");
        }
    }
}
