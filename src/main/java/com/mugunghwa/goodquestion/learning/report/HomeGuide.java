package com.mugunghwa.goodquestion.learning.report;

import java.util.List;

/**
 * 가정 연계 대화 가이드 (리포트 요건 6절).
 *
 * <p>아이에게 내주는 과제가 아니라 보호자가 이어갈 대화의 실마리다. 이번 회차의
 * 강점과 보완점에 따라 질문 유형이 달라진다(7절).
 */
public record HomeGuide(List<String> storyQuestions,
                        List<String> dailyLifeQuestions) {
}