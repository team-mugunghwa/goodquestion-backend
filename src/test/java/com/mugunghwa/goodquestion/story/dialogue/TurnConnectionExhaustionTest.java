package com.mugunghwa.goodquestion.story.dialogue;

import com.mugunghwa.goodquestion.ai.analysis.AnalysisLlmClient;
import com.mugunghwa.goodquestion.ai.character.CharacterLlmClient;
import com.mugunghwa.goodquestion.story.dialogue.dto.UtteranceRequest;
import com.mugunghwa.goodquestion.story.session.SessionService;
import com.mugunghwa.goodquestion.story.session.dto.SessionStartRequest;
import com.mugunghwa.goodquestion.support.IntegrationTest;
import com.mugunghwa.goodquestion.support.TestSessions;
import com.mugunghwa.goodquestion.story.session.StorySessionRepository;
import com.mugunghwa.goodquestion.user.auth.AuthService;
import com.mugunghwa.goodquestion.user.auth.dto.LoginRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 대화 턴이 DB 커넥션을 얼마나 오래 쥐고 있는지, 그 사이 무관한 API가 어떻게 되는지 재현한다.
 *
 * <p>턴 처리는 {@code @Transactional} 한 덩어리라 LLM 응답을 기다리는 동안에도 커넥션을
 * 반납하지 않는다. 커넥션 풀은 앱 전체가 공유하므로 대화가 몰리면 로그인처럼 대화와 아무
 * 상관 없는 요청까지 함께 막힌다. 이 테스트는 그 연쇄를 눈에 보이게 만든다.
 *
 * <p>이 테스트는 트랜잭션 경계를 분리하기 전까지 실패했다. 고쳐야 할 동작을 먼저 적어 두고
 * 분리한 뒤 통과시킨 것이라, 다시 한 트랜잭션으로 묶는 변경이 들어오면 여기서 걸린다.
 *
 * <p>풀 크기를 2로 줄여 재현한다. 운영 기본값 10에서도 같은 일이 벌어지지만 아이 열 명이
 * 동시에 말하는 상황을 테스트로 만들 필요는 없다 - 고갈에 필요한 수가 유한하다는 것이 문제지
 * 그 수가 10이라는 것이 문제가 아니다.
 */
@IntegrationTest
@TestPropertySource(properties = {
        "spring.datasource.hikari.maximum-pool-size=2",
        // 커넥션을 못 얻으면 1초만 기다리고 포기한다. 기본값 30초로는 테스트가 30초를 센다.
        "spring.datasource.hikari.connection-timeout=1000",
})
class TurnConnectionExhaustionTest {

    /** LLM 한 번이 매달리는 시간. gpt-5-mini 추론 응답을 이 정도로 잡는다. */
    private static final Duration LLM_LATENCY = Duration.ofSeconds(2);

    /** 풀을 다 채우는 데 필요한 동시 발화 수 = 풀 크기 */
    private static final int CONCURRENT_TURNS = 2;

