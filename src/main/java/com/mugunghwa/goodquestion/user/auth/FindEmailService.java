package com.mugunghwa.goodquestion.user.auth;

import com.mugunghwa.goodquestion.user.auth.dto.FindEmailRequest;
import com.mugunghwa.goodquestion.user.auth.dto.FindEmailResponse;
import com.mugunghwa.goodquestion.user.child.Child;
import com.mugunghwa.goodquestion.user.child.ChildRepository;
import com.mugunghwa.goodquestion.user.parent.Parent;
import com.mugunghwa.goodquestion.user.parent.ParentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 이메일(ID) 찾기 (계정-07).
 *
 * <p>비밀번호 찾기와 달리 계정 존재 여부를 완전히 숨길 수 없다 — 목적 자체가 이메일을
 * 알려주는 것이다. 대신 본인 확인 강도를 입력 정보로 가른다.
 *
 * <p>자녀 이름과 출생연도까지 맞히면 그 자녀로 좁혀서 찾는다. 그게 없으면 이름만으로
 * 찾되, <b>자녀가 하나라도 등록된 계정은 이 경로에서 절대 나오지 않는다</b> — 아이가
 * 있는 계정은 아이 정보 없이는 못 찾게 막는 것이 핵심 보안 조건이다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FindEmailService {

    private final ParentRepository parentRepository;
    private final ChildRepository childRepository;

    public FindEmailResponse find(FindEmailRequest request) {
        List<Parent> matched = hasCompleteChildInfo(request)
                ? findByChild(request)
                : findByNameOnly(request);

        List<String> emails = matched.stream()
                .map(Parent::getEmail)
                .filter(email -> email != null && !email.isBlank())
                .distinct()
                .map(FindEmailService::mask)
                .toList();
        return new FindEmailResponse(emails);
    }

    private boolean hasCompleteChildInfo(FindEmailRequest request) {
        return request.childName() != null && !request.childName().isBlank()
                && request.childBirthYear() != null;
    }

    private List<Parent> findByChild(FindEmailRequest request) {
        List<Child> children = childRepository.findByParent_NameAndNameAndBirthYear(
                request.parentName(), request.childName(), request.childBirthYear());

        // Child 여러 명이 같은 부모를 가리킬 수 있어(형제) id 기준으로 정리한다.
        Map<UUID, Parent> byParentId = new LinkedHashMap<>();
        for (Child child : children) {
            Parent parent = child.getParent();
            byParentId.putIfAbsent(parent.getId(), parent);
        }
        return List.copyOf(byParentId.values());
    }

    private List<Parent> findByNameOnly(FindEmailRequest request) {
        return parentRepository.findByNameAndNoChildren(request.parentName());
    }

    /** 로컬 파트 앞 2글자(2글자 이하면 1글자)만 남기고 나머지는 ***로 가린다. 도메인은 그대로. */
    private static String mask(String email) {
        int at = email.indexOf('@');
        if (at < 0) {
            return email;
        }
        String local = email.substring(0, at);
        String domain = email.substring(at);
        int visible = local.length() <= 2 ? 1 : 2;
        return local.substring(0, Math.min(visible, local.length())) + "***" + domain;
    }
}
