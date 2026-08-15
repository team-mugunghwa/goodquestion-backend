package com.mugunghwa.goodquestion.learning.report;

import com.mugunghwa.goodquestion.story.session.SessionCompletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 세션 완료 시 보호자 리포트 생성 (리포트-03).
 *
 * <p>별가루와 달리 커밋 뒤에 비동기로 받는다. LLM 호출이 수 초 걸리는데 아이가 완료
 * 화면에서 그만큼 기다릴 이유가 없고, 리포트를 못 만들었다고 완주와 별가루 지급까지
 * 롤백되면 안 된다. 리포트는 나중에 보호자가 여는 화면이라 몇 초 늦어도 된다.
 *
 * <p>생성 전에 열면 REPORT_NOT_READY(409)가 나간다.
 */
@Component
@RequiredArgsConstructor
public class ReportGenerationListener {

    private final ReportService reportService;

    @TransactionalEventListener
    public void generate(SessionCompletedEvent event) {
        reportService.generate(event.sessionId());
    }
}