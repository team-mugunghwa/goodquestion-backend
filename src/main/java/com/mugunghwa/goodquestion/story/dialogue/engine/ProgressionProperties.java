package com.mugunghwa.goodquestion.story.dialogue.engine;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 진행 판단 임계값 (진행-17: 운영 설정으로 조정 가능하게 구현한다).
 * 기본값 2/2/2는 DB 문서에 명시된 MVP 기본값이다.
 *
 * @param stalledTurns        신규 요소 없는 연속 턴이 이 값 이상이면 유도 후보
 * @param lowInformationTurns 저정보 발화 연속 턴이 이 값 이상이면 유도 후보
 * @param remainingTurns      남은 대화 기회가 이 값 이하면 유도 후보 (대화 작동 규칙 2.2)
 * @param alwaysGuide         켜면 필수 요소가 남아 있는 한 매 턴 유도한다(첫 발화 제외).
 *                            성인 테스트와 팀 시연에서 "유도가 안 걸린다"는 지적이 반복돼
 *                            2026-08-17 기본값을 켜짐으로 뒀다. 끄면 위 세 임계값으로
 *                            돌아간다 - 문서의 원 규칙(진행-09/10)이 그쪽이다.
 */
@ConfigurationProperties(prefix = "progression.guidance")
public record ProgressionProperties(int stalledTurns, int lowInformationTurns, int remainingTurns,
                                    Boolean alwaysGuide) {

    public ProgressionProperties {
        if (stalledTurns <= 0) stalledTurns = 2;
        if (lowInformationTurns <= 0) lowInformationTurns = 2;
        if (remainingTurns <= 0) remainingTurns = 2;
        if (alwaysGuide == null) alwaysGuide = true;
    }

    /** 세 임계값만 주는 기존 호출부(테스트 포함)를 위한 편의 생성자 - 강한 유도 기본. */
    public ProgressionProperties(int stalledTurns, int lowInformationTurns, int remainingTurns) {
        this(stalledTurns, lowInformationTurns, remainingTurns, true);
    }

    public boolean guidesAlways() {
        return Boolean.TRUE.equals(alwaysGuide);
    }
}
