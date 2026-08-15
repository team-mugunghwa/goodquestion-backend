package com.mugunghwa.goodquestion.helpdesk.inquiry;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InquiryRepository extends JpaRepository<Inquiry, UUID> {

    /** 내 문의 목록. 사용자 한 명의 문의는 많아야 수십 건이라 페이징하지 않는다. */
    List<Inquiry> findAllByParentIdOrderByCreatedAtDesc(UUID parentId);
}
