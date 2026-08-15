package com.mugunghwa.goodquestion.story.mission;

import com.mugunghwa.goodquestion.global.security.CurrentParentId;
import com.mugunghwa.goodquestion.story.mission.dto.CurrentMissionResponse;
import com.mugunghwa.goodquestion.story.session.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * 미션 조회(미션-02~09).
 *
 * <p>노출 판정은 턴 처리 중 서버가 수행하므로 여기서는 노출된 미션을 읽기만 한다.
 * 수행 결과 제출은 별도 API가 아니다 - 아이가 미션에 대해 말한 발화를
 * {@code POST /utterances}에 {@code missionId}를 실어 보내면 턴 파이프라인이
 * 완료 표시와 분석을 함께 처리한다(이야기_전개_가이드.md 3.5). 한때 있던
 * {@code POST /{missionId}/result} 계약은 2026-08-15에 제거했다.
 */
@RestController
@RequestMapping("/api/sessions/{sessionId}/missions")
@RequiredArgsConstructor
public class MissionController {

    private final SessionService sessionService;

    @GetMapping("/current")
    public CurrentMissionResponse getCurrentMission(@CurrentParentId UUID parentId,
                                                    @PathVariable UUID sessionId) {
        return new CurrentMissionResponse(sessionService.currentMission(parentId, sessionId));
    }
}
