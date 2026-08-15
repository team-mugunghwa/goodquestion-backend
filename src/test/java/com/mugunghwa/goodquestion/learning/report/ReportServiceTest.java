package com.mugunghwa.goodquestion.learning.report;

import com.mugunghwa.goodquestion.global.error.BusinessException;
import com.mugunghwa.goodquestion.global.error.ErrorCode;
import com.mugunghwa.goodquestion.global.vocab.ThinkingElement;
import com.mugunghwa.goodquestion.learning.report.dto.ReportDetailResponse;
import com.mugunghwa.goodquestion.learning.report.dto.ReportListResponse;
import com.mugunghwa.goodquestion.story.session.StorySession;
import com.mugunghwa.goodquestion.story.session.StorySessionRepository;
import com.mugunghwa.goodquestion.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@IntegrationTest
@Transactional
class ReportServiceTest {

    /**
     * Flyway가 적용하는 R__2_seed_demo_data.sql의 데모 데이터를 전제한다.
     * 보호자 "김보호"의 아이 "지우"에게 완주 세션 A(리포트 있음)와 진행 중 세션 B(리포트 없음)가 있다.
     */
    private static final UUID PARENT_ID = UUID.fromString("99999999-9999-9999-9999-000000000001");
    private static final UUID OTHER_PARENT_ID = UUID.fromString("99999999-9999-9999-9999-000000000002");
    private static final UUID CHILD_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-000000000001");
    private static final UUID COMPLETED_SESSION_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-000000000001");
    private static final UUID IN_PROGRESS_SESSION_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-000000000002");

    @Autowired
    private ReportService reportService;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private StorySessionRepository sessionRepository;

    @Test
    void 아이의_리포트_목록을_돌려준다() {
        List<ReportListResponse> reports = reportService.getReports(PARENT_ID, CHILD_ID);

        assertThat(reports).isNotEmpty();
        assertThat(reports.getFirst().sessionId()).isEqualTo(COMPLETED_SESSION_ID);
        assertThat(reports.getFirst().storyTitle()).isNotBlank();
    }

    @Test
    void 남의_아이_리포트는_조회할_수_없다() {
        assertThatThrownBy(() -> reportService.getReports(OTHER_PARENT_ID, CHILD_ID))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
    }

    @Test
    void 리포트_상세는_요약과_강점을_돌려준다() {
        ReportDetailResponse report = reportService.getReport(PARENT_ID, COMPLETED_SESSION_ID);

        assertThat(report.sessionId()).isEqualTo(COMPLETED_SESSION_ID);
        assertThat(report.summary()).isNotBlank();
        assertThat(report.strengths()).isNotEmpty();
        assertThat(report.nextFocus()).isNotEmpty();
    }

    @Test
    void 요소별_근거_발화는_분석에서_구성한다() {
        ReportDetailResponse report = reportService.getReport(PARENT_ID, COMPLETED_SESSION_ID);

        assertThat(report.elementEvidences()).isNotEmpty();
        assertThat(report.elementEvidences())
                .allSatisfy(evidence -> {
                    assertThat(evidence.text()).isNotBlank();
                    assertThat(evidence.element()).isNotNull();
                });
    }

    @Test
    void 같은_요소는_가장_이른_턴_하나만_남는다() {
        // 세션 A의 분석에는 PERSPECTIVE가 두 턴에 걸쳐 들어 있다.
        ReportDetailResponse report = reportService.getReport(PARENT_ID, COMPLETED_SESSION_ID);

        List<ThinkingElement> elements = report.elementEvidences().stream()
                .map(ReportDetailResponse.ElementEvidence::element)
                .toList();

        assertThat(elements).doesNotHaveDuplicates();
        assertThat(report.elementEvidences().stream()
                .filter(e -> e.element() == ThinkingElement.PERSPECTIVE)
                .findFirst())
                .isPresent()
                .hasValueSatisfying(e -> assertThat(e.text()).isEqualTo("가족이니까 이해해 줄 거예요"));
    }

    @Test
    void 인식이_미덥지_않은_발화는_근거에서_빠진다() {
        // 세션 B의 유일한 분석은 stt_low_confidence=true인 발화에 붙어 있다.
        StorySession session = sessionRepository.findById(IN_PROGRESS_SESSION_ID).orElseThrow();
        reportRepository.save(Report.builder()
                .session(session)
                .summary("확인용 리포트")
                .strengths(List.of(new ReportItem(ThinkingElement.PERSPECTIVE, "확인용")))
                .nextFocus(List.of())
                .build());

        ReportDetailResponse report = reportService.getReport(PARENT_ID, IN_PROGRESS_SESSION_ID);

        assertThat(report.elementEvidences()).isEmpty();
    }

    @Test
    void 리포트가_아직_없으면_409로_알린다() {
        assertThatThrownBy(() -> reportService.getReport(PARENT_ID, IN_PROGRESS_SESSION_ID))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REPORT_NOT_READY);
    }

    @Test
    void 남의_세션_리포트는_조회할_수_없다() {
        assertThatThrownBy(() -> reportService.getReport(OTHER_PARENT_ID, COMPLETED_SESSION_ID))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
    }
}
