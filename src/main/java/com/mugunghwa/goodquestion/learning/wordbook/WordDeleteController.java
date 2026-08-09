package com.mugunghwa.goodquestion.learning.wordbook;

import com.mugunghwa.goodquestion.global.security.CurrentParentId;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * 단어 삭제(단어-05).
 *
 * <p>명세 경로가 childId 없이 {@code /api/words/{wordId}}라 {@link WordbookController}
 * (base가 {@code /api/children/{childId}/words}) 아래에 둘 수 없어 별도 컨트롤러로 분리했다.
 * TODO: WordbookService.delete(parentId, wordId) 구현 — 현재는 childId를 함께 받아야 소유권을 검증한다.
 */
@RestController
@RequiredArgsConstructor
public class WordDeleteController {

    @DeleteMapping("/api/words/{wordId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@CurrentParentId UUID parentId, @PathVariable UUID wordId) {
        throw new UnsupportedOperationException("미구현: 단어 삭제(childId 없이 소유권 검증)");
    }
}
