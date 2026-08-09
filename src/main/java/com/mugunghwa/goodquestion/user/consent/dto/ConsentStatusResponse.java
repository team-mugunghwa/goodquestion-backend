package com.mugunghwa.goodquestion.user.consent.dto;

import java.util.List;

/** current가 null이면 유효한 동의가 없어 새 세션을 시작할 수 없다(계정-10). */
public record ConsentStatusResponse(ConsentResponse current, List<ConsentResponse> history) {
}
