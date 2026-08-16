package com.mugunghwa.goodquestion.learning.reward.stardust;

import com.mugunghwa.goodquestion.global.error.BusinessException;
import com.mugunghwa.goodquestion.global.error.ErrorCode;
import com.mugunghwa.goodquestion.learning.reward.StoryPlayCounter;
import com.mugunghwa.goodquestion.learning.reward.shop.Item;
import com.mugunghwa.goodquestion.learning.reward.stardust.dto.StardustAcknowledgeResponse;
import com.mugunghwa.goodquestion.learning.reward.stardust.dto.StardustTransactionResponse;
import com.mugunghwa.goodquestion.learning.reward.stardust.dto.StardustWalletResponse;
import com.mugunghwa.goodquestion.story.content.StoryScene;
import com.mugunghwa.goodquestion.story.session.StorySession;
import com.mugunghwa.goodquestion.user.child.Child;
import com.mugunghwa.goodquestion.user.child.ChildService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 별가루 지갑과 증감 이력 (보상-04~08).
 *
 * <p>적립 API는 클라이언트에 열지 않는다. 세션 완료 처리 안에서 서버가 직접 넣는다 —
 * 클라이언트가 잔액을 올릴 수 있으면 멱등 제약이 무의미해진다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StardustService {

    /** 이야기 완주 보상(보상-04). */
    private static final int STORY_COMPLETED_AWARD = 3;
    /** 유도 없이 목표를 통과한 장면당 보너스. */
    /**
     * 아이 생성 시 1회 지급(환영). 소품이 1~2라 한둘을 바로 사서 놓아 볼 수 있는 양이고,
     * 첫 완주 지급(3)과 같아 학습 보상보다 커지지 않는다. totalEarned에도 잡히지만
     * 가장 낮은 누적 해금 기준(4)에는 못 미쳐 해금 진행은 여전히 학습으로만 연다.
     */
    private static final int WELCOME_AWARD = 3;

    /** 단어 말하기 연습 1건. 소품 하나(1~2)에 못 미치는 잔돈이라 매일 해도 이야기 보상을 넘지 않는다. */
    private static final int WORD_PRACTICED_AWARD = 1;

    /**
     * 예문 따라 말하기 1건. 문장 전체를 90% 이상 정확히 말해야 해서 단어 연습(1)보다 크다.
     * 하루 상한이 2건이라 하루 최대 4개 — 완주 최대치(3+2)를 넘지 않는다.
     */
    private static final int SENTENCE_PRACTICED_AWARD = 2;

    private static final int SCENE_BONUS_AWARD = 1;
    /** 세션 합계를 3~5로 묶기 위한 상한. 유니크로는 표현할 수 없어 세어서 막는다. */
    private static final int SCENE_BONUS_MAX_PER_SESSION = 2;

    private final StardustWalletRepository walletRepository;
    private final StardustTransactionRepository transactionRepository;
    private final ChildService childService;
    private final StoryPlayCounter playCounter;
    private final EntityManager entityManager;

    public StardustWalletResponse getWallet(UUID parentId, UUID childId) {
        childService.getOwnedChild(parentId, childId);
        StardustWallet wallet = getWalletOf(childId);

        return new StardustWalletResponse(
                wallet.getBalance(),
                wallet.getTotalEarned(),
                transactionRepository
                        .findAllByWalletIdAndAcknowledgedFalseOrderByCreatedAtAsc(wallet.getId())
                        .stream().map(StardustService::toResponse).toList());
    }

    /**
     * 떨어지는 연출을 재생했다고 표시한다(보상-08).
     *
     * <p>연출 여부는 서버가 기억해야 한다. 클라이언트가 들고 있으면 앱을 지웠다 깔거나 기기를
     * 바꿨을 때 지난 지급이 전부 다시 떨어진다.
     */
    @Transactional
    public StardustAcknowledgeResponse acknowledge(UUID parentId, UUID childId) {
        childService.getOwnedChild(parentId, childId);
        StardustWallet wallet = getWalletOf(childId);

        List<StardustTransaction> pending = transactionRepository
                .findAllByWalletIdAndAcknowledgedFalseOrderByCreatedAtAsc(wallet.getId());
        pending.forEach(StardustTransaction::acknowledge);

        return new StardustAcknowledgeResponse(pending.size());
    }

    /**
     * 환영 지급. 아이 생성과 같은 트랜잭션에서 지갑을 만든 직후 불린다.
     *
     * <p>멱등을 따로 걸지 않는다 — 아이 생성이 유일한 호출처이고 지갑과 같은 트랜잭션이라
     * 두 번 지급되려면 아이가 두 명 만들어져야 한다.
     */
    @Transactional
    public StardustTransaction awardWelcome(Child child) {
        return earn(child.getId(), WELCOME_AWARD, StardustReason.WELCOME, null, null, null);
    }

    /**
     * 단어 말하기 연습 보상. 최초 1회·하루 상한 판정은 호출자(WordPracticeService)가 끝냈다 —
     * 여기서는 지급만 한다. 판정까지 가져오면 word_practices를 이 서비스가 알아야 한다.
     */
    @Transactional
    public StardustTransaction awardWordPracticed(Child child) {
        return earn(child.getId(), WORD_PRACTICED_AWARD, StardustReason.WORD_PRACTICED,
                null, null, null);
    }

    /**
     * 예문 따라 말하기 보상. 판정(일치율·최초 1회·하루 상한)은 호출자(SentencePracticeService)가
     * 끝냈다 — 여기서는 지급만 한다. 판정까지 가져오면 sentence_practices를 이 서비스가 알아야 한다.
     */
    @Transactional
    public StardustTransaction awardSentencePracticed(Child child) {
        return earn(child.getId(), SENTENCE_PRACTICED_AWARD, StardustReason.SENTENCE_PRACTICED,
                null, null, null);
    }

    /**
     * 이야기 완주 보상. 후속 활동 완료와 같은 트랜잭션에서 불린다.
     *
     * <p>같은 세션에 두 번 불려도 한 번만 지급한다. 완주 횟수는 upsert 반환값으로 정해
     * 조회와 증가 사이가 갈라지지 않게 한다.
     *
     * @return 이번에 만든 이력. 지급이 없으면 빈 목록
     */
    @Transactional
    public List<StardustTransaction> awardStoryCompleted(StorySession session) {
        if (transactionRepository.existsBySessionIdAndReason(
                session.getId(), StardustReason.STORY_COMPLETED)) {
            return List.of();
        }

        int playCount = playCounter.increaseAndGet(
                session.getChild().getId(), session.getStory().getId());
        int amount = completionAward(playCount);
        if (amount == 0) {
            return List.of();
        }

        return List.of(earn(session.getChild().getId(), amount, StardustReason.STORY_COMPLETED,
                session, null, null));
    }

    /**
     * 유도 없이 목표를 통과한 장면 보너스(보상-04). 장면 종료 시점에 불린다.
     *
     * <p>아직 부르는 곳이 없다. 장면 종료 판정이 대화 턴 파이프라인에 있는데 `story`는
     * `learning`을 참조할 수 없어, 파이프라인이 붙을 때 이벤트로 이 메서드에 닿아야 한다.
     */
    @Transactional
    public Optional<StardustTransaction> awardSceneBonus(StorySession session, StoryScene scene) {
        if (transactionRepository.existsBySessionIdAndSceneIdAndReason(
                session.getId(), scene.getId(), StardustReason.SCENE_BONUS)) {
            return Optional.empty();
        }
        // 2회차부터는 장면 보너스가 없다. 완주 전이므로 지금까지의 완주 횟수로 판단한다.
        if (playCounter.get(session.getChild().getId(), session.getStory().getId()) >= 1) {
            return Optional.empty();
        }
        if (transactionRepository.countBySessionIdAndReason(
                session.getId(), StardustReason.SCENE_BONUS) >= SCENE_BONUS_MAX_PER_SESSION) {
            return Optional.empty();
        }

        return Optional.of(earn(session.getChild().getId(), SCENE_BONUS_AWARD,
                StardustReason.SCENE_BONUS, session, scene, null));
    }

    /** 아이템 구매 차감. 잔액이 모자라면 409로 막힌다. */
    @Transactional
    public StardustWallet spendForPurchase(UUID childId, Item item) {
        StardustWallet wallet = lockedWalletOf(childId);
        wallet.spend(item.getPrice());
        transactionRepository.save(StardustTransaction.builder()
                .wallet(wallet)
                .amount(-item.getPrice())
                .reason(StardustReason.ITEM_PURCHASE)
                .item(item)
                .build());
        return wallet;
    }

    public static StardustTransactionResponse toResponse(StardustTransaction transaction) {
        return new StardustTransactionResponse(transaction.getId(), transaction.getAmount(),
                transaction.getReason(), transaction.getCreatedAt());
    }

    /** 1회차 전액, 2회차 절반(내림), 3회차부터 없음(보상-05). */
    private int completionAward(int playCount) {
        return switch (playCount) {
            case 1 -> STORY_COMPLETED_AWARD;
            case 2 -> STORY_COMPLETED_AWARD / 2;
            default -> 0;
        };
    }

    private StardustTransaction earn(UUID childId, int amount, StardustReason reason,
                                     StorySession session, StoryScene scene, Item item) {
        StardustWallet wallet = lockedWalletOf(childId);
        wallet.earn(amount);
        StardustTransaction saved = transactionRepository.saveAndFlush(StardustTransaction.builder()
                .wallet(wallet).amount(amount).reason(reason)
                .session(session).scene(scene).item(item)
                .build());

        // created_at은 DB 기본값이라 저장 직후 엔티티에는 비어 있다. 완주 응답이 지급 내역을
        // 그대로 담아 나가므로 되읽지 않으면 그 한 건만 시각이 null로 나간다.
        entityManager.refresh(saved);
        return saved;
    }

    private StardustWallet getWalletOf(UUID childId) {
        return walletRepository.findByChildId(childId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "별가루 지갑을 찾을 수 없습니다."));
    }

    /** 잔액을 바꾸는 경로는 잠그고 읽는다 — 지급과 구매가 겹치면 한쪽이 덮인다. */
    private StardustWallet lockedWalletOf(UUID childId) {
        return walletRepository.findByChildIdForUpdate(childId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "별가루 지갑을 찾을 수 없습니다."));
    }
}
