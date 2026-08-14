package com.mugunghwa.goodquestion.learning.postactivity;

import com.mugunghwa.goodquestion.global.error.BusinessException;
import com.mugunghwa.goodquestion.global.error.ErrorCode;
import com.mugunghwa.goodquestion.learning.postactivity.dto.CardSubmitRequest;
import com.mugunghwa.goodquestion.learning.postactivity.dto.CardSubmitResponse;
import com.mugunghwa.goodquestion.learning.postactivity.dto.PostActivityStartResponse;
import com.mugunghwa.goodquestion.learning.postactivity.dto.PostActivityStatusResponse;
import com.mugunghwa.goodquestion.learning.postactivity.dto.RetellingRequest;
import com.mugunghwa.goodquestion.learning.postactivity.dto.RetellingResponse;
import com.mugunghwa.goodquestion.learning.report.ReportService;
import com.mugunghwa.goodquestion.learning.reward.shop.Item;
import com.mugunghwa.goodquestion.learning.reward.shop.ItemRepository;
import com.mugunghwa.goodquestion.learning.reward.shop.ItemStatus;
import com.mugunghwa.goodquestion.learning.reward.shop.ItemUnlockPolicy;
import com.mugunghwa.goodquestion.learning.reward.stardust.StardustService;
import com.mugunghwa.goodquestion.learning.reward.stardust.StardustTransaction;
import com.mugunghwa.goodquestion.learning.reward.stardust.StardustTransactionRepository;
import com.mugunghwa.goodquestion.learning.reward.stardust.StardustWallet;
import com.mugunghwa.goodquestion.learning.reward.stardust.StardustWalletRepository;
import com.mugunghwa.goodquestion.story.content.Story;
import com.mugunghwa.goodquestion.story.session.SessionService;
import com.mugunghwa.goodquestion.story.session.SessionStatus;
import com.mugunghwa.goodquestion.story.session.StorySession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 말하기 후 활동 — 카드 순서 맞추기와 다시 이야기하기 (활동-01~10).
 *
 * <p>정답 판정은 서버만 한다. 정답 순서를 응답에 담지 않으므로 화면은 맞았는지 여부만 안다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostActivityService {

    /** 화면 분기용 진행 단계. 저장하지 않고 결과 행에서 파생한다. */
    private static final String STATUS_NOT_STARTED = "NOT_STARTED";
    private static final String STATUS_ORDER_PENDING = "ORDER_PENDING";
    private static final String STATUS_ORDER_CORRECT = "ORDER_CORRECT";
    private static final String STATUS_COMPLETED = "COMPLETED";

    private final PostActivityResultRepository resultRepository;
    private final SessionService sessionService;
    private final StardustService stardustService;
    private final StardustWalletRepository walletRepository;
    private final StardustTransactionRepository transactionRepository;
    private final ItemRepository itemRepository;
    private final ItemUnlockPolicy unlockPolicy;
    private final ReportService reportService;
    /**
     * 카드 순서 맞추기 시작. 카드를 무작위 순서로 준다(활동-02).
     *
     * <p>순서는 결과 행의 시드로 고정한다. 재진입할 때마다 섞으면 아이가 보던 화면이 달라지고,
     * 제출한 순서를 나중에 재현할 수도 없다.
     */
    @Transactional
    public PostActivityStartResponse start(UUID parentId, UUID sessionId) {
        StorySession session = getPostActivitySession(parentId, sessionId);
        PostActivityResult result = resultRepository.findBySessionId(sessionId)
                .orElseGet(() -> resultRepository.save(
                        PostActivityResult.builder().session(session).build()));

        return new PostActivityStartResponse(
                shuffledCards(session.getStory(), result.getCardOrderSeed()),
                result.getAttemptCount());
    }

    /** 새로고침 복구용 상태 조회(활동-08). 세션이 끝난 뒤에도 결과 화면을 다시 그릴 수 있어야 한다. */
    public PostActivityStatusResponse getStatus(UUID parentId, UUID sessionId) {
        StorySession session = sessionService.getOwnedSession(parentId, sessionId);
        PostActivityResult result = resultRepository.findBySessionId(sessionId).orElse(null);

        if (result == null) {
            return new PostActivityStatusResponse(
                    STATUS_NOT_STARTED, List.of(), (short) 0, null, null, null, null);
        }

        boolean orderCorrect = Boolean.TRUE.equals(result.getIsOrderCorrect());
        return new PostActivityStatusResponse(
                statusOf(result),
                shuffledCards(session.getStory(), result.getCardOrderSeed()),
                result.getAttemptCount(),
                result.getIsOrderCorrect(),
                orderCorrect ? retellingKeywords(session.getStory()) : null,
                result.getRetellingText(),
                result.getCompletedAt());
    }

    /**
     * 카드 순서 제출. 정답이면 다시 이야기하기에 쓸 열쇠말을 준다(활동-04~06).
     *
     * <p>오답이면 열쇠말을 주지 않고 시도 횟수만 올린다. 정답 순서는 어떤 경우에도 내려보내지 않는다.
     */
    @Transactional
    public CardSubmitResponse submitOrder(UUID parentId, UUID sessionId, CardSubmitRequest request) {
        StorySession session = getPostActivitySession(parentId, sessionId);
        PostActivityResult result = resultRepository.findBySessionId(sessionId)
                .orElseGet(() -> resultRepository.save(
                        PostActivityResult.builder().session(session).build()));

        boolean correct = correctOrder(session.getStory()).equals(request.submittedOrder());
        result.submitOrder(request.submittedOrder(), correct);

        return new CardSubmitResponse(correct, correct ? retellingKeywords(session.getStory()) : null);
    }

    /**
     * 다시 이야기하기 제출 = 세션 완료 + 별가루 지급(활동-09~10, 보상-04).
     *
     * <p>지급 결과를 응답에 담아야 하므로 완료 처리를 같은 트랜잭션에서 동기로 끝낸다.
     * 카드 순서를 맞히기 전에는 받지 않는다 — 이야기 흐름을 짚은 뒤에 말하게 하는 활동이다.
     */
    @Transactional
    public RetellingResponse submitRetelling(UUID parentId, UUID sessionId, RetellingRequest request) {
        StorySession session = getPostActivitySession(parentId, sessionId);
        PostActivityResult result = resultRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RETELLING_BEFORE_ORDER));
        if (!Boolean.TRUE.equals(result.getIsOrderCorrect())) {
            throw new BusinessException(ErrorCode.RETELLING_BEFORE_ORDER);
        }

        result.completeRetelling(request.text());
        session.complete();

        // 지급 전후로 해금이 달라질 수 있어(완주 횟수·누적 획득량) 먼저 지금 열린 것을 적어 둔다.
        Set<UUID> unlockedBefore = unlockedItemIds(session.getChild().getId());
        stardustService.awardStoryCompleted(session);
        List<Item> newlyUnlocked = itemRepository
                .findAllByStatusOrderByDisplayOrderAsc(ItemStatus.ACTIVE).stream()
                .filter(item -> !unlockedBefore.contains(item.getId()))
                .filter(item -> unlockPolicy.isUnlocked(item, session.getChild().getId(), totalEarnedOf(session)))
                .toList();

        // 리포트 생성은 비동기다. LLM 호출이 수 초 걸리는데 아이가 완료 화면을 기다릴
        // 이유가 없고, 실패해도 완주와 별가루 지급은 그대로 끝나야 한다.
        reportService.generate(session.getId());

        return new RetellingResponse(
                session.getStatus().name(),
                result.getCompletedAt(),
                sessionStardust(session),
                newlyUnlocked.stream()
                        .map(item -> new RetellingResponse.UnlockedItem(
                                item.getId(), item.getName(), item.getThumbnailUrl()))
                        .toList());
    }

    /** 완주 화면은 이번 세션에서 받은 별가루를 모두 보여 준다 — 장면 보너스는 놀이 중에 쌓였다. */
    private RetellingResponse.Stardust sessionStardust(StorySession session) {
        List<StardustTransaction> earned = transactionRepository
                .findAllBySessionIdOrderByCreatedAtAsc(session.getId()).stream()
                .filter(transaction -> transaction.getAmount() > 0)
                .toList();

        return new RetellingResponse.Stardust(
                earned.stream().mapToInt(StardustTransaction::getAmount).sum(),
                earned.stream().map(StardustService::toResponse).toList(),
                totalBalanceOf(session));
    }

    private String statusOf(PostActivityResult result) {
        if (result.getCompletedAt() != null) {
            return STATUS_COMPLETED;
        }
        return Boolean.TRUE.equals(result.getIsOrderCorrect()) ? STATUS_ORDER_CORRECT : STATUS_ORDER_PENDING;
    }

    private Set<UUID> unlockedItemIds(UUID childId) {
        int totalEarned = walletRepository.findByChildId(childId)
                .map(StardustWallet::getTotalEarned).orElse(0);
        return itemRepository.findAllByStatusOrderByDisplayOrderAsc(ItemStatus.ACTIVE).stream()
                .filter(item -> unlockPolicy.isUnlocked(item, childId, totalEarned))
                .map(Item::getId)
                .collect(Collectors.toSet());
    }

    private int totalEarnedOf(StorySession session) {
        return walletRepository.findByChildId(session.getChild().getId())
                .map(StardustWallet::getTotalEarned).orElse(0);
    }

    private int totalBalanceOf(StorySession session) {
        return walletRepository.findByChildId(session.getChild().getId())
                .map(StardustWallet::getBalance).orElse(0);
    }

    /** 시드가 같으면 언제 불러도 같은 순서가 나온다. */
    private List<PostActivityStartResponse.Card> shuffledCards(Story story, String seed) {
        List<PostActivityStartResponse.Card> cards = new ArrayList<>(cardsOf(story).stream()
                .map(card -> new PostActivityStartResponse.Card(
                        String.valueOf(card.get("id")), String.valueOf(card.get("text"))))
                .toList());
        Collections.shuffle(cards, new Random(seed.hashCode()));
        return List.copyOf(cards);
    }

    /** 정답 순서는 콘텐츠의 correct_order로만 만든다. 응답에는 절대 담지 않는다. */
    private List<String> correctOrder(Story story) {
        return cardsOf(story).stream()
                .sorted(java.util.Comparator.comparingInt(card ->
                        ((Number) card.get("correct_order")).intValue()))
                .map(card -> String.valueOf(card.get("id")))
                .toList();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> cardsOf(Story story) {
        Map<String, Object> config = story.getPostActivityConfig();
        if (config == null || !(config.get("cards") instanceof List<?> cards)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "후속 활동 구성이 없는 이야기입니다.");
        }
        return (List<Map<String, Object>>) cards;
    }

    @SuppressWarnings("unchecked")
    private List<String> retellingKeywords(Story story) {
        Map<String, Object> config = story.getPostActivityConfig();
        if (config == null || !(config.get("retelling_keywords") instanceof List<?> keywords)) {
            return List.of();
        }
        return ((List<Object>) keywords).stream().map(String::valueOf).toList();
    }

    /** 후속 활동은 세션이 그 단계에 있을 때만 받는다. 대화 중이거나 이미 끝난 세션이면 409다. */
    private StorySession getPostActivitySession(UUID parentId, UUID sessionId) {
        StorySession session = sessionService.getOwnedSession(parentId, sessionId);
        if (session.getStatus() != SessionStatus.POST_ACTIVITY) {
            throw new BusinessException(ErrorCode.SESSION_NOT_IN_PROGRESS,
                    "후속 활동 단계의 세션이 아닙니다.");
        }
        return session;
    }
}
