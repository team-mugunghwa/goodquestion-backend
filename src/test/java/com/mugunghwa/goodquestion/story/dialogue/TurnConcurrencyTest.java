package com.mugunghwa.goodquestion.story.dialogue;

import com.mugunghwa.goodquestion.ai.analysis.AnalysisLlmClient;
import com.mugunghwa.goodquestion.ai.character.CharacterLlmClient;
import com.mugunghwa.goodquestion.global.error.BusinessException;
import com.mugunghwa.goodquestion.global.error.ErrorCode;
import com.mugunghwa.goodquestion.story.dialogue.dto.UtteranceRequest;
import com.mugunghwa.goodquestion.story.session.SessionService;
import com.mugunghwa.goodquestion.story.session.dto.SessionStartRequest;
import com.mugunghwa.goodquestion.support.IntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 같은 세션에 발화가 겹쳤을 때 어떤 실패가 나가는지 확인한다.
 *
 * <p>아이가 마이크 버튼을 연타하면 한 세션에 두 턴이 겹친다. 트랜잭션을 쪼개면서 이 경로가
 * 늘었다 - 전에는 턴 전체가 한 트랜잭션이라 낙관적 락이 먼저 잡았지만, 지금은 발화 저장이 먼저
 * 커밋되므로 messages의 {@code unique (session_id, turn_order)}가 걸릴 수 있다.
 *
 * <p>검사하는 것은 "충돌이 반드시 난다"가 아니라 <b>실패의 모양</b>이다. 겹친 요청이 어떻게
 * 끝나든 클라이언트는 재시도해도 되는지 알 수 있어야 하고, 그 답은 409다. 무결성 충돌이
 * 그대로 올라와 500이 되면 클라이언트는 아무것도 판단할 수 없다.
 */
@IntegrationTest
class TurnConcurrencyTest {

    /** 장면 3(대화1)의 최대 턴이 4라 그보다 적게 던진다. 최대 턴 초과는 다른 실패다. */
    private static final int CONCURRENT_TURNS = 3;

    private static final UUID PARENT_ID = UUID.fromString("99999999-9999-9999-9999-000000000001");
    private static final UUID CHILD_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-000000000001");
    private static final UUID STORY_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Autowired
    private TurnOrchestrator orchestrator;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private BarrierAnalysisLlmClient analysisLlmClient;

    private ExecutorService executor;
    private UUID sessionId;

    @BeforeEach
    void 대화_장면까지_진행한다() {
        executor = Executors.newFixedThreadPool(CONCURRENT_TURNS);
        analysisLlmClient.releaseTogether(CONCURRENT_TURNS);
        sessionId = sessionService.start(PARENT_ID, CHILD_ID, new SessionStartRequest(STORY_ID))
                .sessionId();
        sessionService.completeStoryScene(PARENT_ID, sessionId);
        sessionService.completeStoryScene(PARENT_ID, sessionId);
    }

    @AfterEach
    void 정리한다() {
        executor.shutdownNow();
        sessionService.stop(PARENT_ID, sessionId);
    }

    @Test
    void 겹친_발화는_재시도_가능한_실패로_끝난다() throws Exception {
        List<Future<Throwable>> attempts = new ArrayList<>();
        for (int i = 0; i < CONCURRENT_TURNS; i++) {
            attempts.add(executor.submit(submitAndCatch("겹치는 발화")));
        }

        List<Throwable> failures = new ArrayList<>();
        for (Future<Throwable> attempt : attempts) {
            Throwable failure = attempt.get(30, TimeUnit.SECONDS);
            if (failure != null) {
                failures.add(failure);
            }
        }

        System.out.printf("동시 발화 %d건 중 실패 %d건: %s%n",
                CONCURRENT_TURNS, failures.size(),
                failures.stream().map(f -> f.getClass().getSimpleName()).toList());

        assertThat(failures)
                .as("""
                        겹친 발화가 재시도 가능한 실패(409)로 끝나야 한다.
                        무결성 충돌이 그대로 올라오면 500이 되고, 클라이언트는 다시 보내도 되는지 알 수 없다.""")
                .allMatch(this::mapsToConflict);
    }

    /** 409로 내려가는 실패인지. 낙관적 락은 전역 핸들러가, 무결성 충돌은 오케스트레이터가 바꾼다. */
    private boolean mapsToConflict(Throwable failure) {
        if (failure instanceof OptimisticLockingFailureException) {
            return true;
        }
        return failure instanceof BusinessException business
                && business.getErrorCode() == ErrorCode.CONCURRENT_TURN;
    }

    /** 성공이면 null, 실패면 그 예외를 돌려준다. */
    private Callable<Throwable> submitAndCatch(String text) {
        return () -> {
            try {
                orchestrator.processUtterance(PARENT_ID, sessionId,
                        new UtteranceRequest(text, null, null, null, null));
                return null;
            } catch (DataIntegrityViolationException e) {
                // 변환되지 않고 그대로 올라온 경우. 이것이 잡으려는 실패다.
                return e;
            } catch (Exception e) {
                return e;
            }
        };
    }

    // ----- LLM 대역 -----

    @TestConfiguration
    static class BarrierLlmConfig {

        @Bean
        @Primary
        BarrierAnalysisLlmClient barrierAnalysisLlmClient() {
            return new BarrierAnalysisLlmClient();
        }

        @Bean
        @Primary
        CharacterLlmClient stubCharacterLlmClient() {
            return new CharacterLlmClient() {
                @Override
                public CharacterLlmResult reply(CharacterLlmInput input) {
                    return new CharacterLlmResult("그렇구나, 조금 더 듣고 싶어.", "WORRIED");
                }
            };
        }
    }

    /**
     * 모든 스레드를 모아 두었다가 한꺼번에 내보내는 분석 대역.
     *
     * <p>분석이 끝난 직후가 저장 트랜잭션이 열리는 지점이다. 여기서 함께 출발시켜야 세 요청이
     * 같은 turn_order를 잡을 가능성이 생긴다.
     */
    static class BarrierAnalysisLlmClient extends AnalysisLlmClient {

        private volatile CyclicBarrier barrier = new CyclicBarrier(1);

        void releaseTogether(int parties) {
            this.barrier = new CyclicBarrier(parties);
        }

        @Override
        public AnalysisLlmResult analyze(AnalysisLlmInput input) {
            try {
                barrier.await(10, TimeUnit.SECONDS);
            } catch (Exception e) {
                throw new IllegalStateException("동시 출발에 실패했다", e);
            }
            return new AnalysisLlmResult(
                    "OPINION", input.childUtterance(),
                    List.of(new AnalysisLlmResult.DetectedElementDto("EMOTION", input.childUtterance())),
                    "VALID", "barrier-stub");
        }
    }
}
