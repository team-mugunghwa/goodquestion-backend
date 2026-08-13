package com.mugunghwa.goodquestion.global.idempotency;

import com.mugunghwa.goodquestion.global.error.BusinessException;
import com.mugunghwa.goodquestion.global.error.ErrorCode;
import com.mugunghwa.goodquestion.global.vocab.ThinkingElement;
import com.mugunghwa.goodquestion.global.vocab.UtteranceValidity;
import com.mugunghwa.goodquestion.learning.reward.shop.ChildItemRepository;
import com.mugunghwa.goodquestion.learning.reward.shop.ShopService;
import com.mugunghwa.goodquestion.learning.reward.shop.dto.ItemPurchaseRequest;
import com.mugunghwa.goodquestion.learning.reward.shop.dto.ItemPurchaseResponse;
import com.mugunghwa.goodquestion.learning.reward.stardust.StardustWalletRepository;
import com.mugunghwa.goodquestion.story.dialogue.TurnOrchestrator;
import com.mugunghwa.goodquestion.story.dialogue.TurnOrchestratorTest;
import com.mugunghwa.goodquestion.story.dialogue.TurnOrchestratorTest.ScriptedAnalysisLlmClient;
import com.mugunghwa.goodquestion.story.dialogue.dto.UtteranceRequest;
import com.mugunghwa.goodquestion.story.dialogue.dto.UtteranceResponse;
import com.mugunghwa.goodquestion.story.session.SessionService;
import com.mugunghwa.goodquestion.story.session.dto.SessionStartRequest;
import com.mugunghwa.goodquestion.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 멱등키 규약의 동작을 실제 파이프라인으로 확인한다.
 *
 * <p>핵심은 재생(replay)이다 - 같은 키의 재전송이 작업을 다시 실행하지 않고 저장된
 * 응답을 돌려주는지. 발화 대역(ScriptedAnalysisLlmClient)은 준비된 대본이 없으면
 * 예외를 던지므로, 재전송에서 예외가 안 났다는 것 자체가 작업 미실행의 증거가 된다.
 */
@IntegrationTest
@Transactional
@Import(TurnOrchestratorTest.StubLlmConfig.class)
class IdempotencyIntegrationTest {

    /** R__2_seed_demo_data.sql의 데모 계정(보호자 "김보호" / 아이 "지우", 잔액 14). */
    private static final UUID PARENT_ID = UUID.fromString("99999999-9999-9999-9999-000000000001");
    private static final UUID CHILD_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-000000000001");
    private static final UUID STORY_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    /** R__1의 아이템 "돌" - 3별가루, 항상 해금. */
    private static final UUID ROCK_ITEM_ID = UUID.fromString("44444444-4444-4444-4444-000000000001");

    @Autowired
    private IdempotencyService idempotencyService;

    @Autowired
    private IdempotentRequestRepository idempotentRequestRepository;

    @Autowired
    private TurnOrchestrator orchestrator;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private ScriptedAnalysisLlmClient analysisLlmClient;

    @Autowired
    private ShopService shopService;

    @Autowired
    private StardustWalletRepository walletRepository;

    @Autowired
    private ChildItemRepository childItemRepository;

    private UUID sessionId;

    @BeforeEach
    void 대화_장면까지_진행한다() {
        analysisLlmClient.reset();
        sessionId = sessionService.start(PARENT_ID, CHILD_ID, new SessionStartRequest(STORY_ID))
                .sessionId();
        sessionService.completeStoryScene(PARENT_ID, sessionId);
        sessionService.completeStoryScene(PARENT_ID, sessionId);
    }

    private UtteranceResponse submit(String key, String text) {
        return idempotencyService.execute(IdempotentEndpoint.UTTERANCE, sessionId, PARENT_ID, key,
                UtteranceResponse.class,
                () -> orchestrator.processUtterance(PARENT_ID, sessionId,
                        new UtteranceRequest(text, null, null, null, null)));
    }

    private ItemPurchaseResponse purchase(String key) {
        return idempotencyService.execute(IdempotentEndpoint.ITEM_PURCHASE, CHILD_ID, PARENT_ID,
                key, ItemPurchaseResponse.class,
                () -> shopService.purchase(PARENT_ID, CHILD_ID,
                        new ItemPurchaseRequest(ROCK_ITEM_ID)));
    }

