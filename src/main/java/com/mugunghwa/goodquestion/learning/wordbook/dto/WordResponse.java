package com.mugunghwa.goodquestion.learning.wordbook.dto;

import com.mugunghwa.goodquestion.learning.wordbook.WordEntryType;
import com.mugunghwa.goodquestion.learning.wordbook.Wordbook;
import com.mugunghwa.goodquestion.story.content.Story;
import com.mugunghwa.goodquestion.story.content.StoryScene;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 명세 3-15 단어.
 *
 * <p>이야기 3필드는 단어장 화면이 단어를 <b>이야기별로 묶어</b> 보여주기 때문에 담는다.
 * 장면 조회는 {@code /api/stories/{storyId}/scenes} 뿐이라 {@code sourceSceneId} 만으로는
 * 클라이언트가 이야기를 되짚을 수 없다. 이름은 이야기를 참조하는 다른 DTO와 맞췄다
 * ({@code ShopItemResponse.UnlockGuide}, {@code ReportListResponse}).
 *
 * <p>장면 없이 저장된 단어는 세 값이 모두 null이다.
 */
public record WordResponse(UUID id, String word, String meaning, String exampleSentence,
                           WordEntryType entryType, UUID sourceSceneId,
                           UUID storyId, String storyTitle, String storyImageUrl,
                           OffsetDateTime createdAt) {

    public static WordResponse from(Wordbook w) {
        StoryScene scene = w.getSourceScene();
        Story story = (scene != null) ? scene.getStory() : null;
        return new WordResponse(w.getId(), w.getWord(), w.getMeaning(), w.getExampleSentence(),
                w.getEntryType(),
                (scene != null) ? scene.getId() : null,
                (story != null) ? story.getId() : null,
                (story != null) ? story.getTitle() : null,
                (story != null) ? story.getImageUrl() : null,
                w.getCreatedAt());
    }
}
