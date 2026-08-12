package com.mugunghwa.goodquestion.user.child;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ChildRepository extends JpaRepository<Child, UUID> {

    List<Child> findAllByParentId(UUID parentId);

    /** 이메일 찾기(계정-07) — 보호자 이름 + 아이 이름 + 출생연도가 모두 일치하는 아이. */
    List<Child> findByParent_NameAndNameAndBirthYear(String parentName, String childName, short birthYear);
}
