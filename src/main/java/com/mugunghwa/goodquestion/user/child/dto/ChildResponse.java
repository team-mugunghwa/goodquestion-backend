package com.mugunghwa.goodquestion.user.child.dto;

import com.mugunghwa.goodquestion.user.child.Child;
import com.mugunghwa.goodquestion.user.consent.ConsentStatus;

import java.util.UUID;

/** 명세 3-3 아이 요약. age는 저장하지 않고 현재연도 − 출생연도로 계산한다(계정-07). */
public record ChildResponse(UUID id, String name, short birthYear, int age,
                            ConsentStatus consentStatus) {

    public static ChildResponse of(Child child, ConsentStatus consentStatus) {
        return new ChildResponse(child.getId(), child.getName(), child.getBirthYear(),
                child.getAge(), consentStatus);
    }
}
