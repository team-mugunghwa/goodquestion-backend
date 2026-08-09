package com.mugunghwa.goodquestion.story.dialogue.dto;

import com.mugunghwa.goodquestion.global.vocab.ResponseMode;
import com.mugunghwa.goodquestion.story.session.dto.MessageResponse;
import com.mugunghwa.goodquestion.story.session.SceneEndReason;
import com.mugunghwa.goodquestion.story.session.dto.SceneResponse;

import java.util.List;

public record UtteranceResponse(
        MessageResponse childMessage,
        MessageResponse characterMessage,
        Progress progress,
        MissionTrigger missionTrigger,    // 미션 노출 신호. 노출 아님 시 null
        SceneTransition sceneTransition   // 장면 유지 시 null
) {
    public record Progress(ResponseMode mode, List<String> accumulatedElements,
                           List<String> missingElements, boolean sceneGoalMet,
                           SceneEndReason closingReason) {}

    /** mission_config에서 클라이언트 표시에 필요한 부분만 추출해 전달 */
    public record MissionTrigger(String missionId, String name, String purpose,
                                 List<String> checkPoints, List<String> examples) {}

    public record SceneTransition(Type type, SceneResponse nextScene,
                                  MessageResponse openingMessage) {
        public enum Type { NEXT_SCENE, POST_ACTIVITY }
    }
}
