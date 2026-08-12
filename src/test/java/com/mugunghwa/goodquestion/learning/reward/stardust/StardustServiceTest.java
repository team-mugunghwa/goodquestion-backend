package com.mugunghwa.goodquestion.learning.reward.stardust;

import com.mugunghwa.goodquestion.global.error.BusinessException;
import com.mugunghwa.goodquestion.global.error.ErrorCode;
import com.mugunghwa.goodquestion.learning.reward.StoryPlayCounter;
import com.mugunghwa.goodquestion.learning.reward.stardust.dto.StardustWalletResponse;
import com.mugunghwa.goodquestion.story.content.StoryScene;
import com.mugunghwa.goodquestion.story.content.StorySceneRepository;
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
class StardustServiceTest {

    /** 하준의 세션 - 아직 완주 기록이 없어 1회차 지급을 확인할 수 있다. */
    private static final UUID SIBLING_SESSION_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-000000000003");

    @Autowired
    private StardustService stardustService;

    @Autowired
    private StardustWalletRepository walletRepository;

    @Autowired
    private StardustTransactionRepository transactionRepository;

    @Autowired
    private StorySessionRepository sessionRepository;

    @Autowired
    private StorySceneRepository sceneRepository;

    @Autowired
    private StoryPlayCounter playCounter;

    @Test
    void 지갑은_잔액과_누적과_미확인_지급을_돌려준다() {
        StardustWalletResponse wallet = stardustService.getWallet(PARENT_ID, CHILD_ID);

        assertThat(wallet.balance()).isEqualTo(14);
        assertThat(wallet.totalEarned()).isEqualTo(25);
        assertThat(wallet.unacknowledged()).isNotEmpty();
    }

    @Test
    void 연출을_확인하면_미확인_지급이_비워진다() {
        int pending = stardustService.getWallet(PARENT_ID, CHILD_ID).unacknowledged().size();

        assertThat(stardustService.acknowledge(PARENT_ID, CHILD_ID).acknowledgedCount()).isEqualTo(pending);
        assertThat(stardustService.getWallet(PARENT_ID, CHILD_ID).unacknowledged()).isEmpty();
    }

    @Test
    void 사용_이력은_연출_대상이_아니다() {
        // 시드의 구매 3건은 처음부터 확인 처리돼 있다
        assertThat(stardustService.getWallet(PARENT_ID, CHILD_ID).unacknowledged())
                .allSatisfy(transaction -> assertThat(transaction.amount()).isPositive());
    }

    @Test
    void 첫_완주는_3개를_지급한다() {
        StorySession session = session(SIBLING_SESSION_ID);
        int before = walletRepository.findByChildId(SIBLING_ID).orElseThrow().getBalance();

        List<StardustTransaction> awarded = stardustService.awardStoryCompleted(session);

        assertThat(awarded).singleElement()
                .satisfies(transaction -> assertThat(transaction.getAmount()).isEqualTo(3));
        assertThat(walletRepository.findByChildId(SIBLING_ID).orElseThrow().getBalance())
                .isEqualTo(before + 3);
    }

    @Test
    void 두_번째_완주는_절반만_지급한다() {
        // 지우는 방귀 이야기를 1회 완주했다 - 진행 중인 세션을 완주시키면 2회차다
        StorySession session = session(IN_PROGRESS_SESSION_ID);

        List<StardustTransaction> awarded = stardustService.awardStoryCompleted(session);

        assertThat(awarded).singleElement()
                .satisfies(transaction -> assertThat(transaction.getAmount()).isEqualTo(1));
    }

    @Test
    void 세_번째_완주부터는_지급하지_않는다() {
        playCounter.increaseAndGet(SIBLING_ID, STORY_ID);   // 하준을 2회차 상태로 만든다
        playCounter.increaseAndGet(SIBLING_ID, STORY_ID);
        int before = walletRepository.findByChildId(SIBLING_ID).orElseThrow().getBalance();

        assertThat(stardustService.awardStoryCompleted(session(SIBLING_SESSION_ID))).isEmpty();
        assertThat(walletRepository.findByChildId(SIBLING_ID).orElseThrow().getBalance()).isEqualTo(before);
    }

    @Test
    void 같은_세션에_두_번_지급하지_않는다() {
        StorySession session = session(SIBLING_SESSION_ID);
        stardustService.awardStoryCompleted(session);
        int after = walletRepository.findByChildId(SIBLING_ID).orElseThrow().getBalance();

        assertThat(stardustService.awardStoryCompleted(session)).isEmpty();
        assertThat(walletRepository.findByChildId(SIBLING_ID).orElseThrow().getBalance()).isEqualTo(after);
    }

    @Test
    void 이미_지급된_세션은_완주_횟수를_올리지_않는다() {
        // 시드의 완주 세션에는 STORY_COMPLETED 이력이 이미 있다
        int before = playCounter.get(CHILD_ID, STORY_ID);

        stardustService.awardStoryCompleted(session(COMPLETED_SESSION_ID));

        assertThat(playCounter.get(CHILD_ID, STORY_ID)).isEqualTo(before);
    }

    @Test
    void 장면_보너스는_장면당_한_번_세션당_두_번까지다() {
        StorySession session = session(SIBLING_SESSION_ID);
        List<StoryScene> scenes = sceneRepository.findAllByStoryIdOrderBySceneOrderAsc(STORY_ID);

        assertThat(stardustService.awardSceneBonus(session, scenes.get(0))).isPresent();
        assertThat(stardustService.awardSceneBonus(session, scenes.get(0))).isEmpty();   // 같은 장면
        assertThat(stardustService.awardSceneBonus(session, scenes.get(1))).isPresent();
        assertThat(stardustService.awardSceneBonus(session, scenes.get(2))).isEmpty();   // 상한
    }

    @Test
    void 두_번째_완주부터는_장면_보너스가_없다() {
        // 지우는 이미 1회 완주했다
        StorySession session = session(IN_PROGRESS_SESSION_ID);
        StoryScene scene = sceneRepository.findAllByStoryIdOrderBySceneOrderAsc(STORY_ID).getFirst();

        assertThat(stardustService.awardSceneBonus(session, scene)).isEmpty();
    }

    @Test
    void 완주와_장면_보너스를_합쳐_최대_5개다() {
        StorySession session = session(SIBLING_SESSION_ID);
        List<StoryScene> scenes = sceneRepository.findAllByStoryIdOrderBySceneOrderAsc(STORY_ID);
        stardustService.awardSceneBonus(session, scenes.get(0));
        stardustService.awardSceneBonus(session, scenes.get(1));
        stardustService.awardStoryCompleted(session);

        int earned = transactionRepository.findAllBySessionIdOrderByCreatedAtAsc(session.getId()).stream()
                .mapToInt(StardustTransaction::getAmount).filter(amount -> amount > 0).sum();

        assertThat(earned).isEqualTo(5);
    }

    @Test
    void 남의_아이_지갑은_볼_수_없다() {
        assertThatThrownBy(() -> stardustService.getWallet(OTHER_PARENT_ID, CHILD_ID))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
    }

    private StorySession session(UUID sessionId) {
        return sessionRepository.findById(sessionId).orElseThrow();
    }
}
