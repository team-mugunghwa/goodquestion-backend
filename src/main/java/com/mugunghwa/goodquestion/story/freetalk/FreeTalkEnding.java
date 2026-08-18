package com.mugunghwa.goodquestion.story.freetalk;

import com.mugunghwa.goodquestion.ai.freetalk.FreeTalkLlmClient;

/**
 * 아이가 먼저 그만둘 때의 준비 결과.
 *
 * @param stored 이미 닫힌 대화면 남아 있는 마지막 대사. 값이 있으면 LLM을 부르지 않는다 -
 *               턴 상한으로 이미 인사를 마친 대화에 "그만하기"가 한 번 더 들어오는 것은
 *               흔한 일이고, 그때마다 새 작별 대사를 만들면 요금만 두 배가 된다
 */
public record FreeTalkEnding(String characterName,
                             FreeTalkLlmClient.FreeTalkLlmInput llmInput,
                             FreeTalkLine stored) {

    public boolean alreadyEnded() {
        return stored != null;
    }
}
