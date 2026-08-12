package com.mugunghwa.goodquestion.story.dialogue;

import com.mugunghwa.goodquestion.ai.analysis.AnalysisLlmClient;
import com.mugunghwa.goodquestion.global.vocab.ChildIntent;
import com.mugunghwa.goodquestion.global.vocab.ThinkingElement;
import com.mugunghwa.goodquestion.global.vocab.UtteranceValidity;
import com.mugunghwa.goodquestion.story.content.StoryScene;
import com.mugunghwa.goodquestion.story.dialogue.engine.AnalysisPostProcessor;
import com.mugunghwa.goodquestion.story.session.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

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
    @Transactional
    public UtteranceAnalysis analyze(Message childMessage, StoryScene scene,
                                     String previousCharacterMessage) {
        AnalysisLlmClient.AnalysisLlmResult raw = analysisLlmClient.analyze(
                new AnalysisLlmClient.AnalysisLlmInput(
                        sceneContext(scene),
                        scene.getSceneGoal(),
                        previousCharacterMessage,
                        childMessage.getText(),
                        scene.getRequiredElements(),
                        scene.getElementCriteria()));

        AnalysisPostProcessor.Result processed =
                postProcessor.process(toDetectedElements(raw), childMessage.getText());

        return analysisRepository.save(UtteranceAnalysis.builder()
                .message(childMessage)
                .childIntent(toIntent(raw.childIntent()))
                .mainPoint(raw.mainPoint())
                .detectedElements(processed.accepted())
                .utteranceValidity(toValidity(raw.utteranceValidity()))
                .modelId(raw.modelId())
                .droppedEvidence(processed.dropped())
                .build());
    }

    private String sceneContext(StoryScene scene) {
        return Stream.of(scene.getSceneDescription(), scene.getConflict())
                .filter(part -> part != null && !part.isBlank())
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");
    }

    /**
     * LLM이 낸 요소 이름을 스키마 값으로 옮긴다.
     *
     * <p>스키마에 없는 이름은 여기서 사라진다. DetectedElement가 ThinkingElement만 담을 수 있어
     * dropped_evidence에도 남길 자리가 없다 - 폐기 통계에는 잡히지 않는다는 뜻이다.
     * TODO: 스키마 밖 요소가 실제로 얼마나 나오는지 보려면 별도 카운터가 필요하다.
     */
    private List<DetectedElement> toDetectedElements(AnalysisLlmClient.AnalysisLlmResult raw) {
        List<DetectedElement> elements = new ArrayList<>();
        if (raw.detectedElements() == null) {
            return elements;
        }
        for (AnalysisLlmClient.AnalysisLlmResult.DetectedElementDto dto : raw.detectedElements()) {
            ThinkingElement type = parse(ThinkingElement.class, dto.type());
            if (type != null) {
                elements.add(new DetectedElement(type, dto.evidence()));
            }
        }
        return elements;
    }

    /** 두 값 모두 not null 컬럼이다. 모르는 값이 오면 판단에 쓰지 않는 쪽으로 떨어뜨린다. */
    private ChildIntent toIntent(String value) {
        ChildIntent intent = parse(ChildIntent.class, value);
        return intent != null ? intent : ChildIntent.UNCLEAR;
    }

    private UtteranceValidity toValidity(String value) {
        UtteranceValidity validity = parse(UtteranceValidity.class, value);
        return validity != null ? validity : UtteranceValidity.UNCLEAR;
    }

    private <E extends Enum<E>> E parse(Class<E> type, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
