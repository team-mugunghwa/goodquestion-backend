package com.mugunghwa.goodquestion.learning.reward;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 이야기별 완주 횟수 (보상-05).
 *
 * <p>반복 완주 상한("2회차는 절반, 3회차부터 없음")의 판정 근거다. `COMPLETED` 세션을 세고
 * 나서 지급하면 조회와 지급 사이가 원자적이지 않아 동시 요청에 같은 회차 보상이 두 번 나간다.
 * upsert 한 문장으로 올리고 그 반환값으로 지급액을 정한다.
 *
 * <p>JPQL로는 {@code on conflict ... returning}을 표현할 수 없어 JdbcTemplate을 쓴다.
 * 같은 트랜잭션과 커넥션을 공유하므로 지갑 갱신과 원자성이 깨지지 않는다.
 */
@Component
@RequiredArgsConstructor
public class StoryPlayCounter {

    private final JdbcTemplate jdbcTemplate;

    /** 완주 횟수를 1 올리고 올린 뒤의 값을 돌려준다. */
    @Transactional
    public int increaseAndGet(UUID childId, UUID storyId) {
        Integer playCount = jdbcTemplate.queryForObject("""
                insert into child_story_play_counts (child_id, story_id, play_count, updated_at)
                values (?, ?, 1, now())
                on conflict (child_id, story_id) do update
                    set play_count = child_story_play_counts.play_count + 1, updated_at = now()
                returning play_count
                """, Integer.class, childId, storyId);
        return playCount == null ? 0 : playCount;
    }

    /** 완주한 적이 없으면 0. */
    @Transactional(readOnly = true)
    public int get(UUID childId, UUID storyId) {
        Integer playCount = jdbcTemplate.queryForObject("""
                select coalesce(max(play_count), 0) from child_story_play_counts
                where child_id = ? and story_id = ?
                """, Integer.class, childId, storyId);
        return playCount == null ? 0 : playCount;
    }
}
