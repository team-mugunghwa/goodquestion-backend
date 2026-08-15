package com.mugunghwa.goodquestion.helpdesk.inquiry;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InquiryAnswerRepository extends JpaRepository<InquiryAnswer, UUID> {

    Optional<InquiryAnswer> findByInquiryId(UUID inquiryId);

    /** 목록에서 문의마다 답변을 따로 묻지 않도록 한 번에 가져온다. */
    List<InquiryAnswer> findAllByInquiryIdIn(List<UUID> inquiryIds);
}
