package com.mugunghwa.goodquestion.story.dialogue;

import com.mugunghwa.goodquestion.story.dialogue.dto.AnalysisResponse;
import com.mugunghwa.goodquestion.story.dialogue.engine.ProgressionDecision;
import com.mugunghwa.goodquestion.story.mission.dto.MissionResponse;
import com.mugunghwa.goodquestion.story.session.dto.MessageResponse;
import com.mugunghwa.goodquestion.story.session.dto.ProgressResponse;

/**
 * 아이 턴을 반영한 트랜잭션이 남긴 것. 캐릭터 LLM 호출과 응답 조립이 이 값만 보고 진행된다.
 *
 * <p>진행 상태(progress)를 여기 담아 두는 이유가 있다. 장면이 닫히는 턴이면 다음 트랜잭션에서
 * 장면을 옮기는데, 그러면 장면 단위 누적이 초기화돼 끝난 장면의 상태를 더는 만들 수 없다.
 */
record ChildTurnRecord(MessageResponse childMessage,
                       AnalysisResponse analysis,
                       ProgressResponse progress,
                       MissionResponse mission,
                       ProgressionDecision decision,
                       CharacterPrompt characterPrompt) {
}
