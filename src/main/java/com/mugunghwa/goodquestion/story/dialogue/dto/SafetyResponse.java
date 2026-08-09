package com.mugunghwa.goodquestion.story.dialogue.dto;

import java.util.List;

/**
 * 위험 신호 개입 결과 — 값이 있으면 캐릭터 대사가 안전 문구로 대체된 턴이다.
 *
 * <p>자해·가정 폭력·학대 정황이 감지되면 대사 생성을 중단하고 사전 정의된 안전 문구를 내보내며,
 * 해당 세션에 확인 필요 표시를 남긴다(story_sessions.safety_flagged).
 * categories에는 범주만 담고 아이 발화 원문은 담지 않는다.
 *
 * <p>recoveryAvailable이 true면 오탐 복귀 버튼을 노출한다.
 * TODO: 실제 감지는 AI 파이프라인 연동 시 채운다. 지금은 응답 계약 자리만 잡아둔 상태다.
 */
public record SafetyResponse(
        List<String> categories,
        boolean recoveryAvailable
) {
}
