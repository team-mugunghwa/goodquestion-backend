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
    private final WordMeaningLlmClient wordMeaningLlmClient;
    private final ChildService childService;
    private final StorySceneRepository sceneRepository;
    private final EntityManager entityManager;

    @Transactional
    public WordResponse create(UUID parentId, UUID childId, WordCreateRequest request) {
        Child child = childService.getOwnedChild(parentId, childId);
        if (wordbookRepository.existsByChildIdAndWord(childId, request.word())) {
            throw new BusinessException(ErrorCode.DUPLICATE_WORD);
        }

        StoryScene sourceScene = findSourceScene(request.sourceSceneId());
        Explanation explanation = explain(request, sourceScene);

        Wordbook saved = wordbookRepository.saveAndFlush(Wordbook.builder()
                .child(child)
                .word(request.word())
                .meaning(explanation.meaning())
                .exampleSentence(explanation.exampleSentence())
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
     * 뜻과 예문 확보. 요청에 뜻이 있으면 그대로 쓰고, 없으면 LLM이 아이 눈높이로 만든다(단어-02).
     *
     * <p>아이가 이야기를 듣다 모르는 단어를 누르는 경로에서는 뜻이 올 수 없으므로 LLM을 탄다.
     * 벤더 선정 전까지 그 경로는 501이고, 클라이언트가 뜻을 직접 담아 보내는 경로는 지금도 동작한다.
     */
    private Explanation explain(WordCreateRequest request, StoryScene sourceScene) {
        if (request.meaning() != null && !request.meaning().isBlank()) {
            return new Explanation(request.meaning(), request.exampleSentence());
        }

        WordMeaningLlmClient.WordMeaningResult generated =
                wordMeaningLlmClient.generate(request.word(), sceneContextOf(sourceScene));

        // 요청이 예문을 함께 보냈다면 이야기 원문에서 딴 문장이므로 생성분보다 우선한다.
        return new Explanation(generated.meaning(),
                (request.exampleSentence() != null && !request.exampleSentence().isBlank())
                        ? request.exampleSentence() : generated.exampleSentence());
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

    private record Explanation(String meaning, String exampleSentence) {}
}