    private static final UUID PARENT_ID = UUID.fromString("99999999-9999-9999-9999-000000000001");
    private static final UUID STORY_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    /** 지우와 하준. 둘 다 동의가 유효해 세션을 시작할 수 있다. */
    private static final List<UUID> CHILD_IDS = List.of(
            UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-000000000001"),
            UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-000000000002"));

    @Autowired
    private StorySessionRepository storySessionRepositoryForIsolation;

    @Autowired
    private TurnOrchestrator orchestrator;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private AuthService authService;

    @Autowired
    private SlowAnalysisLlmClient slowAnalysisLlmClient;

    private ExecutorService executor;
    private List<UUID> sessionIds;

    /**
     * 아이마다 대화 장면까지 진행해 둔다. 트랜잭션 밖에서 실제로 커밋해야 다른 스레드가 본다 -
     * 그래서 이 클래스에는 {@code @Transactional}을 붙이지 않는다.
     */
    @BeforeEach
    void 대화_장면까지_진행한다() {
        // 세션 시작이 진행 중 세션을 이어받으므로(#70) 시드/앞 테스트의 잔여 세션을 치운다.
        TestSessions.stopAllInProgress(storySessionRepositoryForIsolation);
        executor = Executors.newFixedThreadPool(CONCURRENT_TURNS);
        sessionIds = CHILD_IDS.stream().map(childId -> {
            UUID sessionId = sessionService.start(PARENT_ID, childId, new SessionStartRequest(STORY_ID))
                    .sessionId();
            sessionService.completeStoryScene(PARENT_ID, sessionId);
            sessionService.completeStoryScene(PARENT_ID, sessionId);
            return sessionId;
        }).toList();
    }

    @AfterEach
    void 정리한다() {
        executor.shutdownNow();
        sessionIds.forEach(sessionId -> sessionService.stop(PARENT_ID, sessionId));
    }

    @Test
    void 대화_턴이_처리되는_동안에도_로그인은_응답해야_한다() throws Exception {
        // 발화 두 건을 동시에 던져 풀을 채운다.
        CountDownLatch turnsInFlight = new CountDownLatch(CONCURRENT_TURNS);
        slowAnalysisLlmClient.notifyOn(turnsInFlight);

        List<Future<?>> turns = sessionIds.stream()
                .<Future<?>>map(sessionId -> executor.submit(() -> submit(sessionId)))
                .toList();

        // 두 턴이 모두 DB 작업을 마치고 LLM 응답을 기다리는 상태가 될 때까지 기다린다.
        assertThat(turnsInFlight.await(10, TimeUnit.SECONDS))
                .as("발화 두 건이 LLM 대기 상태에 들어가야 재현이 성립한다")
                .isTrue();

        // 대화와 아무 상관 없는 로그인. 이 시점에 커넥션은 두 개 다 대화가 쥐고 있다.
        long startedAt = System.nanoTime();
        Throwable failure = catchLoginFailure();
        Duration elapsed = Duration.ofNanos(System.nanoTime() - startedAt);

        System.out.printf("로그인 소요 %dms, 결과 %s%n",
                elapsed.toMillis(), failure == null ? "성공" : failure.getClass().getSimpleName());

        assertThat(failure)
                .as("""
                        대화 턴이 LLM을 기다리는 동안 로그인이 커넥션을 얻지 못했다.
                        턴 처리가 @Transactional 한 덩어리라 LLM 응답을 기다리는 내내 커넥션을 쥐고 있다.
                        LLM 호출을 트랜잭션 밖으로 빼면 사라진다.""")
                .isNull();

        for (Future<?> turn : turns) {
            turn.get(30, TimeUnit.SECONDS);
        }
    }

    /**
     * 커넥션을 쥐고 있는 시간이 실제 DB 작업 시간이 아니라 LLM 대기 시간에 좌우된다는 것을
     * 턴 자체의 소요 시간으로 보인다. 이 값이 곧 커넥션 점유 시간이다.
     */
    @Test
    void 턴_하나가_커넥션을_쥐는_시간은_LLM_대기_시간에_비례한다() {
        slowAnalysisLlmClient.notifyOn(new CountDownLatch(1));

        long startedAt = System.nanoTime();
        submit(sessionIds.getFirst());
        Duration elapsed = Duration.ofNanos(System.nanoTime() - startedAt);

        System.out.printf("턴 하나 소요 %dms (LLM 두 번 = %dms)%n",
                elapsed.toMillis(), LLM_LATENCY.toMillis() * 2);

        // 분석과 캐릭터 두 번을 순차로 부르므로 최소 두 배는 걸린다.
        assertThat(elapsed).isGreaterThanOrEqualTo(LLM_LATENCY.multipliedBy(2));
    }

    private void submit(UUID sessionId) {
        orchestrator.processUtterance(PARENT_ID, sessionId,
                new UtteranceRequest("며느리가 많이 힘들 것 같아요", null, null, null, null));
    }

    /** 로그인 성공이면 null, 실패면 그 예외. */
    private Throwable catchLoginFailure() {
        try {
            authService.login(new LoginRequest("demo@goodquestion.kr", "demo1234!"), "127.0.0.1");
            return null;
        } catch (Exception e) {
            return e;
        }
    }

    // ----- LLM 대역 -----

    @TestConfiguration
    static class SlowLlmConfig {

        @Bean
        @Primary
        SlowAnalysisLlmClient slowAnalysisLlmClient() {
            return new SlowAnalysisLlmClient();
        }

        @Bean
        @Primary
        CharacterLlmClient slowCharacterLlmClient() {
            return new CharacterLlmClient(null, null) {
                @Override
                public CharacterLlmResult reply(CharacterLlmInput input) {
                    sleep(LLM_LATENCY);
                    return new CharacterLlmResult("그렇구나, 조금 더 듣고 싶어.", "WORRIED");
                }
            };
        }
    }

    /**
     * 응답이 느린 분석 대역.
     *
     * <p>호출되는 순간 래치를 내린다. 이 시점이면 아이 발화 저장이 끝나 커넥션을 이미 쥔
     * 상태이므로, 테스트는 풀이 확실히 비었을 때 로그인을 시도할 수 있다.
     */
    static class SlowAnalysisLlmClient extends AnalysisLlmClient {

        SlowAnalysisLlmClient() {
            super(null, null);
        }

        private volatile CountDownLatch entered = new CountDownLatch(0);

        void notifyOn(CountDownLatch latch) {
            this.entered = latch;
        }

        @Override
        public AnalysisLlmResult analyze(AnalysisLlmInput input) {
            entered.countDown();
            sleep(LLM_LATENCY);
            return new AnalysisLlmResult(
                    "OPINION", input.childUtterance(),
                    List.of(new AnalysisLlmResult.DetectedElementDto("EMOTION", input.childUtterance())),
                    "VALID", "slow-stub");
        }
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
