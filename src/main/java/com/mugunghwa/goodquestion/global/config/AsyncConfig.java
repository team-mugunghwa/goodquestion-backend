package com.mugunghwa.goodquestion.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * ReportService.generate(@Async) 등 비동기 실행 활성화.
 *
 * <p>기본 {@code SimpleAsyncTaskExecutor}는 작업마다 스레드를 새로 만들고 상한이 없다.
 * 리포트 생성은 외부 LLM 호출을 태우므로, 완주가 몰리면 동시 호출 수가 무제한으로
 * 늘어나 벤더 비용과 부하가 같이 튄다. 유한 풀로 막는다.
 *
 * <p>가상 스레드(spring.threads.virtual.enabled)는 이 문제를 풀지 않는다 — 그건 I/O 대기로
 * 플랫폼 스레드가 마르는 걸 막는 것이고, 여기서 필요한 건 외부 호출 동시 실행 수 자체의 상한이다.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-");
        executor.initialize();
        return executor;
    }
}
