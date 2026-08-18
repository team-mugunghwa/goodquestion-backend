package com.mugunghwa.goodquestion.story.session;

import com.mugunghwa.goodquestion.story.session.dto.SessionStartRequest;
import com.mugunghwa.goodquestion.support.IntegrationTest;
import com.mugunghwa.goodquestion.support.TestSessions;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 세션 조회가 몇 번의 쿼리로 끝나는지 고정한다.
 *
 * <p>{@code getOwnedSession}은 대화, 후속 활동, 리포트가 모두 쓰는 공용 진입점이고
 * 턴 하나에만 세 번 불린다(트랜잭션을 셋으로 쪼갰기 때문에 1차 캐시도 듣지 않는다).
 * 그래서 여기서 LAZY 연관 하나가 늘면 그 비용이 서비스 전체에 곱해진다.
 *
 * <p>소유권 검증이 {@code session.getChild()}를 보고, 대부분의 호출자가 곧바로
 * {@code getCurrentScene()}을 보므로 이 둘은 사실상 항상 필요하다. 페치 조인으로
 * 함께 읽어 쿼리 1회로 끝낸다 - 이 테스트가 그 상태를 지킨다.
 *
 * <p>깨졌다면 페치 조인이 빠졌거나 새 LAZY 연관을 건드리기 시작한 것이다.
 * 숫자를 올리기 전에 그 연관이 정말 매 호출 필요한지 먼저 확인할 것.
 */
@IntegrationTest
class SessionFetchPlanTest {

    /** R__2_seed_demo_data.sql의 데모 계정. 보호자 "김보호" / 아이 "지우". */
    private static final UUID PARENT_ID = UUID.fromString("99999999-9999-9999-9999-000000000001");
    private static final UUID CHILD_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-000000000001");
    private static final UUID STORY_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Autowired
    private SessionService sessionService;

    @Autowired
    private StorySessionRepository sessionRepository;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private UUID sessionId;

    @BeforeEach
    void 대화_장면까지_진행한다() {
        TestSessions.stopAllInProgress(sessionRepository);
        sessionId = sessionService.start(PARENT_ID, CHILD_ID, new SessionStartRequest(STORY_ID))
                .sessionId();
        // 장면 3이 첫 대화 장면이다. current_scene_id가 채워진 상태로 재게 된다.
        sessionService.completeStoryScene(PARENT_ID, sessionId);
        sessionService.completeStoryScene(PARENT_ID, sessionId);
    }

    /**
     * 이 테스트는 @Transactional이 아니라 쓴 것이 롤백되지 않는다 - 쿼리 수를 세려면
     * 트랜잭션 경계가 실제와 같아야 해서 일부러 뺐다. 대신 남긴 세션을 직접 치운다.
     * 안 치우면 다음 테스트의 start()가 이 세션을 이어받아 "장면 1부터"를 전제한
     * 셋업이 깨진다(TestSessions 주석 참고).
     */
    @AfterEach
    void 남긴_세션을_치운다() {
        TestSessions.stopAllInProgress(sessionRepository);
    }

    @Test
    void 세션_조회와_소유권_장면_확인이_쿼리_한_번으로_끝난다() {
        long queries = countQueries(() -> {
            StorySession session = sessionService.getOwnedSession(PARENT_ID, sessionId);
            // 소유권 검증이 이미 child를 봤고, 호출자는 곧바로 장면을 본다.
            assertThat(session.getCurrentScene().getSceneOrder()).isEqualTo((short) 3);
        });

        assertThat(queries).isEqualTo(1);
    }

    /**
     * 현재 장면이 없는 세션도 조회된다.
     *
     * <p>페치 조인을 inner join으로 쓰면 current_scene_id가 null인 세션이 결과에서
     * 통째로 사라져 "세션을 찾을 수 없습니다"가 된다. left join이어야 한다.
     */
    @Test
    void 현재_장면이_없는_세션도_조회된다() {
        // current_scene_id는 nullable이다. 지금은 항상 채워지지만 스키마가 허용하는 이상
        // 페치 조인이 이 경우를 견뎌야 한다.
        jdbcTemplate.update("update story_sessions set current_scene_id = null where id = ?",
                sessionId);

        StorySession session = transactionTemplate.execute(status ->
                sessionService.getOwnedSession(PARENT_ID, sessionId));

        assertThat(session).isNotNull();
        assertThat(session.getCurrentScene()).isNull();
    }

    /** 새 영속성 컨텍스트에서 실행해 1차 캐시가 결과를 가리지 않게 한다. */
    private long countQueries(Runnable work) {
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();

        transactionTemplate.executeWithoutResult(status -> work.run());

        return statistics.getPrepareStatementCount();
    }
}
