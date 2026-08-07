package com.mugunghwa.goodquestion.story.scene;

/** 장면 유형 — 콘텐츠 화면 흐름의 두 유형을 구분 */
public enum SceneType {
    STORY,      // 내레이션 장면 (도입·전개). 아이 발화 없음, 재생 완료 후 다음 장면 이동
    DIALOGUE    // 캐릭터 대화 장면. 발화 분석·진행 판단 파이프라인 대상
}
