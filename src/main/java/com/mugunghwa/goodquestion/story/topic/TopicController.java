package com.mugunghwa.goodquestion.story.topic;

import com.mugunghwa.goodquestion.story.topic.dto.TopicResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/topics")
@RequiredArgsConstructor
public class TopicController {

    private final TopicRepository topicRepository;

    @GetMapping
    public List<TopicResponse> getTopics() {
        return topicRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(TopicResponse::from).toList();
    }
}
