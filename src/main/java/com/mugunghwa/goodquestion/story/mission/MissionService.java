package com.mugunghwa.goodquestion.story.mission;

import com.mugunghwa.goodquestion.story.content.StoryScene;
import com.mugunghwa.goodquestion.story.mission.dto.MissionResponse;
import com.mugunghwa.goodquestion.story.session.StorySession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 노출된 미션 조회.
 *
 * <p>노출 판정은 턴 처리 안에서 끝난다. 여기서는 이미 노출된 미션을 다시 읽을 뿐이다 -
 * 새로고침이나 이어하기로 오버레이를 잃어버렸을 때 쓴다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MissionService {

    private final MissionConfigReader configReader;

    /** 미노출 상태면 null이다. 노출 여부는 정상 상태이지 오류가 아니다(미션-02). */
    public MissionResponse exposedMissionOf(StorySession session) {
        StoryScene scene = session.getCurrentScene();
        if (scene == null || !scene.hasMission() || !session.isMissionExposed()) {
            return null;
        }
        return configReader.toResponse(scene);
    }
}
