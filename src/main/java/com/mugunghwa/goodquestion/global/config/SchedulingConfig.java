package com.mugunghwa.goodquestion.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** 예약 작업 활성화. 현재는 멱등 기록 청소(IdempotencyRecordStore.purgeExpired)뿐이다. */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
