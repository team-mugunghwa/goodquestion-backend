package com.mugunghwa.goodquestion.user.parent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * "이 사람이 오늘 다녀갔다"를 남긴다. 관리자 대시보드의 오늘 방문자 수가 이걸 센다.
 *
 * <p><b>왜 요청마다 DB를 치지 않는가.</b> 인증된 요청 하나하나에 upsert를 날리면
 * 이야기 한 편 진행에만 수십 번이 나간다. 대시보드가 묻는 것은 "오늘 왔는가"라서
 * 그 정확도에 커넥션을 쓸 이유가 없다. 그래서 사용자별로 마지막에 기록한 날짜를
 * 메모리에 들고, 날짜가 바뀔 때만 DB에 쓴다.
 *
 * <p>인스턴스를 여러 개 띄우면 각 인스턴스가 하루에 한 번씩 쓴다. upsert라 결과는 같고
 * 쓰기가 인스턴스 수만큼 늘 뿐이다. 캐시는 재기동하면 비지만 그때도 하루 한 번이 두 번이
 * 될 뿐이다 - 정확도가 아니라 쓰기 횟수만 흔들린다.
 *
 * <p>JPA 대신 upsert SQL을 쓴다. 엔티티로 하면 "읽어 보고 없으면 넣기"가 되는데,
 * 같은 사용자가 두 기기에서 동시에 들어오면 그 사이에 유일 제약 위반이 난다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VisitRecorder {

    /** 서비스 기준 시간대. UTC로 세면 자정부터 오전 9시까지가 어제로 집계된다. */
    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

    private static final String UPSERT = """
            insert into daily_visits (parent_id, visit_date, visit_count, last_seen_at)
            values (?, ?, 1, now())
            on conflict (parent_id, visit_date)
            do update set visit_count = daily_visits.visit_count + 1, last_seen_at = now()
            """;

    private final JdbcTemplate jdbcTemplate;

    /** 사용자별 마지막으로 기록한 날짜. 같은 날 두 번째 요청부터는 DB를 치지 않는다. */
    private final Map<UUID, LocalDate> lastRecorded = new ConcurrentHashMap<>();

    /**
     * 방문을 기록한다.
     *
     * <p>{@code REQUIRES_NEW}로 분리한다. 이 기록이 실패해도 사용자가 하려던 일은
     * 그대로 끝나야 한다 - 방문 통계 때문에 이야기 진행이 롤백되면 안 된다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(UUID parentId) {
        LocalDate today = LocalDate.now(SERVICE_ZONE);
        LocalDate previous = lastRecorded.put(parentId, today);
        if (today.equals(previous)) {
            return;
        }
        try {
            jdbcTemplate.update(UPSERT, parentId, today);
        } catch (Exception e) {
            // 통계 실패로 요청을 깨뜨리지 않는다. 다음 요청에서 다시 시도되도록
            // 캐시에서 빼 둔다.
            lastRecorded.remove(parentId);
            log.warn("방문 기록 실패. parentId={} 사유={}", parentId, e.getMessage());
        }
    }
}
