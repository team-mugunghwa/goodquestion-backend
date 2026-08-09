package com.mugunghwa.goodquestion.learning.wordbook;

import com.mugunghwa.goodquestion.global.security.CurrentParentId;
import com.mugunghwa.goodquestion.learning.wordbook.dto.WordCreateRequest;
import com.mugunghwa.goodquestion.learning.wordbook.dto.WordResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/children/{childId}/words")
@RequiredArgsConstructor
public class WordbookController {

    private final WordbookService wordbookService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WordResponse create(@CurrentParentId UUID parentId, @PathVariable UUID childId,
                               @Valid @RequestBody WordCreateRequest request) {
        return wordbookService.create(parentId, childId, request);
    }

    @GetMapping
    public Page<WordResponse> getWords(@CurrentParentId UUID parentId, @PathVariable UUID childId,
                                       @RequestParam(defaultValue = "false") boolean favoriteOnly,
                                       @PageableDefault(size = 20) Pageable pageable) {
        return wordbookService.getWords(parentId, childId, favoriteOnly, pageable);
    }

    @PatchMapping("/{wordId}/favorite")
    public WordResponse toggleFavorite(@CurrentParentId UUID parentId, @PathVariable UUID childId,
                                       @PathVariable UUID wordId) {
        return wordbookService.toggleFavorite(parentId, childId, wordId);
    }

    /**
     * @deprecated 명세 경로는 {@code DELETE /api/words/{wordId}}다({@link WordDeleteController}).
     * 프론트 전환이 끝나면 제거한다.
     */
    @Deprecated
    @DeleteMapping("/{wordId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@CurrentParentId UUID parentId, @PathVariable UUID childId,
                       @PathVariable UUID wordId) {
        wordbookService.delete(parentId, childId, wordId);
    }
}
