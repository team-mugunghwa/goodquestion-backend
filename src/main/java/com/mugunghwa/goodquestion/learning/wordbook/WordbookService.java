package com.mugunghwa.goodquestion.learning.wordbook;

import com.mugunghwa.goodquestion.global.error.BusinessException;
import com.mugunghwa.goodquestion.global.error.ErrorCode;
import com.mugunghwa.goodquestion.infra.llm.WordMeaningLlmClient;
import com.mugunghwa.goodquestion.learning.wordbook.dto.WordCreateRequest;
import com.mugunghwa.goodquestion.learning.wordbook.dto.WordResponse;
import com.mugunghwa.goodquestion.user.child.ChildService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WordbookService {

    private final WordbookRepository wordbookRepository;
    private final WordMeaningLlmClient wordMeaningLlmClient;
    private final ChildService childService;

    @Transactional
    public WordResponse create(UUID parentId, UUID childId, WordCreateRequest request) {
        childService.getOwnedChild(parentId, childId);
        if (wordbookRepository.existsByChildIdAndWord(childId, request.word())) {
            throw new BusinessException(ErrorCode.DUPLICATE_WORD);
        }
        // TODO: LLM으로 아이 수준의 쉬운 뜻 + 이야기 속 문장(sourceScene 문맥) 생성 후 저장
        throw new UnsupportedOperationException("TODO");
    }

    public Page<WordResponse> getWords(UUID parentId, UUID childId, boolean favoriteOnly, Pageable pageable) {
        childService.getOwnedChild(parentId, childId);
        Page<Wordbook> words = favoriteOnly
                ? wordbookRepository.findAllByChildIdAndFavoriteTrue(childId, pageable)
                : wordbookRepository.findAllByChildId(childId, pageable);
        return words.map(WordResponse::from);
    }

    @Transactional
    public WordResponse toggleFavorite(UUID parentId, UUID childId, UUID wordId) {
        childService.getOwnedChild(parentId, childId);
        Wordbook word = wordbookRepository.findById(wordId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        // TODO: word.child == childId 검증
        word.toggleFavorite();
        return WordResponse.from(word);
    }

    @Transactional
    public void delete(UUID parentId, UUID childId, UUID wordId) {
        childService.getOwnedChild(parentId, childId);
        // TODO: 소유 검증 후 삭제
        wordbookRepository.deleteById(wordId);
    }
}
