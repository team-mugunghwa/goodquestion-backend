package com.mugunghwa.goodquestion.learning.wordbook;

import com.mugunghwa.goodquestion.global.error.BusinessException;
import com.mugunghwa.goodquestion.global.error.ErrorCode;
import com.mugunghwa.goodquestion.ai.word.WordMeaningLlmClient;
import com.mugunghwa.goodquestion.learning.wordbook.dto.WordCreateRequest;
import com.mugunghwa.goodquestion.learning.wordbook.dto.WordResponse;
import com.mugunghwa.goodquestion.story.content.StoryScene;
import com.mugunghwa.goodquestion.story.content.StorySceneRepository;
import com.mugunghwa.goodquestion.user.child.Child;
import com.mugunghwa.goodquestion.user.child.ChildService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WordbookService {

    private final WordbookRepository wordbookRepository;
    private final StoryVocabularyRepository storyVocabularyRepository;
    private final WordLemmatizer lemmatizer;
    private final WordMeaningLlmClient wordMeaningLlmClient;
    private final ChildService childService;
    private final StorySceneRepository sceneRepository;
    private final EntityManager entityManager;

    @Transactional
    public WordResponse create(UUID parentId, UUID childId, WordCreateRequest request) {
        Child child = childService.getOwnedChild(parentId, childId);
        // 표제어로 정규화해서 저장한다 - 아이는 대사 어절을 그대로 누르므로
        // "기왓장이"처럼 조사가 붙어 온다. 정규화가 아래 중복 검사와 어휘 사전
        // 조회의 키이기도 해서, 이 순서(정규화 -> 중복 -> 사전 -> LLM)가
        // 같은 단어에 뜻 생성 LLM이 두 번 나가지 않게 하는 구조다.
        // → 프론트 docs/단어_저장_비용_속도_설계_조사.md
        String word = lemmatizer.lemmatize(request.word());
        if (wordbookRepository.existsByChildIdAndWord(childId, word)) {
            throw new BusinessException(ErrorCode.DUPLICATE_WORD);
        }

        StoryScene sourceScene = findSourceScene(request.sourceSceneId());
        Explanation explanation = explain(word, request, sourceScene);

        Wordbook saved = wordbookRepository.saveAndFlush(Wordbook.builder()
                .child(child)
                .word(word)
                .meaning(explanation.meaning())
                .exampleSentence(explanation.exampleSentence())
                .exampleDaily(explanation.exampleDaily())
                .exampleAdvanced(explanation.exampleAdvanced())
                .entryType(request.entryType())
                .sourceScene(sourceScene)
                .build());

        // created_at은 DB 기본값이라 저장 직후 엔티티에는 비어 있다. 응답이 그 값을 담으므로 되읽는다.
        entityManager.refresh(saved);
        return WordResponse.from(saved);
    }

    /** entryType이 null이면 전체. */
    public List<WordResponse> getWords(UUID parentId, UUID childId, WordEntryType entryType) {
        childService.getOwnedChild(parentId, childId);
        List<Wordbook> words = (entryType == null)
                ? wordbookRepository.findAllByChildIdOrderByCreatedAtDesc(childId)
                : wordbookRepository.findAllByChildIdAndEntryTypeOrderByCreatedAtDesc(childId, entryType);
        return words.stream().map(WordResponse::from).toList();
    }

    @Transactional
    public WordResponse toggleFavorite(UUID parentId, UUID childId, UUID wordId) {
        childService.getOwnedChild(parentId, childId);
        Wordbook word = getOwnedWord(childId, wordId);
        word.changeEntryType(word.getEntryType() == WordEntryType.FAVORITE
                ? WordEntryType.UNKNOWN : WordEntryType.FAVORITE);
        return WordResponse.from(word);
    }

    @Transactional
    public void delete(UUID parentId, UUID childId, UUID wordId) {
        childService.getOwnedChild(parentId, childId);
        wordbookRepository.delete(getOwnedWord(childId, wordId));
    }

    /**
     * 뜻과 예문 확보 - 요청 뜻 > 이야기 어휘 사전 > LLM 생성 순서다.
     *
     * <p>고정 대사의 낱말은 이야기 어휘 사전(story_vocabulary)에 검수된 뜻이
     * 있으므로 대부분 LLM 없이 끝난다. 사전에도 없는 단어만 LLM이 아이
     * 눈높이로 만든다(단어-02).
     */
    private Explanation explain(String word, WordCreateRequest request, StoryScene sourceScene) {
        String requestExample = (request.exampleSentence() != null
                && !request.exampleSentence().isBlank()) ? request.exampleSentence() : null;

        // 어휘 사전이 있으면 뜻/예문 3종을 검수본으로 채운다. 이야기 예문은
        // 요청 예문(아이가 단어를 만난 그 대사 문장)이 있으면 그쪽 우선.
        StoryVocabulary entry = (sourceScene == null) ? null
                : storyVocabularyRepository
                        .findByStoryIdAndWord(sourceScene.getStory().getId(), word)
                        .orElse(null);
        if (entry != null) {
            String meaning = (request.meaning() != null && !request.meaning().isBlank())
                    ? request.meaning() : entry.getMeaning();
            return new Explanation(meaning,
                    requestExample != null ? requestExample : entry.getExampleSentence(),
                    entry.getExampleDaily(), entry.getExampleAdvanced());
        }

        WordMeaningLlmClient.WordMeaningResult generated =
                wordMeaningLlmClient.generate(word, sceneContextOf(sourceScene));

        // 요청에 뜻이 실려 온 경로(검수/수동 입력)는 그 뜻을 신뢰한다. 예문
        // 3종을 채우기 위한 생성만 쓰고, 관문 판정과 생성 실패로 저장을
        // 막지 않는다 - 사람이 뜻을 쓴 단어를 모델 판정으로 거절할 이유가 없다.
        if (request.meaning() != null && !request.meaning().isBlank()) {
            boolean usable = generated.realWord();
            return new Explanation(request.meaning(),
                    requestExample != null ? requestExample
                            : (usable ? generated.exampleStory() : null),
                    usable ? generated.exampleDaily() : null,
                    usable ? generated.exampleAdvanced() : null);
        }

        // STT 오인식이 만든 존재하지 않는 말("방비" 부류)은 저장을 거절한다.
        // 단어장은 아이가 두고두고 다시 보는 학습 기록이라 쓰레기 단어가
        // 영구히 남으면 안 된다. 동적(LLM 생성) 대사에서 단어를 담는 경로를
        // 여는 전제 조건이기도 하다. 어휘 사전 히트는 검수된 경로라 이 관문을
        // 타지 않는다.
        if (!generated.realWord()) {
            throw new BusinessException(ErrorCode.INVALID_WORD);
        }

        // 요청이 예문을 함께 보냈다면 이야기 원문에서 딴 문장이므로 생성분보다 우선한다.
        return new Explanation(generated.meaning(),
                requestExample != null ? requestExample : generated.exampleStory(),
                generated.exampleDaily(), generated.exampleAdvanced());
    }

    /** 같은 단어라도 이야기 안에서 쓰인 뜻으로 풀어야 해서 장면 설명을 함께 넘긴다(단어-06). */
    private String sceneContextOf(StoryScene scene) {
        return scene == null ? null : scene.getSceneDescription();
    }

    private StoryScene findSourceScene(UUID sourceSceneId) {
        if (sourceSceneId == null) {
            return null;
        }
        return sceneRepository.findById(sourceSceneId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "장면을 찾을 수 없습니다."));
    }

    /**
     * 단어가 그 아이의 것인지까지 확인한다.
     *
     * <p>보호자가 자기 아이를 가진 것만 보고 넘기면, 아이가 둘인 보호자가 다른 아이의 단어를
     * 지우거나 즐겨찾기를 바꿀 수 있다. 남의 자원이 아니라 404로 알린다 — 있고 없고를
     * 알려 줄 이유가 없다.
     */
    private Wordbook getOwnedWord(UUID childId, UUID wordId) {
        return wordbookRepository.findByIdAndChildId(wordId, childId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "단어를 찾을 수 없습니다."));
    }

    private record Explanation(String meaning, String exampleSentence,
                               String exampleDaily, String exampleAdvanced) {}
}
