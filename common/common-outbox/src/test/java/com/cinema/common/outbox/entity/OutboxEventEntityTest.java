package com.cinema.common.outbox.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.cinema.common.outbox.enums.AggregateType;
import com.cinema.common.outbox.enums.OutboxStatus;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

class OutboxEventEntityTest {

    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-08-21T10:00:00Z");

    @Test
    void newEventShouldBeImmediatelyEligibleForPublication() {

        OutboxEventEntity event = event();

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(event.getRetryCount()).isZero();
        assertThat(event.getNextAttemptAt()).isEqualTo(CREATED_AT);
        assertThat(event.getProcessingOwner()).isNull();
        assertThat(event.getPublishedAt()).isNull();
    }

    @Test
    void claimShouldCreateProcessingLease() {

        OutboxEventEntity event = event();

        OffsetDateTime startedAt = CREATED_AT.plusSeconds(1);
        OffsetDateTime expiresAt = startedAt.plusSeconds(30);

        event.claim("instance-a:claim-1", startedAt, expiresAt);

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PROCESSING);
        assertThat(event.getProcessingOwner()).isEqualTo("instance-a:claim-1");
        assertThat(event.getProcessingStartedAt()).isEqualTo(startedAt);
        assertThat(event.getProcessingExpiresAt()).isEqualTo(expiresAt);
    }

    @Test
    void matchingOwnerShouldMarkEventSentAndClearLease() {

        OutboxEventEntity event = event();

        event.claim("instance-a:claim-1", CREATED_AT.plusSeconds(1), CREATED_AT.plusSeconds(31));

        boolean updated = event.markSent("instance-a:claim-1", CREATED_AT.plusSeconds(2));

        assertThat(updated).isTrue();
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.SENT);
        assertThat(event.getPublishedAt()).isEqualTo(CREATED_AT.plusSeconds(2));
        assertThat(event.getNextAttemptAt()).isNull();
        assertThat(event.getProcessingOwner()).isNull();
        assertThat(event.getProcessingStartedAt()).isNull();
        assertThat(event.getProcessingExpiresAt()).isNull();
    }

    @Test
    void staleOwnerShouldNotMarkReclaimedEventSent() {

        OutboxEventEntity event = event();

        event.claim("instance-b:claim-2", CREATED_AT.plusMinutes(1), CREATED_AT.plusMinutes(2));

        boolean updated = event.markSent("instance-a:claim-1", CREATED_AT.plusMinutes(1));

        assertThat(updated).isFalse();
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PROCESSING);
        assertThat(event.getProcessingOwner()).isEqualTo("instance-b:claim-2");
        assertThat(event.getPublishedAt()).isNull();
    }

    @Test
    void failureShouldIncrementRetryAndClearLease() {

        OutboxEventEntity event = event();

        event.claim("instance-a:claim-1", CREATED_AT.plusSeconds(1), CREATED_AT.plusSeconds(31));

        OffsetDateTime nextAttemptAt = CREATED_AT.plusSeconds(5);

        boolean updated =
                event.markFailed("instance-a:claim-1", nextAttemptAt, "Kafka broker unavailable");

        assertThat(updated).isTrue();
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(event.getRetryCount()).isEqualTo(1);
        assertThat(event.getNextAttemptAt()).isEqualTo(nextAttemptAt);
        assertThat(event.getLastError()).isEqualTo("Kafka broker unavailable");
        assertThat(event.getProcessingOwner()).isNull();
    }

    @Test
    void persistedErrorShouldBeBounded() {

        OutboxEventEntity event = event();

        event.claim("instance-a:claim-1", CREATED_AT.plusSeconds(1), CREATED_AT.plusSeconds(31));

        event.markFailed("instance-a:claim-1", CREATED_AT.plusSeconds(5), "x".repeat(3000));

        assertThat(event.getLastError()).hasSize(2000);
    }

    private OutboxEventEntity event() {

        UUID bookingId = UUID.randomUUID();

        return new OutboxEventEntity(
                UUID.randomUUID(),
                AggregateType.BOOKING,
                bookingId,
                "seat-reservation-requested",
                "1",
                "seat-reservation-requested",
                bookingId.toString(),
                CREATED_AT,
                UUID.randomUUID(),
                null,
                """
                {
                  "bookingId": "%s",
                  "seatNumbers": ["H7"]
                }
                """
                        .formatted(bookingId),
                CREATED_AT);
    }
}