    @Test
    void 같은_키의_재전송은_저장된_응답을_재생하고_턴을_중복_처리하지_않는다() {
        analysisLlmClient.willDetect(UtteranceValidity.VALID, ThinkingElement.EMOTION);
        UtteranceResponse first = submit("key-1", "며느리가 많이 힘들 것 같아요");

        // 대본을 준비하지 않았다 - 작업이 다시 실행되면 대역이 예외를 던진다.
        UtteranceResponse replayed = submit("key-1", "며느리가 많이 힘들 것 같아요");

        assertThat(replayed.progress().turnCount()).isEqualTo(first.progress().turnCount());
        assertThat(replayed.childMessage().text()).isEqualTo(first.childMessage().text());
        assertThat(replayed.characterMessage().text()).isEqualTo(first.characterMessage().text());
        // 세션의 턴 수는 여전히 1 - 재전송이 새 턴이 되지 않았다.
        assertThat(sessionService.getTurnState(PARENT_ID, sessionId).progress().turnCount())
                .isEqualTo(1);
    }

    @Test
    void 새_키는_새_턴으로_처리된다() {
        analysisLlmClient.willDetect(UtteranceValidity.VALID, ThinkingElement.EMOTION);
        submit("key-1", "며느리가 많이 힘들 것 같아요");

        analysisLlmClient.willDetect(UtteranceValidity.VALID, ThinkingElement.PERSPECTIVE);
        UtteranceResponse second = submit("key-2", "가족들도 이해해 줄 거예요");

        assertThat(second.progress().turnCount()).isEqualTo(2);
    }

    @Test
    void 처리_중인_키의_재전송은_409를_받는다() {
        idempotentRequestRepository.saveAndFlush(IdempotentRequest.builder()
                .endpoint(IdempotentEndpoint.UTTERANCE).scopeId(sessionId).parentId(PARENT_ID)
                .idempotencyKey("in-flight").build());

        assertThatThrownBy(() -> submit("in-flight", "몰라요"))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.REQUEST_IN_PROGRESS));
    }

    @Test
    void 실패한_작업의_키는_비워져_재시도가_다시_실행된다() {
        // 대본이 없어 분석 대역이 예외를 던진다 - 작업 실패.
        assertThatThrownBy(() -> submit("key-fail", "며느리가 힘들 것 같아요"))
                .isInstanceOf(IllegalStateException.class);

        // 키가 지워졌으므로 같은 키의 정직한 재시도는 다시 실행된다.
        analysisLlmClient.willDetect(UtteranceValidity.VALID, ThinkingElement.EMOTION);
        UtteranceResponse retried = submit("key-fail", "며느리가 힘들 것 같아요");

        assertThat(retried.progress().turnCount()).isEqualTo(1);
    }

    @Test
    void 같은_키의_재구매는_별가루를_한_번만_차감한다() {
        int balanceBefore = balance();
        long rocksBefore = ownedRocks();

        ItemPurchaseResponse first = purchase("buy-1");
        ItemPurchaseResponse replayed = purchase("buy-1");

        assertThat(first.balance()).isEqualTo(balanceBefore - 3);
        assertThat(replayed.balance()).isEqualTo(first.balance());
        assertThat(balance()).isEqualTo(balanceBefore - 3);
        // 재생이 입고를 반복하지 않는다 - 같은 아이템 여러 개 보유가 정상인 도메인이라
        // 유니크로는 못 막고, 멱등키만이 이 중복을 구분할 수 있다.
        assertThat(ownedRocks()).isEqualTo(rocksBefore + 1);
    }

    @Test
    void 키가_64자를_넘으면_400이다() {
        assertThatThrownBy(() -> submit("k".repeat(65), "몰라요"))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_IDEMPOTENCY_KEY));
    }

    @Test
    void 키가_없으면_기록_없이_그대로_실행된다() {
        analysisLlmClient.willDetect(UtteranceValidity.VALID, ThinkingElement.EMOTION);
        submit(null, "며느리가 많이 힘들 것 같아요");

        assertThat(idempotentRequestRepository.count()).isZero();
    }

    private int balance() {
        return walletRepository.findByChildId(CHILD_ID).orElseThrow().getBalance();
    }

    private long ownedRocks() {
        return childItemRepository.findAll().stream()
                .filter(item -> item.getChild().getId().equals(CHILD_ID))
                .filter(item -> item.getItem().getId().equals(ROCK_ITEM_ID))
                .count();
    }
}
