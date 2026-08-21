package com.cinema.common.outbox.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
public class OutboxConfiguration {
    @Bean
    public Clock outboxClock() {

        return Clock.systemUTC();
    }
}
