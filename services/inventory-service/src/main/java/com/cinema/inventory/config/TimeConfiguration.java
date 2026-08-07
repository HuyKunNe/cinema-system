package com.cinema.inventory.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Duration;

@Configuration
public class TimeConfiguration {

    @Bean
    Clock systemClock() {
        return Clock.tick(
                Clock.systemUTC(),
                Duration.ofNanos(1_000));
    }
}
