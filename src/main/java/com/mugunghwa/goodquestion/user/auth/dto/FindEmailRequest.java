package com.mugunghwa.goodquestion.user.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 이메일(ID) 찾기 요청.
 *
 * <p>{@code childName}과 {@code childBirthYear}가 둘 다 채워지면 자녀 정보로 좁혀서 찾는다.
 * 하나라도 비어 있으면 이름만으로 찾되, 자녀가 등록된 계정은 대상에서 제외한다.
 */
public record FindEmailRequest(
        @NotBlank String parentName,
        String childName,
        Short childBirthYear
) {
}
