package com.cinema.common.outbox.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "cinema.outbox")
public record OutboxProperties(
        String producer,
        int batchSize,
        Duration schedulerDelay,
        Duration leaseDuration,
        int maximumAttempts,
        Duration baseRetryDelay,
        Duration maximumRetryDelay,
        Duration maximumJitter) {

    public OutboxProperties {

        if (producer == null || producer.isBlank()) {
            throw new IllegalStateException("cinema.outbox.producer must be configured");
        }

        producer = producer.trim();

        if (batchSize <= 0) {
            batchSize = 100;
        }

        if (schedulerDelay == null || schedulerDelay.isNegative() || schedulerDelay.isZero()) {

            schedulerDelay = Duration.ofSeconds(5);
        }

        if (leaseDuration == null || leaseDuration.isNegative() || leaseDuration.isZero()) {

            leaseDuration = Duration.ofSeconds(30);
        }

        if (maximumAttempts <= 0) {
            maximumAttempts = 5;
        }

        if (baseRetryDelay == null || baseRetryDelay.isNegative() || baseRetryDelay.isZero()) {

            baseRetryDelay = Duration.ofSeconds(1);
        }

        if (maximumRetryDelay == null
                || maximumRetryDelay.isNegative()
                || maximumRetryDelay.isZero()) {

            maximumRetryDelay = Duration.ofMinutes(1);
        }

        if (maximumJitter == null || maximumJitter.isNegative()) {

            maximumJitter = Duration.ofMillis(500);
        }

        if (maximumRetryDelay.compareTo(baseRetryDelay) < 0) {
            maximumRetryDelay = baseRetryDelay;
        }
    }
}
