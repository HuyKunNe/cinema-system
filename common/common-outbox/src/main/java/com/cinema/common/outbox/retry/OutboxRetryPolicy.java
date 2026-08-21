package com.cinema.common.outbox.retry;

import com.cinema.common.outbox.config.OutboxProperties;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.random.RandomGenerator;

@Component
public class OutboxRetryPolicy {

    private final OutboxProperties properties;

    private final RandomGenerator randomGenerator;

    public OutboxRetryPolicy(OutboxProperties properties, RandomGenerator randomGenerator) {

        this.properties = properties;
        this.randomGenerator = randomGenerator;
    }

    public OffsetDateTime nextAttemptAt(int currentRetryCount, OffsetDateTime failedAt) {

        int exponent = Math.min(currentRetryCount, 30);

        long multiplier = 1L << exponent;

        Duration exponentialDelay = multiplySafely(properties.baseRetryDelay(), multiplier);

        Duration boundedDelay =
                exponentialDelay.compareTo(properties.maximumRetryDelay()) > 0
                        ? properties.maximumRetryDelay()
                        : exponentialDelay;

        long maximumJitterMillis = properties.maximumJitter().toMillis();

        long jitterMillis =
                maximumJitterMillis == 0 ? 0 : randomGenerator.nextLong(maximumJitterMillis + 1);

        return failedAt.plus(boundedDelay).plus(Duration.ofMillis(jitterMillis));
    }

    private static Duration multiplySafely(Duration duration, long multiplier) {

        try {
            return duration.multipliedBy(multiplier);

        } catch (ArithmeticException exception) {
            return Duration.ofSeconds(Long.MAX_VALUE);
        }
    }
}
