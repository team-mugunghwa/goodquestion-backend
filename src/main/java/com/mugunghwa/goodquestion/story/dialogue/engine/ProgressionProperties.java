package com.mugunghwa.goodquestion.story.dialogue.engine;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 진행 판단 임계값 (진행-17: 운영 설정으로 조정 가능하게 구현한다).
 * 기본값 2/2/2는 DB 문서에 명시된 MVP 기본값이다.
 *
 * @param stalledTurns        신규 요소 없는 연속 턴이 이 값 이상이면 유도 후보
 * @param lowInformationTurns 저정보 발화 연속 턴이 이 값 이상이면 유도 후보
 * @param remainingTurns      남은 대화 기회가 이 값 이하면 유도 후보 (대화 작동 규칙 2.2)
 */
@ConfigurationProperties(prefix = "progression.guidance")
public record ProgressionProperties(int stalledTurns, int lowInformationTurns, int remainingTurns) {

    public ProgressionProperties {
        if (stalledTurns <= 0) stalledTurns = 2;
        if (lowInformationTurns <= 0) lowInformationTurns = 2;
        if (remainingTurns <= 0) remainingTurns = 2;
    }
}
