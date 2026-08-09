package com.mugunghwa.goodquestion.learning.wordbook;

import com.mugunghwa.goodquestion.global.security.CurrentParentId;
import com.mugunghwa.goodquestion.learning.wordbook.dto.WordCreateRequest;
import com.mugunghwa.goodquestion.learning.wordbook.dto.WordResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
    public List<WordResponse> getWords(@CurrentParentId UUID parentId, @PathVariable UUID childId,
                                       @RequestParam(required = false) WordEntryType entryType) {
        return wordbookService.getWords(parentId, childId, entryType);
    }

    @PatchMapping("/{wordId}/favorite")
    public WordResponse toggleFavorite(@CurrentParentId UUID parentId, @PathVariable UUID childId,
                                       @PathVariable UUID wordId) {
        return wordbookService.toggleFavorite(parentId, childId, wordId);
    }
}
