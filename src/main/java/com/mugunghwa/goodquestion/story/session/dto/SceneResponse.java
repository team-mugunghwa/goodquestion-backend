package com.mugunghwa.goodquestion.story.session.dto;

import com.mugunghwa.goodquestion.story.content.StoryScene;

import java.util.UUID;

public record SceneResponse(UUID sceneId, short sceneOrder, String sceneDescription,
                            String imageUrl, String characterName) {

    public static SceneResponse from(StoryScene scene) {
        return new SceneResponse(scene.getId(), scene.getSceneOrder(),
                scene.getSceneDescription(), scene.getImageUrl(), scene.getCharacterName());
    }
}
