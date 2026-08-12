package com.mugunghwa.goodquestion.learning.postactivity;

import com.mugunghwa.goodquestion.global.error.BusinessException;
import com.mugunghwa.goodquestion.global.error.ErrorCode;
import com.mugunghwa.goodquestion.learning.postactivity.dto.CardSubmitRequest;
import com.mugunghwa.goodquestion.learning.postactivity.dto.CardSubmitResponse;
import com.mugunghwa.goodquestion.learning.postactivity.dto.PostActivityStartResponse;
import com.mugunghwa.goodquestion.learning.postactivity.dto.PostActivityStatusResponse;
import com.mugunghwa.goodquestion.learning.postactivity.dto.RetellingRequest;
import com.mugunghwa.goodquestion.learning.postactivity.dto.RetellingResponse;
import com.mugunghwa.goodquestion.story.session.SessionStatus;
import com.mugunghwa.goodquestion.story.session.StorySession;
import com.mugunghwa.goodquestion.story.session.StorySessionRepository;
import com.mugunghwa.goodquestion.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static com.mugunghwa.goodquestion.learning.reward.RewardFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@IntegrationTest
@Transactional
class PostActivityServiceTest {

    /** 하준의 세션 - 완주 기록이 없어 첫 완주 지급과 해금을 함께 확인할 수 있다. */
    private static final UUID SIBLING_SESSION_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-000000000003");

    /** 시드 콘텐츠의 정답 순서. 응답에는 담기지 않으므로 테스트가 직접 안다. */
    private static final List<String> CORRECT_ORDER =
            List.of("card_1", "card_2", "card_3", "card_4", "card_5");

    @Autowired
    private PostActivityService activityService;

    @Autowired
    private StorySessionRepository sessionRepository;

    @Test
    void 시작하면_카드를_섞어_돌려준다() {
        UUID sessionId = postActivitySession(IN_PROGRESS_SESSION_ID);

        PostActivityStartResponse response = activityService.start(PARENT_ID, sessionId);

        assertThat(response.cards()).hasSize(5);
        assertThat(response.attemptCount()).isZero();
        assertThat(response.cards()).allSatisfy(card -> {
            assertThat(card.cardId()).isNotBlank();
            assertThat(card.text()).isNotBlank();
        });
    }

    @Test
    void 다시_시작해도_같은_순서를_돌려준다() {
        UUID sessionId = postActivitySession(IN_PROGRESS_SESSION_ID);

        List<String> first = cardIds(activityService.start(PARENT_ID, sessionId));
        List<String> second = cardIds(activityService.start(PARENT_ID, sessionId));

        assertThat(second).isEqualTo(first);
    }

    @Test
    void 상태_조회도_시작과_같은_순서를_돌려준다() {
        UUID sessionId = postActivitySession(IN_PROGRESS_SESSION_ID);
        List<String> started = cardIds(activityService.start(PARENT_ID, sessionId));

        PostActivityStatusResponse status = activityService.getStatus(PARENT_ID, sessionId);

        assertThat(status.cards().stream().map(PostActivityStartResponse.Card::cardId).toList())
                .isEqualTo(started);
        assertThat(status.status()).isEqualTo("ORDER_PENDING");
    }

    @Test
    void 시작_전_상태는_비어_있다() {
        UUID sessionId = postActivitySession(IN_PROGRESS_SESSION_ID);

        PostActivityStatusResponse status = activityService.getStatus(PARENT_ID, sessionId);

        assertThat(status.status()).isEqualTo("NOT_STARTED");
        assertThat(status.cards()).isEmpty();
        assertThat(status.isOrderCorrect()).isNull();
    }

    @Test
    void 오답이면_열쇠말을_주지_않고_시도만_센다() {
        UUID sessionId = postActivitySession(IN_PROGRESS_SESSION_ID);
        activityService.start(PARENT_ID, sessionId);

        CardSubmitResponse response = activityService.submitOrder(PARENT_ID, sessionId,
                new CardSubmitRequest(List.of("card_2", "card_1", "card_3", "card_4", "card_5")));

        assertThat(response.correct()).isFalse();
        assertThat(response.retellingKeywords()).isNull();
        assertThat(activityService.getStatus(PARENT_ID, sessionId).attemptCount()).isEqualTo((short) 1);
    }

    @Test
    void 정답이면_열쇠말을_준다() {
        UUID sessionId = postActivitySession(IN_PROGRESS_SESSION_ID);
        activityService.start(PARENT_ID, sessionId);

        CardSubmitResponse response = activityService.submitOrder(
                PARENT_ID, sessionId, new CardSubmitRequest(CORRECT_ORDER));

        assertThat(response.correct()).isTrue();
        assertThat(response.retellingKeywords()).containsExactly("방귀", "며느리", "배나무", "시아버지");
        assertThat(activityService.getStatus(PARENT_ID, sessionId).status()).isEqualTo("ORDER_CORRECT");
    }

    @Test
    void 대화_중인_세션에서는_시작할_수_없다() {
        assertThatThrownBy(() -> activityService.start(PARENT_ID, IN_PROGRESS_SESSION_ID))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SESSION_NOT_IN_PROGRESS);
    }

    @Test
    void 남의_세션은_다룰_수_없다() {
        UUID sessionId = postActivitySession(IN_PROGRESS_SESSION_ID);

        assertThatThrownBy(() -> activityService.start(OTHER_PARENT_ID, sessionId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
    }

    /** 대화 턴 파이프라인이 아직 없어 세션을 후속 활동 단계로 직접 옮긴다. */
    private UUID postActivitySession(UUID sessionId) {
        StorySession session = sessionRepository.findById(sessionId).orElseThrow();
        session.toPostActivity();
        return session.getId();
    }

    private void 맞히기까지_진행한다(UUID sessionId) {
        activityService.start(PARENT_ID, sessionId);
        activityService.submitOrder(PARENT_ID, sessionId, new CardSubmitRequest(CORRECT_ORDER));
    }

    private List<String> cardIds(PostActivityStartResponse response) {
        return response.cards().stream().map(PostActivityStartResponse.Card::cardId).toList();
    }
}
