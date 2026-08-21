package com.cinema.common.outbox.claim;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cinema.common.outbox.config.OutboxProperties;
import com.cinema.common.outbox.entity.OutboxEventEntity;
import com.cinema.common.outbox.enums.AggregateType;
import com.cinema.common.outbox.enums.OutboxStatus;
import com.cinema.common.outbox.repository.OutboxRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

class DefaultOutboxClaimServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-21T10:00:00Z");

    @Mock private OutboxRepository repository;

    private OutboxProperties properties;

    private DefaultOutboxClaimService claimService;

    @BeforeEach
    void setUp() {

        MockitoAnnotations.openMocks(this);

        properties =
                new OutboxProperties(
                        "booking-service",
                        100,
                        Duration.ofSeconds(5),
                        Duration.ofSeconds(30),
                        5,
                        Duration.ofSeconds(1),
                        Duration.ofMinutes(1),
                        Duration.ofMillis(500));

        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

        claimService = new DefaultOutboxClaimService(repository, properties, clock);
    }

    @Test
    void shouldClaimEligibleEventsWithUniqueProcessingOwners() {

        OutboxEventEntity first = event();
        OutboxEventEntity second = event();

        OffsetDateTime now = OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC);

        when(repository.findClaimableEvents(
                        now, properties.maximumAttempts(), properties.batchSize()))
                .thenReturn(List.of(first, second));

        List<OutboxEventEntity> claimed = claimService.claimNextBatch();

        assertThat(claimed).containsExactly(first, second);

        assertThat(claimed)
                .extracting(OutboxEventEntity::getStatus)
                .containsOnly(OutboxStatus.PROCESSING);

        assertThat(first.getProcessingOwner()).startsWith("outbox-claim:");

        assertThat(second.getProcessingOwner()).startsWith("outbox-claim:");

        assertThat(first.getProcessingOwner()).isNotEqualTo(second.getProcessingOwner());

        assertThat(first.getProcessingStartedAt()).isEqualTo(now);

        assertThat(first.getProcessingExpiresAt()).isEqualTo(now.plusSeconds(30));

        verify(repository)
                .findClaimableEvents(now, properties.maximumAttempts(), properties.batchSize());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<OutboxEventEntity>> captor = ArgumentCaptor.forClass(List.class);

        verify(repository).saveAll(captor.capture());

        assertThat(captor.getValue()).containsExactly(first, second);
    }

    @Test
    void emptyBatchShouldReturnEmptyList() {

        OffsetDateTime now = OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC);

        when(repository.findClaimableEvents(
                        now, properties.maximumAttempts(), properties.batchSize()))
                .thenReturn(List.of());

        List<OutboxEventEntity> claimed = claimService.claimNextBatch();

        assertThat(claimed).isEmpty();

        verify(repository)
                .findClaimableEvents(now, properties.maximumAttempts(), properties.batchSize());

        verify(repository, org.mockito.Mockito.never()).saveAll(org.mockito.ArgumentMatchers.any());
    }

    private OutboxEventEntity event() {

        UUID bookingId = UUID.randomUUID();

        OffsetDateTime createdAt = OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC);

        return new OutboxEventEntity(
                UUID.randomUUID(),
                AggregateType.BOOKING,
                bookingId,
                "seat-reservation-requested",
                "1",
                "seat-reservation-requested",
                bookingId.toString(),
                createdAt,
                UUID.randomUUID(),
                null,
                """
                {
                  "bookingId": "%s",
                  "seatNumbers": ["H7"]
                }
                """
                        .formatted(bookingId),
                createdAt);
    }
}
