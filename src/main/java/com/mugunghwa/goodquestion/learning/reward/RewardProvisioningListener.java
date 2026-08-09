package com.mugunghwa.goodquestion.learning.reward;

import com.mugunghwa.goodquestion.learning.reward.planet.Planet;
import com.mugunghwa.goodquestion.learning.reward.planet.PlanetRepository;
import com.mugunghwa.goodquestion.learning.reward.stardust.StardustWallet;
import com.mugunghwa.goodquestion.learning.reward.stardust.StardustWalletRepository;
import com.mugunghwa.goodquestion.user.child.Child;
import com.mugunghwa.goodquestion.user.child.ChildCreatedEvent;
import com.mugunghwa.goodquestion.user.child.ChildRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 아이 생성 시 행성·지갑을 함께 만든다(계정-14).
 *
 * <p>둘 다 child_id UNIQUE라 뒤늦게 만들면 동시 요청에서 충돌한다. 아이 생성과 같은
 * 트랜잭션에서 끝내야 해서 동기 리스너를 쓴다 — 실패하면 아이 생성도 함께 롤백된다.
 */
@Component
@RequiredArgsConstructor
public class RewardProvisioningListener {

    private final ChildRepository childRepository;
    private final PlanetRepository planetRepository;
    private final StardustWalletRepository walletRepository;

    @EventListener
    public void provision(ChildCreatedEvent event) {
        Child child = childRepository.getReferenceById(event.childId());
        planetRepository.save(Planet.builder().child(child).build());
        walletRepository.save(StardustWallet.builder().child(child).build());
    }
}
