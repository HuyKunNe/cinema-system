package com.cinema.common.outbox.acknowledgement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cinema.common.outbox.enums.OutboxStatus;
import com.cinema.common.outbox.repository.OutboxRepository;
import com.cinema.common.outbox.retry.OutboxRetryPolicy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

class DefaultOutboxAcknowledgementServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-21T10:00:00Z");

    @Mock private OutboxRepository repository;

    @Mock private OutboxRetryPolicy retryPolicy;

    private DefaultOutboxAcknowledgementService service;

    @BeforeEach
    void setUp() {

        MockitoAnnotations.openMocks(this);

        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

        service = new DefaultOutboxAcknowledgementService(repository, retryPolicy, clock);
    }

    @Test
    void matchingClaimShouldAcknowledgeSuccessfulPublication() {

        UUID eventId = UUID.randomUUID();
        String owner = "outbox-claim:" + UUID.randomUUID();

        OffsetDateTime publishedAt = OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC);

        when(repository.markSentIfOwned(
                        eventId, owner, publishedAt, OutboxStatus.PROCESSING, OutboxStatus.SENT))
                .thenReturn(1);

        boolean acknowledged = service.acknowledgeSuccess(eventId, owner);

        assertThat(acknowledged).isTrue();

        verify(repository)
                .markSentIfOwned(
                        eventId, owner, publishedAt, OutboxStatus.PROCESSING, OutboxStatus.SENT);
    }

    @Test
    void staleSuccessfulCallbackShouldBeIgnored() {

        UUID eventId = UUID.randomUUID();
        String staleOwner = "outbox-claim:" + UUID.randomUUID();

        OffsetDateTime publishedAt = OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC);

        when(repository.markSentIfOwned(
                        eventId,
                        staleOwner,
                        publishedAt,
                        OutboxStatus.PROCESSING,
                        OutboxStatus.SENT))
                .thenReturn(0);

        boolean acknowledged = service.acknowledgeSuccess(eventId, staleOwner);

        assertThat(acknowledged).isFalse();
    }

    @Test
    void matchingClaimShouldScheduleRetryAfterFailure() {

        UUID eventId = UUID.randomUUID();
        String owner = "outbox-claim:" + UUID.randomUUID();

        int currentRetryCount = 1;

        OffsetDateTime failedAt = OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC);

        OffsetDateTime nextAttemptAt = failedAt.plusSeconds(2);

        RuntimeException exception = new RuntimeException("Kafka broker unavailable");

        when(retryPolicy.nextAttemptAt(currentRetryCount, failedAt)).thenReturn(nextAttemptAt);

        when(repository.markFailedIfOwned(
                        eventId,
                        owner,
                        nextAttemptAt,
                        "Kafka broker unavailable",
                        OutboxStatus.PROCESSING,
                        OutboxStatus.FAILED))
                .thenReturn(1);

        boolean acknowledged =
                service.acknowledgeFailure(eventId, owner, currentRetryCount, exception);

        assertThat(acknowledged).isTrue();

        verify(repository)
                .markFailedIfOwned(
                        eventId,
                        owner,
                        nextAttemptAt,
                        "Kafka broker unavailable",
                        OutboxStatus.PROCESSING,
                        OutboxStatus.FAILED);
    }

    @Test
    void persistedFailureMessageShouldBeBounded() {

        UUID eventId = UUID.randomUUID();
        String owner = "outbox-claim:" + UUID.randomUUID();

        OffsetDateTime failedAt = OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC);

        OffsetDateTime nextAttemptAt = failedAt.plusSeconds(1);

        RuntimeException exception = new RuntimeException("x".repeat(3000));

        when(retryPolicy.nextAttemptAt(0, failedAt)).thenReturn(nextAttemptAt);

        when(repository.markFailedIfOwned(
                        eventId,
                        owner,
                        nextAttemptAt,
                        "x".repeat(2000),
                        OutboxStatus.PROCESSING,
                        OutboxStatus.FAILED))
                .thenReturn(1);

        boolean acknowledged = service.acknowledgeFailure(eventId, owner, 0, exception);

        assertThat(acknowledged).isTrue();

        verify(repository)
                .markFailedIfOwned(
                        eventId,
                        owner,
                        nextAttemptAt,
                        "x".repeat(2000),
                        OutboxStatus.PROCESSING,
                        OutboxStatus.FAILED);
    }
}
