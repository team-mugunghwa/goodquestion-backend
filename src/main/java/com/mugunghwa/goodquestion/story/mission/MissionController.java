package com.mugunghwa.goodquestion.story.mission;

import com.mugunghwa.goodquestion.global.security.CurrentParentId;
import com.mugunghwa.goodquestion.story.mission.dto.CurrentMissionResponse;
import com.mugunghwa.goodquestion.story.mission.dto.MissionResultRequest;
import com.mugunghwa.goodquestion.story.mission.dto.MissionResultResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 미션 조회·결과 제출(미션-02~09).
 *
 * <p>노출 판정 자체는 턴 처리 중 서버가 수행하므로 여기서는 노출된 미션을 읽고 결과만 받는다.
 * TODO: MissionResult 엔티티와 MissionService 구현.
 */
@RestController
@RequestMapping("/api/sessions/{sessionId}/missions")
@RequiredArgsConstructor
public class MissionController {

    @GetMapping("/current")
    public CurrentMissionResponse getCurrentMission(@CurrentParentId UUID parentId,
                                                    @PathVariable UUID sessionId) {
        throw new UnsupportedOperationException("미구현: 현재 미션 조회");
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
