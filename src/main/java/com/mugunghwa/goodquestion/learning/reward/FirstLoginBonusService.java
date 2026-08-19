package com.mugunghwa.goodquestion.learning.reward;

import com.mugunghwa.goodquestion.learning.reward.stardust.FirstLoginBonusGrantRepository;
import com.mugunghwa.goodquestion.learning.reward.stardust.StardustService;
import com.mugunghwa.goodquestion.user.child.Child;
import com.mugunghwa.goodquestion.user.child.ChildRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 최초 로그인 별가루 — 계정당 1회, 그 계정의 아이 전원에게 지급한다.
 *
 * <p>지갑은 아이당인데 로그인은 보호자 단위다. 로그인 시점에 "누구에게" 줄지가 정해져
 * 있지 않으므로 아이를 고르지 않고 전원에게 같은 양을 넣는다 — 한 아이만 고르면
 * 형제 중 누구를 고를지에 대한 규칙이 또 필요해진다.
 *
 * <p>아이가 아직 없으면 선점하지 않고 그냥 돌아간다. 가입 직후 첫 로그인은 아이를 만들기
 * 전이라, 여기서 기록만 남기면 지급 없이 기회가 소진된다. 아이가 생긴 뒤 첫 로그인이
 * 실제 지급 시점이 된다.
 */
@Service
@RequiredArgsConstructor
public class FirstLoginBonusService {

    private final ChildRepository childRepository;
    private final FirstLoginBonusGrantRepository grantRepository;
    private final StardustService stardustService;

    /**
     * 로그인 트랜잭션이 커밋된 뒤에 불린다. 그 시점에는 바깥 트랜잭션이 이미 끝나 있어
     * REQUIRED로 참여하면 여기서 쓴 내용이 커밋되지 않는다 — 새 트랜잭션으로 연다.
     *
     * <p>선점과 지급이 같은 트랜잭션이라 둘이 갈라지지 않는다. 지급이 실패하면 선점도
     * 함께 롤백돼 다음 로그인에 다시 시도된다.
     *
     * @return 이번에 지급한 아이 수. 0이면 이미 지급됐거나 아이가 없는 계정
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int grant(UUID parentId) {
        List<Child> children = childRepository.findAllByParentId(parentId);
        if (children.isEmpty()) {
            return 0;
        }
        // 동시에 들어온 두 로그인 중 삽입에 성공한 쪽만 지급한다.
        if (grantRepository.claim(parentId) == 0) {
            return 0;
        }
        children.forEach(stardustService::awardFirstLogin);
        return children.size();
    }
}
