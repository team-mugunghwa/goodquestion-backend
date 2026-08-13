package com.mugunghwa.goodquestion.story.dialogue;

/**
 * 분석 LLM을 부르기 전에 DB에서 떠 두는 값.
 *
 * @param previousCharacterText 직전 캐릭터 대사. 분석 LLM이 "무엇에 대한 대답인지" 알아야 한다
 */
record TurnPreparation(SceneAnalysisContext analysisContext, String previousCharacterText) {
}
