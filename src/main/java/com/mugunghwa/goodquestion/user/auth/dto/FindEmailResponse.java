package com.mugunghwa.goodquestion.user.auth.dto;

import java.util.List;

/** 매치가 없으면 빈 리스트 — 존재 여부를 에러로 구분하지 않는다. */
public record FindEmailResponse(List<String> emails) {
}
