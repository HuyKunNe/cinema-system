package com.cinema.booking.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "cinema.booking.kafka.retry")
public record BookingKafkaRetryProperties(Duration interval, long maximumRetries) {

    public BookingKafkaRetryProperties {

        if (interval == null || interval.isZero() || interval.isNegative()) {

            interval = Duration.ofSeconds(1);
        }

        if (maximumRetries < 0) {
            maximumRetries = 3;
        }
    }
}
