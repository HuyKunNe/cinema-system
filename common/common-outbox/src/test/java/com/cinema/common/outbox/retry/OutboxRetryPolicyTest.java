package com.cinema.common.outbox.retry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cinema.common.outbox.config.OutboxProperties;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.random.RandomGenerator;

class OutboxRetryPolicyTest {

    private final RandomGenerator randomGenerator = mock(RandomGenerator.class);

    private final OutboxProperties properties =
            new OutboxProperties(
                    "booking-service",
                    100,
                    Duration.ofSeconds(5),
                    Duration.ofSeconds(30),
                    5,
                    Duration.ofSeconds(1),
                    Duration.ofSeconds(10),
                    Duration.ofMillis(500));

    private final OutboxRetryPolicy retryPolicy =
            new OutboxRetryPolicy(properties, randomGenerator);

    @Test
    void firstFailureShouldUseBaseDelayAndJitter() {

        OffsetDateTime failedAt = OffsetDateTime.parse("2026-08-21T10:00:00Z");

        when(randomGenerator.nextLong(501)).thenReturn(250L);

        OffsetDateTime nextAttemptAt = retryPolicy.nextAttemptAt(0, failedAt);

        assertThat(nextAttemptAt).isEqualTo(failedAt.plusSeconds(1).plus(Duration.ofMillis(250)));
    }

    @Test
    void retryDelayShouldGrowExponentially() {

        OffsetDateTime failedAt = OffsetDateTime.parse("2026-08-21T10:00:00Z");

        when(randomGenerator.nextLong(501)).thenReturn(0L);

        assertThat(retryPolicy.nextAttemptAt(0, failedAt)).isEqualTo(failedAt.plusSeconds(1));

        assertThat(retryPolicy.nextAttemptAt(1, failedAt)).isEqualTo(failedAt.plusSeconds(2));

        assertThat(retryPolicy.nextAttemptAt(2, failedAt)).isEqualTo(failedAt.plusSeconds(4));
    }

    @Test
    void retryDelayShouldBeLimitedByConfiguredMaximum() {

        OffsetDateTime failedAt = OffsetDateTime.parse("2026-08-21T10:00:00Z");

        when(randomGenerator.nextLong(501)).thenReturn(0L);

        OffsetDateTime nextAttemptAt = retryPolicy.nextAttemptAt(10, failedAt);

        assertThat(nextAttemptAt).isEqualTo(failedAt.plusSeconds(10));
    }
}
