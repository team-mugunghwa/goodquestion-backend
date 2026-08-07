package com.mugunghwa.goodquestion.story.topic.dto;

import com.mugunghwa.goodquestion.story.topic.Topic;

import java.util.UUID;

public record TopicResponse(UUID id, String name) {

    public static TopicResponse from(Topic topic) {
        return new TopicResponse(topic.getId(), topic.getName());
    }
}
