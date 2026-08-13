package com.mugunghwa.goodquestion.story.session;

/**
 * 장면 종료 후 이동 대상.
 * COMPLETED는 후속 활동 config가 없는 이야기가 마지막 장면에서 곧바로 완료된 경우다.
 */
public enum SceneTransitionTarget { SCENE, POST_ACTIVITY, COMPLETED }
