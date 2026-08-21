package com.cinema.common.outbox.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;
import java.util.random.RandomGenerator;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
@EnableConfigurationProperties(OutboxProperties.class)
public class OutboxConfiguration {

    @Bean
    @ConditionalOnMissingBean(Clock.class)
    public Clock outboxClock() {

        return Clock.systemUTC();
    }

    @Bean
    @ConditionalOnMissingBean(RandomGenerator.class)
    public RandomGenerator outboxRandomGenerator() {

        return RandomGenerator.getDefault();
    }
}
