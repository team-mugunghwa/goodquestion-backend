package com.mugunghwa.goodquestion.story.content.dto;

import java.util.List;
import java.util.UUID;

/**
 * 이 아이가 완주한 이야기의 id들.
 *
 * <p><b>목록 화면의 "끝냈어" 도장이 이 응답 하나로 그려진다.</b> 이전에는 프런트가
 * 보호자 리포트 목록의 <b>이야기 제목 문자열</b>로 맞추고 있었는데, 리포트에는
 * {@code storyId}가 없어서 그 방법밖에 없었다. 제목이 같은 이야기가 둘 생기거나 제목이
 * 한 글자만 바뀌어도 조용히 틀리는 판정이었다.
 *
 * <p>이야기 <b>한 편</b>의 완주 여부는 이 응답이 아니라 자유 대화 인물 목록
 * ({@code /free-talk/characters})으로도 알 수 있다 - 미완주면 404다. 그쪽은 이야기당
 * 한 번씩 물어야 해서 여덟 편짜리 목록에는 못 쓴다. 그래서 목록용으로 이것 하나를 둔다.
 *
 * <p>날짜를 함께 주지 않는 이유 - 지금 화면이 쓰지 않는다. 목록 카드에 "3일 전에
 * 끝냈어"를 붙이게 되면 그때 필드를 늘린다.
 */
public record CompletedStoriesResponse(List<UUID> storyIds) {
}
