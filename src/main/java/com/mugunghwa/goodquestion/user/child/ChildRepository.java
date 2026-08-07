package com.mugunghwa.goodquestion.user.child;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ChildRepository extends JpaRepository<Child, UUID> {

    List<Child> findAllByParentId(UUID parentId);
}
