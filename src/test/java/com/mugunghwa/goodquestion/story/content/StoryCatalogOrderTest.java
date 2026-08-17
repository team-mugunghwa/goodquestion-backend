package com.mugunghwa.goodquestion.story.content;

import com.mugunghwa.goodquestion.story.content.dto.StoryCardResponse;
import com.mugunghwa.goodquestion.story.content.dto.StoryListResponse;
import com.mugunghwa.goodquestion.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 시드가 채운 이야기 목록의 노출 순서 검증.
 *
 * <p>진행 가능한 이야기가 방귀 뀌는 며느리뿐이라 목록과 홈 모두 이 이야기가 첫 칸이어야
 * 한다. 정렬 키가 created_at 하나였을 때는 이걸 보장할 수 없었다 - 시드가 created_at을
 * 적지 않아 DB 기본값 now()가 들어가는데, 포스트그레스의 now()는 트랜잭션 시작 시각이라
 * 한 마이그레이션에서 들어간 이야기끼리 값이 같고 그 사이 순서는 실행 계획이 정했다.
 * display_order를 앞세운 뒤로 순서가 콘텐츠의 결정이 됐으므로 여기서 고정해 둔다.
 */
@IntegrationTest
class StoryCatalogOrderTest {

    private static final UUID BANGGUI = UUID.fromString("11111111-1111-1111-1111-111111111111");
    /** R__2가 넣었다가 내린 흐름 확인용 데모 이야기. */
    private static final UUID SMALL_SEED = UUID.fromString("11111111-1111-1111-1111-222222222222");

    @Autowired
    private StoryService storyService;

    @Autowired
    private StoryRepository storyRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void 목록의_첫_번째는_방귀_뀌는_며느리다() {
        StoryListResponse response = storyService.getStories(null);

        assertThat(response.stories()).isNotEmpty();
        assertThat(response.stories().getFirst().id()).isEqualTo(BANGGUI);
        assertThat(response.stories().getFirst().title()).isEqualTo("방귀 뀌는 며느리");
    }

    @Test
    void 홈_추천_상위_3개의_첫_번째도_방귀_뀌는_며느리다() {
        List<Story> top3 = storyRepository
                .findTop3ByStatusOrderByDisplayOrderAscCreatedAtDesc(StoryStatus.PUBLISHED);

        assertThat(top3).hasSize(3);
        assertThat(top3.getFirst().getId()).isEqualTo(BANGGUI);
    }

    /**
     * display_order가 같으면 created_at으로 갈리는데 시드의 created_at은 서로 같다.
     * 값이 겹치는 순간 순서가 실행 계획에 좌우되므로 시드 단계에서 전부 달라야 한다.
     */
    @Test
    void 공개된_이야기의_노출_순서_값은_서로_겹치지_않는다() {
        List<Integer> orders = jdbcTemplate.queryForList(
                "select display_order from stories where status = 'PUBLISHED'", Integer.class);

        assertThat(orders).doesNotHaveDuplicates();
    }

    @Test
    void 목록은_노출_순서_오름차순으로_나온다() {
        List<UUID> listed = storyService.getStories(null).stories().stream()
                .map(StoryCardResponse::id)
                .toList();

        List<UUID> expected = storyRepository
                .findAllByStatusOrderByDisplayOrderAscCreatedAtDesc(StoryStatus.PUBLISHED).stream()
                .map(Story::getId)
                .toList();

        assertThat(listed).containsExactlyElementsOf(expected);
    }

    @Test
    void 작은_씨앗은_목록에_없다() {
        List<UUID> listed = storyService.getStories(null).stories().stream()
                .map(StoryCardResponse::id)
                .toList();

        assertThat(listed).doesNotContain(SMALL_SEED);
    }

    /**
     * 세션 기록이 남은 DB에서는 하드 삭제 대신 ARCHIVED로 내린다(V19). 어느 쪽이든
     * 사용자에게 나가는 조회는 PUBLISHED만 보므로, 남아 있더라도 PUBLISHED면 안 된다.
     */
    @Test
    void 작은_씨앗은_지워졌거나_최소한_공개_상태가_아니다() {
        List<String> statuses = jdbcTemplate.queryForList(
                "select status from stories where id = ?", String.class, SMALL_SEED);

        assertThat(statuses).doesNotContain("PUBLISHED");
    }
}
