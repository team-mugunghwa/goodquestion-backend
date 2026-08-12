package com.mugunghwa.goodquestion.learning.reward.stardust;

import com.mugunghwa.goodquestion.global.security.CurrentParentId;
import com.mugunghwa.goodquestion.learning.reward.stardust.dto.StardustAcknowledgeResponse;
import com.mugunghwa.goodquestion.learning.reward.stardust.dto.StardustWalletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/** 별가루 지갑(보상-07~08). 적립은 세션 완료 처리 안에서 서버가 넣으므로 API로 열지 않는다. */
@RestController
@RequestMapping("/api/children/{childId}/stardust")
@RequiredArgsConstructor
public class StardustController {

    private final StardustService stardustService;

    @GetMapping
    public StardustWalletResponse getWallet(@CurrentParentId UUID parentId,
                                            @PathVariable UUID childId) {
        return stardustService.getWallet(parentId, childId);
    }

    /** 떨어지는 연출을 재생한 뒤 미확인 지급분을 확인 처리한다. */
    @PostMapping("/acknowledge")
    public StardustAcknowledgeResponse acknowledge(@CurrentParentId UUID parentId,
                                                   @PathVariable UUID childId) {
        return stardustService.acknowledge(parentId, childId);
    }
}
