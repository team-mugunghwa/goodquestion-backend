package com.mugunghwa.goodquestion.learning.activity.dto;

/** 마이페이지 활동 요약. 완주 편수와 별가루 잔액을 한 번에 준다. */
public record ChildActivityResponse(long completedStories, int stardust) {}