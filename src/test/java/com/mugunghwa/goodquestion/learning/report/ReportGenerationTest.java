package com.mugunghwa.goodquestion.learning.report;

import com.mugunghwa.goodquestion.ai.report.ReportLlmClient;
import com.mugunghwa.goodquestion.learning.report.dto.ReportDetailResponse;
import com.mugunghwa.goodquestion.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

/**
 * 리포트 생성. LLM은 대역으로 둔다 — 실제 호출은 비용이 들고 응답이 매번 달라져
 * "우리가 결과를 제대로 저장하고 내려주는가"를 검증할 수 없다.
 */
@IntegrationTest
@Transactional
class ReportGenerationTest {

    private static final UUID PARENT_ID = UUID.fromString("99999999-9999-9999-9999-000000000001");
    private static final UUID IN_PROGRESS_SESSION_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-000000000002");

    @Autowired
    private ReportService reportService;

    @MockitoBean
    private ReportLlmClient reportLlmClient;

    @BeforeEach
    void 대역_응답을_준비한다() {
        given(reportLlmClient.generate(any())).willReturn(new ReportLlmClient.ReportLlmResult(
                "이야기에 즐겁게 참여했습니다.",
                List.of(new ReportLlmClient.ItemDto("PERSPECTIVE", "상대 입장을 헤아렸어요")),
                List.of(new ReportLlmClient.ItemDto("REASON", "까닭을 덧붙이면 좋겠어요")),
                new ReportLlmClient.VocabularyDto(
                        List.of("방귀", "며느리"), List.of(), List.of("그래서"),
                        "다양한 낱말을 써 보면 좋겠어요"),
                List.of(new ReportLlmClient.CompetencyDto(
                        "관점과 공감", "상대 입장을 살폈어요", "가족이니까 이해해 줄 거예요",
                        "다른 사람 마음을 헤아렸어요", "왜 그렇게 생각했는지도 말해 보면 좋겠어요")),
                new ReportLlmClient.RepresentativeUtteranceDto(
                        "가족이니까 이해해 줄 거예요", "상대 입장과 자기 판단이 자연스럽게 이어졌습니다"),
                new ReportLlmClient.HomeGuideDto(
                        List.of("며느리는 어떤 기분이었을까?"),
                        List.of("너도 창피했던 적 있어?")),
                "gpt-5-mini"));
    }

    @Test
    void 생성한_리포트를_조회하면_분석_본문이_들어_있다() {
        reportService.generateNow(PARENT_ID, IN_PROGRESS_SESSION_ID);

        ReportDetailResponse report = reportService.getReport(PARENT_ID, IN_PROGRESS_SESSION_ID);

        assertThat(report.summary()).isNotBlank();
        assertThat(report.strengths()).isNotEmpty();
        assertThat(report.vocabulary().mainWords()).contains("방귀");
        assertThat(report.competencies()).hasSize(1);
        assertThat(report.representativeUtterance().reason()).isNotBlank();
        assertThat(report.homeGuide().storyQuestions()).isNotEmpty();
    }

    /** 완주할 때마다 새로 만들면 보호자가 보던 리포트가 바뀐다. */
    @Test
    void 이미_리포트가_있으면_다시_만들지_않는다() {
        reportService.generateNow(PARENT_ID, IN_PROGRESS_SESSION_ID);
        UUID first = reportService.getReport(PARENT_ID, IN_PROGRESS_SESSION_ID).id();

        reportService.generateNow(PARENT_ID, IN_PROGRESS_SESSION_ID);

        assertThat(reportService.getReport(PARENT_ID, IN_PROGRESS_SESSION_ID).id()).isEqualTo(first);
    }

    /** 알 수 없는 요소 하나 때문에 리포트 전체를 잃지 않는다. */
    @Test
    void 알_수_없는_사고_요소는_건너뛴다() {
        given(reportLlmClient.generate(any())).willReturn(new ReportLlmClient.ReportLlmResult(
                "요약", List.of(new ReportLlmClient.ItemDto("NOT_AN_ELEMENT", "무시된다")),
                List.of(), new ReportLlmClient.VocabularyDto(List.of(), List.of(), List.of(), "-"),
                List.of(), new ReportLlmClient.RepresentativeUtteranceDto("-", "-"),
                new ReportLlmClient.HomeGuideDto(List.of(), List.of()), "gpt-5-mini"));

        reportService.generateNow(PARENT_ID, IN_PROGRESS_SESSION_ID);

        assertThat(reportService.getReport(PARENT_ID, IN_PROGRESS_SESSION_ID).strengths()).isEmpty();
    }
}