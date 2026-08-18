package com.mugunghwa.goodquestion.story.freetalk;

import com.mugunghwa.goodquestion.global.idempotency.IdempotencyService;
import com.mugunghwa.goodquestion.global.idempotency.IdempotentEndpoint;
import com.mugunghwa.goodquestion.global.security.CurrentParentId;
import com.mugunghwa.goodquestion.story.freetalk.dto.FreeTalkCharacterResponse;
import com.mugunghwa.goodquestion.story.freetalk.dto.FreeTalkEndResponse;
import com.mugunghwa.goodquestion.story.freetalk.dto.FreeTalkMessageRequest;
import com.mugunghwa.goodquestion.story.freetalk.dto.FreeTalkStartRequest;
import com.mugunghwa.goodquestion.story.freetalk.dto.FreeTalkStartResponse;
import com.mugunghwa.goodquestion.story.freetalk.dto.FreeTalkTurnResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * 후속 자유 대화. 인물 목록·시작은 아이 소유 경로로, 진행·마무리는 대화 자체를 가리키는
 * 경로로 받는다 - 대화가 만들어진 뒤에는 아이와 이야기가 이미 대화에 붙어 있어 경로에
 * 다시 실을 이유가 없다.
 */
@RestController
@RequiredArgsConstructor
public class FreeTalkController {

    private final FreeTalkService freeTalkService;
    private final IdempotencyService idempotencyService;

    /** 완주한 이야기의 대화 가능 인물. 완주하지 않았으면 404다(스포일러 방지). */
    @GetMapping("/api/children/{childId}/stories/{storyId}/free-talk/characters")
    public List<FreeTalkCharacterResponse> getCharacters(@CurrentParentId UUID parentId,
                                                         @PathVariable UUID childId,
                                                         @PathVariable UUID storyId) {
        return freeTalkService.getCharacters(parentId, childId, storyId);
    }

    @PostMapping("/api/children/{childId}/free-talk")
    @ResponseStatus(HttpStatus.CREATED)
    public FreeTalkStartResponse start(@CurrentParentId UUID parentId,
                                       @PathVariable UUID childId,
                                       @Valid @RequestBody FreeTalkStartRequest request) {
        return freeTalkService.start(parentId, childId, request);
    }

    /**
     * 아이의 말 제출. 음성 인식은 클라이언트가 /api/stt로 하고 여기는 텍스트만 받는다.
     *
     * <p>Idempotency-Key 헤더(선택)를 실으면 타임아웃 후의 재전송이 중복 턴이 되지 않는다 -
     * 완료된 요청이면 저장된 응답을 재생한다. 학습 발화와 같은 이유(중복 처리 시 LLM 요금
     * 2배)이고 규약도 같다. 키는 발화마다 새로 만들고 재시도 사이에만 유지한다.
     */
    @PostMapping("/api/free-talk/{freeTalkId}/messages")
    public FreeTalkTurnResponse speak(@CurrentParentId UUID parentId,
                                      @PathVariable UUID freeTalkId,
                                      @RequestHeader(value = "Idempotency-Key", required = false)
                                      String idempotencyKey,
                                      @Valid @RequestBody FreeTalkMessageRequest request) {
        return idempotencyService.execute(IdempotentEndpoint.FREE_TALK_MESSAGE, freeTalkId,
                parentId, idempotencyKey, FreeTalkTurnResponse.class,
                () -> freeTalkService.speak(parentId, freeTalkId, request));
    }

    /** 아이가 먼저 그만둘 때. 캐릭터가 작별 인사를 남기고 대화가 닫힌다. */
    @PostMapping("/api/free-talk/{freeTalkId}/end")
    public FreeTalkEndResponse end(@CurrentParentId UUID parentId,
                                   @PathVariable UUID freeTalkId) {
        return freeTalkService.end(parentId, freeTalkId);
    }
}
