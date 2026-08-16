package com.mugunghwa.goodquestion.story.session;

import com.mugunghwa.goodquestion.story.session.dto.SceneAdvanceResponse;
import com.mugunghwa.goodquestion.story.session.dto.SessionStartRequest;
import com.mugunghwa.goodquestion.story.session.dto.SessionStartResponse;
import com.mugunghwa.goodquestion.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 장면 영상(video_url)이 장면 콘텐츠 응답에 실리는지 확인한다.
 *
 * <p>SceneContentResponse 하나를 세션 시작·이어하기·장면 전환·현재 장면 조회가
 * 공유하므로, 시작과 전환에서 실리면 나머지 경로도 같은 매핑을 탄다.
 *
 * <p>영상은 이미지를 대체하지 않고 얹는다 - video_url이 있어도 image_url은
 * 함께 실려야 클라이언트가 재생 실패 시 이미지로 폴백할 수 있다.
 */
@IntegrationTest
@Transactional
class SceneVideoUrlTest {

    /** R__2_seed_demo_data.sql의 데모 계정. 보호자 "김보호" / 아이 "지우". */
    private static final UUID PARENT_ID = UUID.fromString("99999999-9999-9999-9999-000000000001");
    private static final UUID CHILD_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-000000000001");

    private static final String VIDEO_URL = "/stories/banggui/scenes/01_intro_loop.mp4";
    private static final String IMAGE_URL = "/stories/banggui/scenes/01_intro.jpg";

    @Autowired
    private SessionService sessionService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID storyId;

    @BeforeEach
    void 영상이_있는_장면과_없는_장면을_등록한다() {
        storyId = UUID.randomUUID();
        jdbcTemplate.update("""
                insert into stories (id, title, summary, difficulty, status)
                values (?, '영상 있는 이야기', '장면 1에만 영상이 있다', '보통', 'PUBLISHED')
                """, storyId);
        jdbcTemplate.update("""
                insert into story_scenes (id, story_id, scene_order, scene_type, scene_description, image_url, video_url)
                values (?, ?, 1, 'STORY', '영상이 있는 장면.', ?, ?)
                """, UUID.randomUUID(), storyId, IMAGE_URL, VIDEO_URL);
        jdbcTemplate.update("""
                insert into story_scenes (id, story_id, scene_order, scene_type, scene_description, image_url)
                values (?, ?, 2, 'STORY', '영상이 없는 장면.', ?)
                """, UUID.randomUUID(), storyId, IMAGE_URL);
    }

    @Test
    void 세션을_시작하면_장면_영상_URL이_이미지와_함께_실린다() {
        SessionStartResponse response = sessionService.start(PARENT_ID, CHILD_ID,
                new SessionStartRequest(storyId));

        assertThat(response.currentScene().videoUrl()).isEqualTo(VIDEO_URL);
        // 영상이 있어도 이미지는 그대로 - 재생 실패 시 폴백 경로다.
        assertThat(response.currentScene().imageUrl()).isEqualTo(IMAGE_URL);
    }

    @Test
    void 영상이_없는_장면은_videoUrl이_null이고_이미지만_실린다() {
        UUID sessionId = sessionService.start(PARENT_ID, CHILD_ID,
                new SessionStartRequest(storyId)).sessionId();

        SceneAdvanceResponse response = sessionService.completeStoryScene(PARENT_ID, sessionId);

        assertThat(response.currentScene().sceneOrder()).isEqualTo((short) 2);
        assertThat(response.currentScene().videoUrl()).isNull();
        assertThat(response.currentScene().imageUrl()).isEqualTo(IMAGE_URL);
    }
}
