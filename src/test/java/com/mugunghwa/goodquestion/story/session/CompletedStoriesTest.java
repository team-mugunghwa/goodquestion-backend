package com.mugunghwa.goodquestion.story.session;

import com.mugunghwa.goodquestion.global.error.BusinessException;
import com.mugunghwa.goodquestion.story.session.dto.CompletedStoriesResponse;
import com.mugunghwa.goodquestion.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 완주한 이야기 목록. 이야기 목록 화면이 카드에 "끝냈어" 도장을 찍는 근거다.
 *
 * <p>완주 판정은 <b>COMPLETED 세션</b>이고, 이것은 자유 대화의 진입 조건
 * ({@code FreeTalkTransactions.requireCompleted})과 같은 근거다. 근거가 갈리면 도장은
 * 찍혔는데 친구는 못 만나는(또는 그 반대의) 화면이 나오고, 그 어긋남은 화면에서만
 * 보여서 찾기 어렵다. 둘 다 {@code StorySessionRepository}의 COMPLETED 조회를 쓴다.
 *
 * <p><b>세션 패키지에 있는 이유</b> - 완주는 이야기의 성질이 아니라 (아이, 이야기)의
 * 런타임 상태다. 처음에 {@code story.content}에 뒀다가
 * {@code ArchitectureTest.content_must_not_depend_on_runtime}(데이터-02)에 걸렸다.
 *
 * <p>자유 대화 쪽을 여기서 함께 부르지 않는 이유 - 그쪽 테스트는 LLM·TTS 대역
 * ({@code StubFreeTalkConfig})을 {@code @Import}로 얹는데, 같은 설정을 안 쓰는 테스트와는
 * 스프링 컨텍스트가 갈린다. 컨텍스트가 하나 더 뜨면 커넥션 풀도 하나 더 붙어 무관한
 * 테스트가 "too many clients"로 죽는다(그쪽 설정 클래스의 주석에 그 사고가 적혀 있다).
 */
@IntegrationTest
class CompletedStoriesTest {

    /** R__2_seed_demo_data.sql의 데모 계정. 보호자 "김보호" / 아이 "지우". */
    private static final UUID PARENT_ID = UUID.fromString("99999999-9999-9999-9999-000000000001");
    private static final UUID CHILD_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-000000000001");

    /** 같은 보호자의 둘째 "하준" - 세션이 하나도 없다. */
    private static final UUID CHILD_WITHOUT_SESSIONS =
            UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-000000000002");

    /** 다른 보호자의 아이 "서연". */
    private static final UUID OTHER_PARENTS_CHILD =
            UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-000000000003");

    /** 지우가 완주한 이야기 - 시드에 COMPLETED 세션이 있다. */
    private static final UUID COMPLETED_STORY_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Autowired
    private SessionService sessionService;

    @Test
    void 완주한_이야기만_한_번씩_담긴다() {
        CompletedStoriesResponse response = sessionService.getCompletedStories(PARENT_ID, CHILD_ID);

        // 시드에는 같은 이야기의 진행 중 세션도 둘 있다. 진행 중은 완주가 아니고,
        // 세션이 여러 개여도 이야기는 한 번만 담겨야 한다.
        assertThat(response.storyIds()).containsExactly(COMPLETED_STORY_ID);
    }

    @Test
    void 아무것도_안_한_아이는_빈_목록이다() {
        CompletedStoriesResponse response =
                sessionService.getCompletedStories(PARENT_ID, CHILD_WITHOUT_SESSIONS);

        // 404가 아니다. 아직 아무것도 안 한 것은 정상이고, 목록 화면은 도장만 안 찍는다.
        assertThat(response.storyIds()).isEmpty();
    }

    @Test
    void 남의_아이는_볼_수_없다() {
        assertThatThrownBy(() -> sessionService.getCompletedStories(PARENT_ID, OTHER_PARENTS_CHILD))
                .isInstanceOf(BusinessException.class);
    }
}
