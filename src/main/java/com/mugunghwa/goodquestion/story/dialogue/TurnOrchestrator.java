package com.mugunghwa.goodquestion.story.dialogue;

import com.mugunghwa.goodquestion.story.dialogue.engine.AnalysisPostProcessor;
import com.mugunghwa.goodquestion.story.dialogue.dto.UtteranceRequest;
import com.mugunghwa.goodquestion.story.dialogue.dto.UtteranceResponse;
import com.mugunghwa.goodquestion.story.mission.MissionPolicy;
import com.mugunghwa.goodquestion.story.dialogue.engine.ProgressionEngine;
import com.mugunghwa.goodquestion.global.error.BusinessException;
import com.mugunghwa.goodquestion.global.error.ErrorCode;
import com.mugunghwa.goodquestion.story.session.SessionService;
import com.mugunghwa.goodquestion.story.session.StorySession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 발화 처리 파이프라인 오케스트레이터.
 * 분석·진행·표현의 책임 분리 원칙: 각 단계는 하위 컴포넌트가 담당하고 여기서는 순서만 조율한다.
 * 미션 수행 결과(request.missionId 존재)도 동일 파이프라인으로 분석·누적한다 —
 * 미션 check_points가 장면 target_elements와 대응하므로 별도 채점 없이 요소 충족으로 확인한다.
 */
@Service
@RequiredArgsConstructor
public class TurnOrchestrator {

    private final SessionService sessionService;
    private final UtteranceAnalysisService analysisService;
    private final AnalysisPostProcessor postProcessor;
    private final ProgressionEngine progressionEngine;
    private final MissionPolicy missionPolicy;
    private final CharacterResponseService characterResponseService;
    private final SceneClosingHandler sceneClosingHandler;
    // TODO: MessageService 주입

    @Transactional
    public UtteranceResponse processUtterance(UUID parentId, UUID sessionId, UtteranceRequest request) {
        StorySession session = sessionService.getOwnedSession(parentId, sessionId);
        if (!session.isInProgress()) {
            throw new BusinessException(ErrorCode.SESSION_NOT_IN_PROGRESS);
        }
        // TODO: 현재 장면이 DIALOGUE인지 검증 (STORY면 SCENE_NOT_DIALOGUE 에러)

        // ① child 메시지 저장 (text, stt_raw_text)
        //    request.missionId가 있으면 미션 수행 발화 — session.completeMission() 처리
        // ② 발화 분석 LLM 호출 (sceneContext, goal, previousCharacterMessage,
        //    childUtterance, targetElements, elementCriteria)
        // ③ 서버 후처리 (postProcessor)
        // ④ 진행 규칙 엔진 — 누적 상태 갱신 + NORMAL/GUIDED/CLOSING 결정
        //    (미션 필수 장면의 종료 조건: 요소 충족 && missionCompleted — 콘텐츠 문서 출력 조건 3)
        // ⑤ 미션 노출 판단 — scene.hasMission() && !missionExposed && missionPolicy.shouldExpose()
        //    → session.exposeMission(), 응답 missionTrigger 구성 (mission_config에서 추출)
        // ⑥-a NORMAL/GUIDED: 캐릭터 LLM 호출(감정 포함) → CHARACTER 메시지 저장
        //    미션 노출 턴에는 캐릭터 대사가 미션을 자연스럽게 이어주도록 프롬프트에 반영
        // ⑥-b CLOSING: sceneClosingHandler — LLM 짧은 반응 + 고정 마지막 대사 재생 후
        //    다음 장면 이동(다음이 STORY면 이동만, DIALOGUE면 opening 저장) 또는 후속 활동 전환
        // ⑦ UtteranceResponse 조립 (missingElements = required - accumulated)
        throw new UnsupportedOperationException("TODO: 파이프라인 구현");
    }
}
