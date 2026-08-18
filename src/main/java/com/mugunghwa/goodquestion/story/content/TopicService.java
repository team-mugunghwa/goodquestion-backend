package com.mugunghwa.goodquestion.story.content;

import com.mugunghwa.goodquestion.global.config.CacheConfig;
import com.mugunghwa.goodquestion.story.content.dto.TopicResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 토픽 마스터 조회. 목록 API와 이야기 목록의 필터 칩이 같은 목록을 쓴다.
 *
 * <p>topics는 시드로만 바뀌는 수 개짜리 마스터인데, 이야기 목록을 열 때마다 전체를
 * 다시 읽고 있었다. 첫 조회만 DB를 타고 그 뒤로는 캐시에서 낸다(무효화가 필요 없는
 * 근거는 CacheConfig 참고).
 *
 * <p>캐시에는 엔티티가 아니라 응답 DTO를 담는다. 엔티티를 담으면 영속성 컨텍스트
 * 밖에서 여러 요청이 같은 인스턴스를 공유하게 된다.
 */
@Service
@RequiredArgsConstructor
public class TopicService {

    private final TopicRepository topicRepository;

    @Cacheable(CacheConfig.TOPICS)
    public List<TopicResponse> getTopics() {
        return topicRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(TopicResponse::from).toList();
    }
}
