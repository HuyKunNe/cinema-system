package com.cinema.movie.config;

import java.time.Clock;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class TimeConfiguration {

    @Bean
    @ConditionalOnMissingBean(Clock.class)
    public Clock movieClock() {
        return Clock.systemUTC();
    }
}
