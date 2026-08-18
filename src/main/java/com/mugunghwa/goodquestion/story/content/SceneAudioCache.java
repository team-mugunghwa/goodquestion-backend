package com.mugunghwa.goodquestion.story.content;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 공용 사전 렌더 음성 전량을 기동 시 한 번 읽어 들고 있는다.
 *
 * <p>scene_audio는 Flyway 시드로만 바뀌는 읽기 전용 데이터고(SceneAudio 주석), 공용 행
 * 전량이 수십 건이다. 그런데 장면을 렌더할 때마다 조회하니 장면 목록 응답 한 번에
 * STORY 장면 수만큼 쿼리가 나갔다. 전량을 한 번 적재하면 그 뒤로는 맵 조회다.
 *
 * <p>축출형 캐시(Caffeine 등)를 쓰지 않는 이유: 축출과 TTL은 "전부 들 수 없거나 낡는"
 * 데이터의 도구다. 이 데이터는 전부 들 수 있고, 재배포 전에는 낡지 않으며, 재배포가
 * 곧 재적재다. 관리자 콘솔 같은 다른 프로세스가 이 테이블을 쓰게 되면 이 전제가
 * 깨지므로 그때는 TTL 있는 캐시로 바꿔야 한다.
 *
 * <p>적재 전(기동 직후의 짧은 구간)에는 빈 결과가 나간다. 사전 렌더가 없는 장면과 같은
 * 경로라 클라이언트는 평소처럼 /api/tts로 합성한다 - 틀린 답이 아니라 느린 답이다.
 *
 * <p>아이별 음성(child_id 있는 행)은 여기 담지 않는다. 지금은 만드는 곳이 없고, 생기면
 * 아이 수에 비례해 자라 전량 적재 전제가 깨지므로 별도 경로여야 한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SceneAudioCache {

    private final SceneAudioRepository repository;

    private volatile Map<UUID, List<SceneAudioView>> byScene = Map.of();

    @EventListener(ApplicationReadyEvent.class)
    @Transactional(readOnly = true)
    public void load() {
        byScene = repository.findAllByChildIdIsNull().stream()
                .collect(Collectors.collectingAndThen(
                        Collectors.groupingBy(audio -> audio.getScene().getId(),
                                Collectors.mapping(SceneAudioView::from, Collectors.toUnmodifiableList())),
                        Map::copyOf));
        log.info("공용 사전 렌더 음성 {}건 적재 (장면 {}개)",
                byScene.values().stream().mapToInt(List::size).sum(), byScene.size());
    }

    /** 장면의 공용 음성 전부(최대 3건 - 내레이션/첫 대사/마지막 대사). 없으면 빈 목록. */
    public List<SceneAudioView> sharedAudioOf(UUID sceneId) {
        return byScene.getOrDefault(sceneId, List.of());
    }
}
