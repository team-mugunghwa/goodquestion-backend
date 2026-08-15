package com.mugunghwa.goodquestion.helpdesk.guide;

import com.mugunghwa.goodquestion.helpdesk.ContentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GuideRepository extends JpaRepository<Guide, UUID> {

    List<Guide> findAllByStatusOrderByCategoryAscDisplayOrderAsc(ContentStatus status);

    List<Guide> findAllByStatusAndCategoryOrderByDisplayOrderAsc(ContentStatus status,
                                                                 GuideCategory category);

    Optional<Guide> findByIdAndStatus(UUID id, ContentStatus status);
}
