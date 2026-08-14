package com.mugunghwa.goodquestion.story.session;

import com.mugunghwa.goodquestion.learning.reward.stardust.StardustWalletRepository;
import com.mugunghwa.goodquestion.story.session.dto.SceneAdvanceResponse;
import com.mugunghwa.goodquestion.story.session.dto.SessionStartRequest;
import com.mugunghwa.goodquestion.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 후속 활동 config가 없는 이야기의 완주 (2026-08 확정: 건너뛰고 즉시 완료).
 *
 * <p>건너뛰지 않으면 세션이 POST_ACTIVITY로 전이된 뒤 카드 시작이 404를 던져
 * 영영 COMPLETED가 못 되는 갇힘이 생긴다 - 그 회귀 테스트다. 완주 별가루도
 * retelling 없이 지급되어야 한다.
 */
@IntegrationTest
@Transactional
class PostActivitySkipTest {

    /** R__2_seed_demo_data.sql의 데모 계정. 보호자 "김보호" / 아이 "지우". */
    private static final UUID PARENT_ID = UUID.fromString("99999999-9999-9999-9999-000000000001");
    private static final UUID CHILD_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-000000000001");

    @Autowired
    private SessionService sessionService;

    @Autowired
    private StorySessionRepository sessionRepository;

    @Autowired
    private StardustWalletRepository walletRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID storyId;

    @BeforeEach
    void 후속_활동_구성이_없는_이야기를_등록한다() {
        storyId = UUID.randomUUID();
        jdbcTemplate.update("""
                insert into stories (id, title, summary, difficulty, status)
                values (?, '구성 없는 이야기', '후속 활동 config가 null인 이야기', '보통', 'PUBLISHED')
                """, storyId);
        jdbcTemplate.update("""
                insert into story_scenes (id, story_id, scene_order, scene_type, scene_description)
                values (?, ?, 1, 'STORY', '테스트 내레이션.')
                """, UUID.randomUUID(), storyId);
    }

    @Test
    void 마지막_장면이_끝나면_후속_활동을_건너뛰고_즉시_완료된다() {
        int balanceBefore = balance();
        UUID sessionId = sessionService.start(PARENT_ID, CHILD_ID,
                new SessionStartRequest(storyId)).sessionId();

        SceneAdvanceResponse response = sessionService.completeStoryScene(PARENT_ID, sessionId);

        // POST_ACTIVITY가 아니라 곧바로 끝났다.
        assertThat(response.phase()).isEqualTo(PlayPhase.ENDED);
        assertThat(response.currentScene()).isNull();
        assertThat(sessionRepository.findById(sessionId).orElseThrow().getStatus())
                .isEqualTo(SessionStatus.COMPLETED);
        // 완주 별가루(1회차 3개)가 retelling 없이 지급됐다.
        assertThat(balance()).isEqualTo(balanceBefore + 3);
    }

    private int balance() {
        return walletRepository.findByChildId(CHILD_ID).orElseThrow().getBalance();
    }
}
