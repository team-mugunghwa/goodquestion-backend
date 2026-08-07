package com.mugunghwa.goodquestion.dialog.analysis;

import com.mugunghwa.goodquestion.infra.llm.AnalysisLlmClient;
import com.mugunghwa.goodquestion.session.message.Message;
import com.mugunghwa.goodquestion.story.scene.StoryScene;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UtteranceAnalysisService {

    private final AnalysisLlmClient analysisLlmClient;
    private final AnalysisPostProcessor postProcessor;
    private final UtteranceAnalysisRepository analysisRepository;

    /**
     * 분석 LLM 호출 → 서버 후처리 → 통과본 저장.
     * LLM 입력 (발화 분석 문서 4장):
     *  sceneContext = scene.sceneDescription + conflict
     *  goal = scene.sceneGoal
     *  targetElements = scene.requiredElements
     *  elementCriteria = scene.elementCriteria   (장면별 인정 기준)
     */
    public UtteranceAnalysis analyze(Message childMessage, StoryScene scene, String previousCharacterMessage) {
        // TODO: ① AnalysisLlmClient.analyze 호출
        //       ② postProcessor.process로 evidence 검증·중복 정리
        //       ③ UtteranceAnalysis 저장 후 반환
        throw new UnsupportedOperationException("TODO");
    }
}
