package com.mugunghwa.goodquestion.story.mission;

import com.mugunghwa.goodquestion.global.security.CurrentParentId;
import com.mugunghwa.goodquestion.story.mission.dto.CurrentMissionResponse;
import com.mugunghwa.goodquestion.story.mission.dto.MissionResultRequest;
import com.mugunghwa.goodquestion.story.mission.dto.MissionResultResponse;
import com.mugunghwa.goodquestion.story.session.SessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 미션 조회·결과 제출(미션-02~09).
 *
 * <p>노출 판정 자체는 턴 처리 중 서버가 수행하므로 여기서는 노출된 미션을 읽고 결과만 받는다.
 * TODO: 결과 제출(MissionResult 저장 + 다음 턴 대사 반영) 구현.
 */
@RestController
@RequestMapping("/api/sessions/{sessionId}/missions")
@RequiredArgsConstructor
public class MissionController {

    private final SessionService sessionService;
    private final MissionService missionService;

    @GetMapping("/current")
    public CurrentMissionResponse getCurrentMission(@CurrentParentId UUID parentId,
                                                    @PathVariable UUID sessionId) {
        return new CurrentMissionResponse(missionService.exposedMissionOf(
                sessionService.getOwnedSession(parentId, sessionId)));
    }

    @PostMapping("/{missionId}/result")
    @ResponseStatus(HttpStatus.CREATED)
    public MissionResultResponse submitResult(@CurrentParentId UUID parentId,
                                              @PathVariable UUID sessionId,
                                              @PathVariable String missionId,
                                              @Valid @RequestBody MissionResultRequest request) {
        throw new UnsupportedOperationException("미구현: 미션 결과 제출");
    }
}
