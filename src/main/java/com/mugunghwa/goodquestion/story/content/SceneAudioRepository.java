package com.mugunghwa.goodquestion.story.content;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SceneAudioRepository extends JpaRepository<SceneAudio, UUID> {

    /** 공용 음성 전량 - 기동 시 SceneAudioCache 적재용. 장면 단위 조회는 캐시가 대신한다. */
    List<SceneAudio> findAllByChildIdIsNull();
}
