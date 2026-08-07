package com.mugunghwa.goodquestion.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/** ReportService.generate(@Async) 등 비동기 실행 활성화 */
@Configuration
@EnableAsync
public class AsyncConfig {
    // TODO: 스레드풀 크기·큐 용량 튜닝 (기본 SimpleAsyncTaskExecutor는 운영 부적합)
}
