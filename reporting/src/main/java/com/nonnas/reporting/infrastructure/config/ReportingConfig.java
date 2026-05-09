package com.nonnas.reporting.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

@Configuration
@EnableScheduling
public class ReportingConfig {

    @Bean
    public Clock reportingClock() {
        return Clock.systemUTC();
    }
}
