package com.mugunghwa.goodquestion.story.content;

import com.mugunghwa.goodquestion.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 기동 시 적재한 사전 렌더 음성이 장면 렌더 경로에 그대로 이어지는지 확인한다.
 *
 * <p>캐시로 바꾸며 지키려는 것은 두 가지다. 시드에 있는 음성은 전과 똑같이 잡혀야 하고
 * (내레이션 5 + 고정 첫/마지막 대사 8), 해시가 어긋난 문장 - 대사를 고쳤거나 아이 이름이
 * 치환된 경우 - 은 전과 똑같이 걸러져야 한다. 걸러짐이 깨지면 화면의 새 문장에
 * 스피커의 옛 문장이 나가는데, 그걸 알아챌 사람이 없다.
 */
@IntegrationTest
class SceneAudioCacheTest {

    /** R__1_seed_content.sql의 방귀 뀌는 며느리. 장면 9개 전부에 공용 음성이 있다. */
    private static final UUID STORY_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Autowired
    private SceneAudioCache cache;

    @Autowired
    private SceneAudioResolver resolver;

    @Autowired
    private SceneService sceneService;

    @Autowired
    private StorySceneRepository sceneRepository;

    @Test
    void 시드의_공용_음성이_전부_적재된다() {
        List<StoryScene> scenes = sceneRepository.findAllByStoryIdOrderBySceneOrderAsc(STORY_ID);

        int total = scenes.stream().mapToInt(s -> cache.sharedAudioOf(s.getId()).size()).sum();

        // 내레이션 5(STORY 장면) + 고정 첫/마지막 대사 8(DIALOGUE 장면 4개 x 2)
        assertThat(total).isEqualTo(13);
    }

    @Test
    void 내레이션은_원문_그대로일_때만_잡히고_실측_시각이_실린다() {
        StoryScene first = sceneRepository.findByStoryIdAndSceneOrder(STORY_ID, (short) 1).orElseThrow();

        Optional<SceneAudioView> narration =
                resolver.narrationOf(first.getId(), first.getSceneDescription());

        assertThat(narration).isPresent();
        assertThat(narration.get().url()).isEqualTo("/tts/banggui/sc_banggui_01.mp3");
        assertThat(narration.get().sentenceTimings()).isNotEmpty();
    }

    @Test
    void 문장이_달라지면_해시가_어긋나_잡히지_않는다() {
        StoryScene dialogue = sceneRepository.findByStoryIdAndSceneOrder(STORY_ID, (short) 3).orElseThrow();

        // 렌더 원본과 같으면 잡힌다
        assertThat(resolver.forText(dialogue.getId(), dialogue.getCharacterOpening())).isPresent();
        // 이름 치환처럼 한 글자라도 다르면 걸러진다 - 클라이언트가 실시간 합성으로 간다
        assertThat(resolver.forText(dialogue.getId(), dialogue.getCharacterOpening() + " ")).isEmpty();
    }

    @Test
    void 장면_목록의_STORY_장면마다_내레이션_음성이_실린다() {
        var contents = sceneService.getScenes(STORY_ID);

        var storyScenes = contents.stream().filter(c -> c.sceneType() == SceneType.STORY).toList();
        assertThat(storyScenes).hasSize(5);
        assertThat(storyScenes).allSatisfy(scene -> {
            assertThat(scene.narrationAudioUrl()).isNotNull();
            assertThat(scene.narrationTimings()).isNotEmpty();
        });
        // DIALOGUE 장면은 내레이션이 없다
        assertThat(contents).filteredOn(c -> c.sceneType() == SceneType.DIALOGUE)
                .allSatisfy(scene -> assertThat(scene.narrationAudioUrl()).isNull());
    }

    @Test
    void 음성이_없는_장면은_빈_목록이_나간다() {
        assertThat(cache.sharedAudioOf(UUID.randomUUID())).isEmpty();
    }
}
