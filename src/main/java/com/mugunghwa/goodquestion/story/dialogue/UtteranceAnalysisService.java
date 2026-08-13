package com.mugunghwa.goodquestion.story.dialogue;

import com.mugunghwa.goodquestion.ai.analysis.AnalysisLlmClient;
import com.mugunghwa.goodquestion.global.vocab.ChildIntent;
import com.mugunghwa.goodquestion.global.vocab.ThinkingElement;
import com.mugunghwa.goodquestion.global.vocab.UtteranceValidity;
import com.mugunghwa.goodquestion.story.dialogue.engine.AnalysisPostProcessor;
import com.mugunghwa.goodquestion.story.session.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 발화 분석 - LLM 호출과 후처리, 그리고 저장.
 *
 * <p>둘을 한 메서드에 두지 않는다. LLM 호출은 수 초가 걸리고 저장은 수 밀리초인데, 묶어 두면
 * 저장 트랜잭션이 LLM 대기 시간만큼 DB 커넥션을 쥐게 된다.
 * 자세한 배경은 docs/트러블슈팅_턴_처리_커넥션_점유.md에 있다.
 */
@Service
@RequiredArgsConstructor
public class UtteranceAnalysisService {

    private final AnalysisLlmClient analysisLlmClient;
    private final AnalysisPostProcessor postProcessor;
    private final UtteranceAnalysisRepository analysisRepository;

    /**
     * 분석 LLM 호출과 서버 후처리. <b>트랜잭션 밖에서 부른다 - DB를 만지지 않는다.</b>
     *
     * <p>LLM 입력 (발화 분석 문서 4장): sceneContext, goal, previousCharacterMessage,
     * childUtterance, targetElements, elementCriteria
     */
    public AnalysisOutcome analyze(String childUtterance, SceneAnalysisContext scene,
                                   String previousCharacterMessage) {
        AnalysisLlmClient.AnalysisLlmResult raw = analysisLlmClient.analyze(
                new AnalysisLlmClient.AnalysisLlmInput(
                        scene.sceneContext(), scene.goal(), previousCharacterMessage,
                        childUtterance, scene.targetElements(), scene.elementCriteria()));

        AnalysisPostProcessor.Result processed =
                postProcessor.process(toDetectedElements(raw), childUtterance);

        return new AnalysisOutcome(
                toIntent(raw.childIntent()), raw.mainPoint(),
                processed.accepted(), processed.dropped(),
                toValidity(raw.utteranceValidity()), raw.modelId());
    }

    /** 후처리를 통과한 분석 저장. 아이 메시지가 먼저 저장돼 있어야 한다(1:1). */
    @Transactional
    public UtteranceAnalysis save(Message childMessage, AnalysisOutcome outcome) {
        return analysisRepository.save(UtteranceAnalysis.builder()
                .message(childMessage)
                .childIntent(outcome.childIntent())
                .mainPoint(outcome.mainPoint())
                .detectedElements(outcome.detectedElements())
                .utteranceValidity(outcome.utteranceValidity())
                .modelId(outcome.modelId())
                .droppedEvidence(outcome.droppedEvidence())
                .build());
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
