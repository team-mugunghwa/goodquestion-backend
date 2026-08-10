package com.mugunghwa.goodquestion.support;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * 테스트용 PostgreSQL 컨테이너.
 *
 * <p>{@code @ServiceConnection}이 컨테이너의 실제 접속 정보를 스프링에 주입하므로
 * application.yml의 {@code spring.datasource.*} 값 대신 이쪽이 쓰인다.
 * 컨테이너 기동/종료는 스프링 부트가 빈 생명주기에 맞춰 처리한다.
 *
 * <p>스프링 테스트 컨텍스트 캐시 덕분에 같은 설정을 쓰는 테스트 클래스끼리는
 * 컨테이너를 한 번만 띄우고 공유한다.
 *
 * <p>개발 DB에 붙이지 않고 매번 빈 DB를 새로 만드는 이유가 있다. 개발 DB를 쓰면 테스트
 * 통과 여부가 그 사람 DB 상태에 달리게 되고, 손으로 고친 스키마가 결과를 가린다.
 * 빈 DB에서 시작하면 "마이그레이션 파일만 갖고 처음부터 띄웠을 때 뜨는가"를 매번
 * 검사하게 되는데, 새 팀원과 배포 환경이 겪는 상황이 정확히 그것이다.
 *
 * <p>{@code ./gradlew test -PlocalDb}로 실행하면 {@code test.datasource.mode=local}이
 * 넘어와 이 설정이 통째로 꺼지고, 이미 설치해 둔 PostgreSQL에 붙는다. Docker를 쓰지 않는
 * 팀원을 위한 탈출구이고, 위 검사를 포기하는 대가가 있다. 자세한 내용은
 * docs/backend-dev-guide.md 2절에 있다.
 */
@TestConfiguration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "test.datasource.mode", havingValue = "testcontainers", matchIfMissing = true)
public class PostgresContainerConfig {

    /** 운영/로컬과 같은 메이저 버전을 쓴다. 버전이 갈리면 잡히지 않는 문법 차이가 생긴다. */
    private static final String IMAGE = "postgres:17";

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(IMAGE);
    }
}
