package com.mugunghwa.goodquestion.learning.reward;

import com.mugunghwa.goodquestion.learning.reward.stardust.StardustReason;
import com.mugunghwa.goodquestion.learning.reward.stardust.StardustTransactionRepository;
import com.mugunghwa.goodquestion.learning.reward.stardust.StardustWallet;
import com.mugunghwa.goodquestion.learning.reward.stardust.StardustWalletRepository;
import com.mugunghwa.goodquestion.support.IntegrationTest;
import com.mugunghwa.goodquestion.user.child.ChildService;
import com.mugunghwa.goodquestion.user.child.dto.ChildCreateRequest;
import com.mugunghwa.goodquestion.user.child.dto.ChildResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 아이 생성 시 보상 프로비저닝(계정-14) — 행성·지갑에 더해 환영 별가루까지.
 *
 * <p>새 지갑이 잔액 0이면 첫 완주 전에는 상점에서 아무것도 살 수 없어, 행성 탭을
 * 먼저 연 아이가 빈 화면만 만난다. 환영 지급으로 사이클을 시작 전에 한 바퀴 맛보게 한다.
 */
@IntegrationTest
@Transactional
class RewardProvisioningTest {

    /** 데모 시드의 보호자(demo@goodquestion.kr). 아이 생성은 보호자에 매달린다. */
    private static final UUID PARENT_ID = UUID.fromString("99999999-9999-9999-9999-000000000001");

    @Autowired
    private ChildService childService;

    @Autowired
    private StardustWalletRepository walletRepository;

    @Autowired
    private StardustTransactionRepository transactionRepository;

    @Test
    void 아이를_만들면_환영_별가루가_함께_들어온다() {
        ChildResponse child = childService.create(PARENT_ID, new ChildCreateRequest("새싹이", (short) 2019));

        StardustWallet wallet = walletRepository.findByChildId(child.id()).orElseThrow();
        assertThat(wallet.getBalance()).isEqualTo(3);
        assertThat(wallet.getTotalEarned()).isEqualTo(3);
    }

    @Test
    void 환영_지급은_WELCOME_사유로_기록된다() {
        ChildResponse child = childService.create(PARENT_ID, new ChildCreateRequest("새싹이", (short) 2019));

        StardustWallet wallet = walletRepository.findByChildId(child.id()).orElseThrow();
        assertThat(transactionRepository.findAllByWalletIdAndAcknowledgedFalseOrderByCreatedAtAsc(wallet.getId()))
                .singleElement()
                .satisfies(transaction -> {
                    assertThat(transaction.getReason()).isEqualTo(StardustReason.WELCOME);
                    assertThat(transaction.getAmount()).isEqualTo(3);
                });
    }

    /** 환영 3은 가장 낮은 누적 해금 기준(모닥불 4)에 못 미친다 — 해금은 여전히 학습으로만 연다. */
    @Test
    void 환영_별가루만으로는_아무_아이템도_해금되지_않는다() {
        ChildResponse child = childService.create(PARENT_ID, new ChildCreateRequest("새싹이", (short) 2019));

        StardustWallet wallet = walletRepository.findByChildId(child.id()).orElseThrow();
        assertThat(wallet.getTotalEarned()).isLessThan(4);
    }
}
