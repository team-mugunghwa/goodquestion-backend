package com.mugunghwa.goodquestion.learning.report;

import com.mugunghwa.goodquestion.global.vocab.ReportAxis;
import com.mugunghwa.goodquestion.global.vocab.ThinkingElement;
import com.mugunghwa.goodquestion.learning.report.dto.AxisScoreResponse;
import com.mugunghwa.goodquestion.story.content.StoryScene;
import com.mugunghwa.goodquestion.story.content.StorySceneRepository;
import com.mugunghwa.goodquestion.story.dialogue.DetectedElement;
import com.mugunghwa.goodquestion.story.dialogue.UtteranceAnalysis;
import com.mugunghwa.goodquestion.story.dialogue.UtteranceAnalysisRepository;
import com.mugunghwa.goodquestion.story.session.SessionService;
import com.mugunghwa.goodquestion.story.session.SessionStatus;
import com.mugunghwa.goodquestion.story.session.StorySession;
import com.mugunghwa.goodquestion.story.session.StorySessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 보호자 리포트 6각 그래프 축 점수 — {@link ReportService}와 별개 경로다.
 *
 * <p>{@code reports} 테이블(요약·강점·다음 연습)은 LLM 벤더가 아직 정해지지 않아
 * {@code ReportService.generate()}가 비어 있고, 세션이 끝나도 {@code Report} 행이 생기지
 * 않는다. 이 서비스는 그 상태와 무관하게 {@code story_scenes.required_elements}와
 * {@code utterance_analyses.detected_elements}만 집계하는 순수 서버 계산이라 Report 행 유무를
 * 타지 않는다 — LLM 파이프라인이 막혀 있어도 그래프는 실데이터로 뜬다.
 *
 * <p>설계: claude/보호자리포트_6축그래프_설계안_D6.md
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AxisScoreService {

    /** 지난 회차 평균에 반영하는 최근 완료 세션 수 (D6 4장). */
    private static final int PREVIOUS_SESSION_LIMIT = 3;

    private final SessionService sessionService;
    private final StorySceneRepository sceneRepository;
    private final UtteranceAnalysisRepository analysisRepository;
    private final StorySessionRepository sessionRepository;

    public List<AxisScoreResponse> getAxisScores(UUID parentId, UUID sessionId) {
        StorySession session = sessionService.getOwnedSession(parentId, sessionId);

        Map<ReportAxis, AxisAccumulator> currentRaw =
                computeRaw(sessionId, session.getStory().getId());

        List<StorySession> previousSessions = sessionRepository
                .findTop3ByChildIdAndStatusAndIdNotOrderByCompletedAtDesc(
                        session.getChild().getId(), SessionStatus.COMPLETED, sessionId);
        Map<ReportAxis, Integer> previousScores = averagePreviousScores(previousSessions);

        List<AxisScoreResponse> result = new ArrayList<>();
        for (ReportAxis axis : ReportAxis.values()) {
            AxisAccumulator raw = currentRaw.get(axis);
            boolean active = raw.required > 0;
            result.add(new AxisScoreResponse(
                    axis.name(),
                    axis.getLabel(),
                    axis.getDescription(),
                    active,
                    active ? AxisScoreCalculator.score(raw.hits, raw.required) : null,
                    active ? raw.hits : null,
                    active ? raw.required : null,
                    active ? previousScores.get(axis) : null,
                    active ? raw.evidence : null));
        }
        return result;
    }

    /**
     * 직전 완료 세션 최대 {@value PREVIOUS_SESSION_LIMIT}회의 축별 평균 점수.
     * 그 축이 활성이었던 회차가 하나도 없으면(예: 1회차) null.
     */
    private Map<ReportAxis, Integer> averagePreviousScores(List<StorySession> previousSessions) {
        Map<ReportAxis, List<Integer>> collected = new EnumMap<>(ReportAxis.class);
        for (ReportAxis axis : ReportAxis.values()) {
            collected.put(axis, new ArrayList<>());
        }

        for (StorySession previous : previousSessions) {
            Map<ReportAxis, AxisAccumulator> raw =
                    computeRaw(previous.getId(), previous.getStory().getId());
            for (Map.Entry<ReportAxis, AxisAccumulator> entry : raw.entrySet()) {
                AxisAccumulator a = entry.getValue();
                if (a.required > 0) {
                    collected.get(entry.getKey()).add(AxisScoreCalculator.score(a.hits, a.required));
                }
            }
        }

        Map<ReportAxis, Integer> averages = new EnumMap<>(ReportAxis.class);
        for (Map.Entry<ReportAxis, List<Integer>> entry : collected.entrySet()) {
            List<Integer> scores = entry.getValue();
            averages.put(entry.getKey(), scores.isEmpty() ? null : average(scores));
        }
        return averages;
    }

    private int average(List<Integer> scores) {
        int sum = 0;
        for (int score : scores) {
            sum += score;
        }
        return Math.round(sum / (float) scores.size());
    }

    /** 세션 1건의 축별 hits/required/evidence 집계. */
    private Map<ReportAxis, AxisAccumulator> computeRaw(UUID sessionId, UUID storyId) {
        Map<ReportAxis, AxisAccumulator> accumulators = new EnumMap<>(ReportAxis.class);
        for (ReportAxis axis : ReportAxis.values()) {
            accumulators.put(axis, new AxisAccumulator());
        }

        for (StoryScene scene : sceneRepository.findAllByStoryIdOrderBySceneOrderAsc(storyId)) {
            for (ThinkingElement element : scene.getRequiredElementTypes()) {
                ReportAxis axis = ReportAxis.of(element);
                if (axis != null) {
                    accumulators.get(axis).required++;
                }
            }
        }

        for (UtteranceAnalysis analysis : analysisRepository.findAllBySessionId(sessionId)) {
            // stt_low_confidence는 대표 발화(evidence) 후보에서만 제외한다 — hits 집계에는 영향을 주지 않는다.
            // (Message.sttLowConfidence 주석: "리포트 대표 발화 후보에서 제외한다")
            boolean eligibleForEvidence = !analysis.getMessage().isSttLowConfidence();

            for (DetectedElement detected : analysis.getDetectedElements()) {
                ReportAxis axis = ReportAxis.of(detected.type());
                if (axis == null) {
                    continue;
                }
                AxisAccumulator accumulator = accumulators.get(axis);
                accumulator.hits++;
                if (eligibleForEvidence && accumulator.evidence == null) {
                    accumulator.evidence = textOf(detected, analysis);
                }
            }
        }

        return accumulators;
    }

    /** 근거는 발화에서 잘라낸 조각이라 그대로 쓴다. 비어 있으면 발화 전체로 대신한다. */
    private String textOf(DetectedElement detected, UtteranceAnalysis analysis) {
        return (detected.evidence() == null || detected.evidence().isBlank())
                ? analysis.getMessage().getText()
                : detected.evidence();
    }

    private static final class AxisAccumulator {
        private int hits;
        private int required;
        private String evidence;
    }
}
