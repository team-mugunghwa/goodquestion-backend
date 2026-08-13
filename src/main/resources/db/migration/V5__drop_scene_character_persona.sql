-- story_scenes.character_persona 제거
--
-- 캐릭터 LLM 입력이 characters.personality + story_scenes.scene_stance로 옮겨져
-- 내용이 완전히 겹치는 레거시 컬럼만 남았다. 같은 문장을 두 곳에서 고치다
-- 어긋나는 사고를 막기 위해 컬럼을 없앤다.
-- (CharacterResponseService.characterContext가 characters 참조를 읽도록 함께 바뀐다)

alter table story_scenes drop column character_persona;
