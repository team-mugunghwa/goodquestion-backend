package com.mugunghwa.goodquestion.support;

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
 */
@TestConfiguration(proxyBeanMethods = false)
public class PostgresContainerConfig {

    /** 운영/로컬과 같은 메이저 버전을 쓴다. 버전이 갈리면 잡히지 않는 문법 차이가 생긴다. */
    private static final String IMAGE = "postgres:17";

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(IMAGE);
    }
}
