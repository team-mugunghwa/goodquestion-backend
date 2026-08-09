package com.mugunghwa.goodquestion.story.content;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TopicRepository extends JpaRepository<Topic, UUID> {

    List<Topic> findAllByOrderByDisplayOrderAsc();
}
