package com.mugunghwa.goodquestion.learning.report;

import com.mugunghwa.goodquestion.global.error.BusinessException;
import com.mugunghwa.goodquestion.global.error.ErrorCode;
import com.mugunghwa.goodquestion.global.vocab.ThinkingElement;
import com.mugunghwa.goodquestion.story.dialogue.DetectedElement;
import com.mugunghwa.goodquestion.story.dialogue.UtteranceAnalysis;
import com.mugunghwa.goodquestion.story.dialogue.UtteranceAnalysisRepository;
import com.mugunghwa.goodquestion.ai.report.ReportLlmClient;
import com.mugunghwa.goodquestion.learning.report.dto.ReportDetailResponse;
import com.mugunghwa.goodquestion.learning.report.dto.ReportListResponse;
import com.mugunghwa.goodquestion.story.session.*;
import com.mugunghwa.goodquestion.user.child.Child;
import com.mugunghwa.goodquestion.user.child.ChildService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final ReportRepository reportRepository;
    private final UtteranceAnalysisRepository analysisRepository;
    private final ReportLlmClient reportLlmClient;
    private final SessionService sessionService;
    private final ChildService childService;
    private final StorySessionRepository sessionRepository;
    private final MessageRepository messageRepository;
    /**
     * 세션 완료 시 후속 활동에서 호출 (비동기).
     *
     * <p>LLM 호출이 수 초 걸리므로 세션 완료 응답을 붙잡지 않는다. 생성 전에 리포트를
     * 열면 REPORT_NOT_READY(409)가 나가고, 보호자 화면은 잠시 뒤 다시 보라고 안내한다.
     */
    @Async
    @Transactional
    public void generate(UUID sessionId) {
        StorySession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "세션을 찾을 수 없습니다."));
        if (reportRepository.findBySessionId(sessionId).isPresent()) {
            return;
        }

        ReportLlmClient.ReportLlmResult result = reportLlmClient.generate(toLlmInput(session));

        reportRepository.save(Report.builder()
                .session(session)
                .summary(result.summary())
                .strengths(toItems(result.strengths()))
                .nextFocus(toItems(result.nextFocus()))
                .analysis(new ReportAnalysis(
                        new VocabularyAnalysis(
                                result.vocabulary().mainWords(),
                                result.vocabulary().askedWords(),
                                result.vocabulary().repeatedExpressions(),
                                result.vocabulary().feedback()),
                        result.competencies().stream()
                                .map(c -> new Competency(c.name(), c.finding(),
                                        c.evidenceUtterance(), c.strength(), c.nextFocus()))
                                .toList(),
                        new RepresentativeUtterance(
                                result.representativeUtterance().text(),
                                result.representativeUtterance().reason()),
                        new HomeGuide(
                                result.homeGuide().storyQuestions(),
                                result.homeGuide().dailyLifeQuestions())))
                .build());
    }

    /**
     * 대화 기록을 LLM 입력으로 옮긴다.
     *
     * <p>인식이 미덥지 않았던 아이 발화는 빼고 넣는다. 저장된 원문이 실제로 한 말과 다를
     * 수 있는데, 리포트는 보호자에게 "아이가 이렇게 말했다"고 보여주는 자리다.
     */
    private ReportLlmClient.ReportLlmInput toLlmInput(StorySession session) {
        Map<UUID, List<String>> elementsByMessage = new LinkedHashMap<>();
        for (UtteranceAnalysis analysis : analysisRepository.findAllBySessionId(session.getId())) {
            elementsByMessage.put(analysis.getMessage().getId(),
                    analysis.getDetectedElements().stream()
                            .map(detected -> detected.type().name())
                            .toList());
        }

        List<ReportLlmClient.TurnDigest> turns = new ArrayList<>();
        String pendingCharacterText = null;
        String pendingCharacterName = null;

        for (Message message : messageRepository.findAllBySessionIdOrderByTurnOrderAsc(session.getId())) {
            if (message.getSpeakerType() == SpeakerType.CHARACTER) {
                pendingCharacterText = message.getText();
                pendingCharacterName = message.getScene().getCharacterName();
                continue;
            }
            if (message.getSpeakerType() != SpeakerType.CHILD || message.isSttLowConfidence()) {
                continue;
            }
            turns.add(new ReportLlmClient.TurnDigest(
                    message.getScene().getSceneOrder(),
                    pendingCharacterName,
                    pendingCharacterText,
                    message.getText(),
                    elementsByMessage.getOrDefault(message.getId(), List.of())));
        }

        Child child = session.getChild();
        return new ReportLlmClient.ReportLlmInput(
                session.getStory().getTitle(), child.getName(), child.getAge(), turns);
    }

    /** LLM은 요소를 문자열로 준다. 알 수 없는 값이면 그 항목을 버린다. */
    private List<ReportItem> toItems(List<ReportLlmClient.ItemDto> dtos) {
        List<ReportItem> items = new ArrayList<>();
        for (ReportLlmClient.ItemDto dto : dtos) {
            try {
                items.add(new ReportItem(ThinkingElement.valueOf(dto.element()), dto.comment()));
            } catch (IllegalArgumentException | NullPointerException ignored) {
                // 스키마 enum으로 막고 있어 사실상 오지 않지만, 한 항목 때문에 리포트 전체를
                // 잃지 않도록 건너뛴다.
            }
        }
        return items;
    }

    public List<ReportListResponse> getReports(UUID parentId, UUID childId) {
        childService.getOwnedChild(parentId, childId);

        return reportRepository.findAllByChildId(childId).stream()
                .map(report -> new ReportListResponse(
                        report.getId(),
                        report.getSession().getId(),
                        report.getSession().getStory().getTitle(),
                        report.getCreatedAt()))
                .toList();
    }

    /**
     * 세션 리포트 조회. 리포트는 후속 활동 완료 후 비동기로 생성되므로 아직 없을 수 있다.
     *
     * <p>없는 것을 404로 돌려주면 "세션이 없다"와 구분되지 않는다. 보호자 화면은 둘을
     * 다르게 안내해야 한다 — 하나는 잘못된 접근이고 하나는 잠시 뒤 다시 보면 되는 상태다.
     */
    public ReportDetailResponse getReport(UUID parentId, UUID sessionId) {
        StorySession session = sessionService.getOwnedSession(parentId, sessionId);
        Report report = reportRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REPORT_NOT_READY));

        return new ReportDetailResponse(
                report.getId(),
                sessionId,
                session.getStory().getTitle(),
                report.getSummary(),
                report.getStrengths(),
                report.getNextFocus(),
                report.getAnalysis().vocabulary(),
                report.getAnalysis().competencies(),
                report.getAnalysis().representativeUtterance(),
                report.getAnalysis().homeGuide(),
                elementEvidences(sessionId),
                report.getCreatedAt());
    }

    /**
     * 대표 발화 구성 — 저장하지 않고 분석 근거에서 매번 만든다(리포트-04).
     *
     * <p>요소당 가장 이른 턴 하나만 남긴다. 같은 요소가 여러 턴에서 나오면 화면에 비슷한
     * 문장이 반복되고, 무엇을 잘했는지가 오히려 흐려진다.
     *
     * <p>인식이 미덥지 않았던 발화는 후보에서 뺀다. 저장된 원문이 아이가 실제로 한 말과
     * 다를 수 있는데, 리포트는 보호자에게 "아이가 이렇게 말했다"고 보여주는 자리다.
     */
        private List<ReportDetailResponse.ElementEvidence> elementEvidences(UUID sessionId) {
            Map<ThinkingElement, ReportDetailResponse.ElementEvidence> byElement = new LinkedHashMap<>();

            for (UtteranceAnalysis analysis : analysisRepository.findAllBySessionId(sessionId)) {
                if (analysis.getMessage().isSttLowConfidence()) {
                    continue;
                }
                for (DetectedElement detected : analysis.getDetectedElements()) {
                    byElement.putIfAbsent(detected.type(),
                            new ReportDetailResponse.ElementEvidence(
                                    textOf(detected, analysis), detected.type()));
                }
            }

            return List.copyOf(byElement.values());
        }

    /** 근거는 발화에서 잘라낸 조각이라 그대로 쓴다. 비어 있으면 발화 전체로 대신한다. */
    private String textOf(DetectedElement detected, UtteranceAnalysis analysis) {
        return (detected.evidence() == null || detected.evidence().isBlank())
                ? analysis.getMessage().getText()
                : detected.evidence();
    }

    /** API로 즉시 생성. 소유권을 확인하고 동기로 만든다. */
    @Transactional
    public void generateNow(UUID parentId, UUID sessionId) {
        sessionService.getOwnedSession(parentId, sessionId);
        generate(sessionId);
    }
}
