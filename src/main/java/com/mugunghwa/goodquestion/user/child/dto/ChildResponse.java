package com.mugunghwa.goodquestion.user.child.dto;

import com.mugunghwa.goodquestion.user.child.Child;

import java.util.UUID;

public record ChildResponse(UUID id, String name, short birthYear, int age, boolean hasActiveConsent) {

    public static ChildResponse of(Child child, boolean hasActiveConsent) {
        return new ChildResponse(child.getId(), child.getName(), child.getBirthYear(),
                child.getAge(), hasActiveConsent);
    }
}
