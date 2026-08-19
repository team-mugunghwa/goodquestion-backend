package com.mugunghwa.goodquestion.learning.reward;

import com.mugunghwa.goodquestion.learning.reward.stardust.StardustReason;
import com.mugunghwa.goodquestion.learning.reward.stardust.StardustTransactionRepository;
import com.mugunghwa.goodquestion.learning.reward.stardust.StardustWallet;
import com.mugunghwa.goodquestion.learning.reward.stardust.StardustWalletRepository;
import com.mugunghwa.goodquestion.support.IntegrationTest;
import com.mugunghwa.goodquestion.user.auth.AuthService;
import com.mugunghwa.goodquestion.user.auth.dto.LoginRequest;
import com.mugunghwa.goodquestion.user.auth.dto.SignUpRequest;
import com.mugunghwa.goodquestion.user.child.ChildService;
import com.mugunghwa.goodquestion.user.child.dto.ChildCreateRequest;
import com.mugunghwa.goodquestion.user.parent.ParentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 최초 로그인 별가루 — 계정당 1회, 아이 전원.
 *
 * <p>@Transactional을 쓰지 않는다. 지급이 로그인 커밋 뒤 REQUIRES_NEW로 일어나므로
 * 테스트가 트랜잭션 안에 있으면 커밋이 없어 리스너 자체가 불리지 않는다
 * ({@link com.mugunghwa.goodquestion.user.auth.LoginLockTest}와 같은 이유).
 * 대신 뒷정리를 직접 한다 — 계정을 지우면 아이·지갑·선점 기록이 FK cascade로 함께 지워진다.
 */
@IntegrationTest
class FirstLoginBonusTest {

    private static final String EMAIL = "first-login-bonus@test.com";
    private static final String PASSWORD = "password123";
    private static final String IP = "127.0.0.1";

    /** 아이 생성 시 함께 들어오는 환영 지급. 최초 로그인 지급과 겹쳐 보이지 않게 빼고 센다. */
    private static final int WELCOME = 3;
    private static final int FIRST_LOGIN = 100;

    @Autowired
    private AuthService authService;

    @Autowired
    private ChildService childService;

    @Autowired
    private ParentRepository parentRepository;

    @Autowired
    private StardustWalletRepository walletRepository;

    @Autowired
    private StardustTransactionRepository transactionRepository;

    @AfterEach
    void 뒷정리() {
        parentRepository.findByEmail(EMAIL).ifPresent(parentRepository::delete);
    }

    private UUID 가입한다() {
        return authService.signUp(new SignUpRequest(EMAIL, PASSWORD, "김보호"), IP).parent().id();
    }

    private UUID 아이를_만든다(UUID parentId, String name) {
        return childService.create(parentId, new ChildCreateRequest(name, (short) 2019)).id();
    }

    private void 로그인한다() {
        authService.login(new LoginRequest(EMAIL, PASSWORD), IP);
    }

    private int 잔액(UUID childId) {
        return walletRepository.findByChildId(childId).orElseThrow().getBalance();
    }

    @Test
    void 첫_로그인에_아이_지갑으로_백이_들어온다() {
        UUID parentId = 가입한다();
        UUID childId = 아이를_만든다(parentId, "지우");

        로그인한다();

        assertThat(잔액(childId)).isEqualTo(WELCOME + FIRST_LOGIN);
    }

    /** 로그인은 보호자 단위라 아이를 고르지 않는다 — 형제가 있으면 각자 받는다. */
    @Test
    void 아이가_여럿이면_전원이_받는다() {
        UUID parentId = 가입한다();
        UUID first = 아이를_만든다(parentId, "지우");
        UUID second = 아이를_만든다(parentId, "하준");

        로그인한다();

        assertThat(잔액(first)).isEqualTo(WELCOME + FIRST_LOGIN);
        assertThat(잔액(second)).isEqualTo(WELCOME + FIRST_LOGIN);
    }

    /** 핵심 — 이게 막히지 않으면 로그아웃/로그인 반복으로 무한히 벌 수 있다. */
    @Test
    void 두_번째_로그인부터는_주지_않는다() {
        UUID parentId = 가입한다();
        UUID childId = 아이를_만든다(parentId, "지우");

        로그인한다();
        로그인한다();
        로그인한다();

        assertThat(잔액(childId)).isEqualTo(WELCOME + FIRST_LOGIN);
    }

    /**
     * 가입 직후 첫 로그인은 아이를 만들기 전이다. 여기서 기회를 소진하면 아무도 못 받는다 —
     * 아이가 생긴 뒤 첫 로그인이 실제 지급 시점이어야 한다.
     */
    @Test
    void 아이가_없을_때_로그인해도_기회는_남는다() {
        가입한다();
        로그인한다();

        UUID parentId = parentRepository.findByEmail(EMAIL).orElseThrow().getId();
        UUID childId = 아이를_만든다(parentId, "지우");
        로그인한다();

        assertThat(잔액(childId)).isEqualTo(WELCOME + FIRST_LOGIN);
    }

    /** 지급이 끝난 계정에 아이가 늘어도 다시 열리지 않는다. 새 아이는 환영 지급만 받는다. */
    @Test
    void 지급_뒤에_생긴_아이는_환영만_받는다() {
        UUID parentId = 가입한다();
        아이를_만든다(parentId, "지우");
        로그인한다();

        UUID late = 아이를_만든다(parentId, "늦둥이");
        로그인한다();

        assertThat(잔액(late)).isEqualTo(WELCOME);
    }

    @Test
    void 지급은_FIRST_LOGIN_사유로_기록된다() {
        UUID parentId = 가입한다();
        UUID childId = 아이를_만든다(parentId, "지우");

        로그인한다();

        StardustWallet wallet = walletRepository.findByChildId(childId).orElseThrow();
        assertThat(transactionRepository
                .findAllByWalletIdAndAcknowledgedFalseOrderByCreatedAtAsc(wallet.getId()))
                .anySatisfy(transaction -> {
                    assertThat(transaction.getReason()).isEqualTo(StardustReason.FIRST_LOGIN);
                    assertThat(transaction.getAmount()).isEqualTo(FIRST_LOGIN);
                });
    }
}
