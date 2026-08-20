package com.cinema.booking.config;

import java.time.Clock;
import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class TimeConfiguration {

    @Bean
    Clock systemClock() {
        return Clock.tick(
                Clock.systemUTC(),
                Duration.ofNanos(1_000));
    }
}
