package com.mugunghwa.goodquestion.story.mission;

import com.mugunghwa.goodquestion.story.mission.dto.CurrentMissionResponse;
import com.mugunghwa.goodquestion.story.mission.dto.MissionResponse;
import com.mugunghwa.goodquestion.story.session.SessionService;
import com.mugunghwa.goodquestion.story.session.StorySessionRepository;
import com.mugunghwa.goodquestion.story.session.dto.SessionStartRequest;
import com.mugunghwa.goodquestion.support.IntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GET /missions/current가 트랜잭션 밖에서도 동작하는지 확인한다(PR #52 회귀 테스트).
 *
 * <p>컨트롤러가 소유권 검증과 미션 조회를 각각 다른 서비스로 부르면 두 호출이 서로 다른
 * 트랜잭션이 된다. 먼저 꺼낸 세션은 준영속 상태가 되고, 그 세션의 currentScene(LAZY)을
 * 두 번째 호출에서 건드리는 순간 LazyInitializationException으로 500이 났다
 * (open-in-view: false). 지금은 두 일이 한 트랜잭션 안에서 끝난다.
 *
 * <p><b>일부러 {@code @Transactional}을 붙이지 않는다.</b> 테스트 트랜잭션이 있으면 모든
 * 호출이 한 영속성 컨텍스트에 묶여 LAZY 로딩이 조용히 성공하고, 실서버에서만 터지는 이
 * 계열의 결함을 영영 못 잡는다. 세션 정리는 그래서 직접 한다.
 */
@IntegrationTest
class MissionCurrentEndpointTest {

    private static final UUID PARENT_ID = UUID.fromString("99999999-9999-9999-9999-000000000001");
    private static final UUID CHILD_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-000000000001");
    private static final UUID STORY_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    /** R__1_seed_content.sql의 대화3(장면 7) - 미션1이 붙어 있는 장면. */
    private static final UUID MISSION_SCENE_ID = UUID.fromString("33333333-3333-3333-3333-000000000007");

    @Autowired
    private MissionController missionController;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private StorySessionRepository sessionRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID sessionId;

    @AfterEach
    void 세션을_지운다() {
        if (sessionId != null) {
            sessionRepository.deleteById(sessionId);
        }
    }

    @Test
    void 미션이_없는_장면에서는_null을_돌려준다() {
        sessionId = 세션을_시작한다();

        CurrentMissionResponse response = missionController.getCurrentMission(PARENT_ID, sessionId);

        // 장면 1(도입)은 미션이 없으니 null이 정상이다. 예외 없이 응답이 오는지가 핵심 -
        // 준영속 세션이면 hasMission()을 부르는 자리에서 터졌다.
        assertThat(response.mission()).isNull();
    }

    @Test
    void 노출된_미션은_설정_내용까지_담아_돌려준다() {
        sessionId = 세션을_시작한다();
        미션_장면에_노출된_상태로_바꾼다(sessionId);

        CurrentMissionResponse response = missionController.getCurrentMission(PARENT_ID, sessionId);

        // 미션 설정(jsonb)까지 읽어 payload를 만드는 경로도 트랜잭션 밖에서 안전해야 한다.
        assertThat(response.mission()).isNotNull();
        assertThat(response.mission().missionId()).isEqualTo("mission_1");
        assertThat(response.mission().payload().questions())
                .extracting(MissionResponse.Question::key)
                .containsExactly("tool", "reason", "request", "expectedResult");
    }

    private UUID 세션을_시작한다() {
        return sessionService.start(PARENT_ID, CHILD_ID, new SessionStartRequest(STORY_ID))
                .sessionId();
    }

    /** 턴을 실제로 돌리지 않고 미션 노출 상태만 만든다 - 이 테스트가 볼 것은 조회 경로다. */
    private void 미션_장면에_노출된_상태로_바꾼다(UUID sessionId) {
        jdbcTemplate.update("""
                update story_sessions set current_scene_id = ?, mission_exposed = true
                where id = ?
                """, MISSION_SCENE_ID, sessionId);
    }
}
