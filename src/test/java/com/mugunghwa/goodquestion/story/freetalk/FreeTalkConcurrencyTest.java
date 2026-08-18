package com.mugunghwa.goodquestion.story.freetalk;

import com.mugunghwa.goodquestion.global.error.BusinessException;
import com.mugunghwa.goodquestion.global.error.ErrorCode;
import com.mugunghwa.goodquestion.story.freetalk.dto.FreeTalkMessageRequest;
import com.mugunghwa.goodquestion.story.freetalk.dto.FreeTalkStartRequest;
import com.mugunghwa.goodquestion.support.IntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 같은 자유 대화에 발화가 겹쳤을 때 <b>실제 DB 트랜잭션 둘</b>이 어떻게 갈리는지 확인한다.
 *
 * <p>{@link FreeTalkIntegrationTest}의 경합 테스트는 한 트랜잭션 안에서 조건 불일치만
 * 본다 - 조건부 갱신의 판정은 맞지만 실제 행 잠금 경합은 재현하지 못한다. 그래서 이
 * 클래스는 <b>클래스 수준 트랜잭션 없이</b> 스레드를 띄운다.
 *
 * <p>검사하는 것은 "충돌이 반드시 난다"가 아니라 <b>정확히 하나만 통과한다</b>이다.
 * 둘이 통과하면 턴이 두 번 올라가 10턴 상한이 새고, 아이는 자기가 하지 않은 말에 대한
 * 답을 듣는다. 진 쪽은 재시도해도 되는지 알 수 있어야 하므로 409여야 한다.
 *
 * <p>대역 설정을 {@link StubFreeTalkConfig}로 두 클래스가 똑같이 import 하는 것은
 * 컨텍스트를 하나만 띄우기 위해서다 - 설정이 갈리면 컨텍스트가 하나 더 생기고,
 * 컨텍스트마다 붙는 커넥션 풀이 PostgreSQL의 max_connections를 넘긴다.
 */
@IntegrationTest
@Import(StubFreeTalkConfig.class)
class FreeTalkConcurrencyTest {

    private static final int CONCURRENT_TURNS = 3;

    private static final UUID PARENT_ID = UUID.fromString("99999999-9999-9999-9999-000000000001");
    private static final UUID CHILD_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-000000000001");
    private static final UUID STORY_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Autowired
    private FreeTalkService freeTalkService;

    @Autowired
    private FreeTalkRepository freeTalkRepository;

    @Autowired
    private FreeTalkMessageRepository messageRepository;

    @Autowired
    private StubFreeTalkLlmClient llmClient;

    private ExecutorService executor;
    private UUID freeTalkId;

    @BeforeEach
    void 대화를_연다() {
        executor = Executors.newFixedThreadPool(CONCURRENT_TURNS);
        // 첫 인사는 혼자 지나가야 한다 - 장벽은 겹칠 턴에만 건다.
        llmClient.reset();
        UUID characterId = freeTalkService.getCharacters(PARENT_ID, CHILD_ID, STORY_ID)
                .getFirst().characterId();
        freeTalkId = freeTalkService.start(PARENT_ID, CHILD_ID,
                new FreeTalkStartRequest(STORY_ID, characterId)).freeTalkId();
        llmClient.releaseTogether(CONCURRENT_TURNS);
    }

    /**
     * 이 테스트는 클래스 수준 트랜잭션이 없어 남긴 행이 커밋된다. 지우지 않으면 다른
     * 테스트의 "아직 이야기한 적 없다"(lastTalkedAt is null) 단언이 순서에 따라 깨진다.
     */
    @AfterEach
    void 남긴_대화를_지운다() {
        executor.shutdownNow();
        llmClient.reset();
        freeTalkRepository.deleteById(freeTalkId);
    }

    @Test
    void 겹친_발화는_하나만_통과하고_나머지는_재시도_가능한_실패로_끝난다() throws Exception {
        List<Future<Throwable>> attempts = new ArrayList<>();
        for (int i = 0; i < CONCURRENT_TURNS; i++) {
            attempts.add(executor.submit(speakAndCatch("같이 말했어")));
        }

        List<Throwable> failures = new ArrayList<>();
        for (Future<Throwable> attempt : attempts) {
            Throwable failure = attempt.get(30, TimeUnit.SECONDS);
            if (failure != null) {
                failures.add(failure);
            }
        }

        assertThat(failures)
                .as("겹친 발화 중 정확히 하나만 통과해야 한다. 둘이 통과하면 10턴 상한이 샌다.")
                .hasSize(CONCURRENT_TURNS - 1);
        assertThat(failures)
                .as("""
                        진 쪽은 재시도해도 되는지 알 수 있어야 한다 - 409다.
                        무결성 충돌이 그대로 올라오면 500이 되고 클라이언트는 아무것도 판단할 수 없다.""")
                .allMatch(this::mapsToConflict);

        // 첫 인사 + 이긴 턴의 두 줄. 진 요청은 아무것도 남기지 않았다.
        assertThat(messageRepository.findAllByFreeTalkIdOrderByTurnOrderAsc(freeTalkId)).hasSize(3);
        assertThat(freeTalkRepository.findById(freeTalkId).orElseThrow().getTurnCount())
                .isEqualTo((short) 1);
    }

    private boolean mapsToConflict(Throwable failure) {
        return failure instanceof BusinessException business
                && business.getErrorCode() == ErrorCode.CONCURRENT_TURN;
    }

    /** 성공이면 null, 실패면 그 예외를 돌려준다. */
    private Callable<Throwable> speakAndCatch(String text) {
        return () -> {
            try {
                freeTalkService.speak(PARENT_ID, freeTalkId, new FreeTalkMessageRequest(text));
                return null;
            } catch (DataIntegrityViolationException e) {
                // 변환되지 않고 그대로 올라온 경우. 이것이 잡으려는 실패다.
                return e;
            } catch (Exception e) {
                return e;
            }
        };
    }
}
