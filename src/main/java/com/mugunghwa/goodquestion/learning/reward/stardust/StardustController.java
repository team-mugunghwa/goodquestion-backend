package com.mugunghwa.goodquestion.learning.reward.stardust;

import com.mugunghwa.goodquestion.global.security.CurrentParentId;
import com.mugunghwa.goodquestion.learning.reward.stardust.dto.StardustAcknowledgeResponse;
import com.mugunghwa.goodquestion.learning.reward.stardust.dto.StardustWalletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/** 별가루 지갑(보상-07~08). TODO: StardustWallet·StardustTransaction 엔티티와 서비스 구현. */
@RestController
@RequestMapping("/api/children/{childId}/stardust")
@RequiredArgsConstructor
public class StardustController {

    @GetMapping
    public StardustWalletResponse getWallet(@CurrentParentId UUID parentId,
                                            @PathVariable UUID childId) {
        throw new UnsupportedOperationException("미구현: 별가루 지갑 조회");
    }

    /** 떨어지는 연출을 재생한 뒤 미확인 지급분을 확인 처리한다. */
    @PostMapping("/acknowledge")
    public StardustAcknowledgeResponse acknowledge(@CurrentParentId UUID parentId,
                                                   @PathVariable UUID childId) {
        throw new UnsupportedOperationException("미구현: 별가루 획득 연출 확인");
    }
}
