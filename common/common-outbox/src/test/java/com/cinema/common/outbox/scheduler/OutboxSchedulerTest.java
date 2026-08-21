package com.cinema.common.outbox.scheduler;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.cinema.common.outbox.acknowledgement.OutboxAcknowledgementService;
import com.cinema.common.outbox.claim.OutboxClaimService;
import com.cinema.common.outbox.entity.OutboxEventEntity;
import com.cinema.common.outbox.enums.AggregateType;
import com.cinema.common.outbox.publisher.OutboxPublisher;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

class OutboxSchedulerTest {

    @Mock private OutboxClaimService claimService;

    @Mock private OutboxPublisher publisher;

    @Mock private OutboxAcknowledgementService acknowledgementService;

    private OutboxScheduler scheduler;

    @BeforeEach
    void setUp() {

        MockitoAnnotations.openMocks(this);

        scheduler = new OutboxScheduler(claimService, publisher, acknowledgementService);
    }

    @Test
    void successfulKafkaAcknowledgementShouldMarkClaimSent() {

        OutboxEventEntity event = claimedEvent();

        when(claimService.claimNextBatch()).thenReturn(List.of(event));

        when(publisher.publish(event)).thenReturn(CompletableFuture.completedFuture(null));

        scheduler.publishPendingEvents();

        verify(acknowledgementService)
                .acknowledgeSuccess(event.getId(), event.getProcessingOwner());
    }

    @Test
    void failedKafkaAcknowledgementShouldScheduleRetry() {

        OutboxEventEntity event = claimedEvent();

        RuntimeException failure = new RuntimeException("Kafka unavailable");

        CompletableFuture<Void> failedFuture = CompletableFuture.failedFuture(failure);

        when(claimService.claimNextBatch()).thenReturn(List.of(event));

        when(publisher.publish(event)).thenReturn(failedFuture);

        scheduler.publishPendingEvents();

        verify(acknowledgementService)
                .acknowledgeFailure(
                        event.getId(), event.getProcessingOwner(), event.getRetryCount(), failure);
    }

    @Test
    void emptyClaimBatchShouldNotInvokePublisher() {

        when(claimService.claimNextBatch()).thenReturn(List.of());

        scheduler.publishPendingEvents();

        verifyNoInteractions(publisher, acknowledgementService);
    }

    private OutboxEventEntity claimedEvent() {

        OffsetDateTime now = OffsetDateTime.parse("2026-08-21T10:00:00Z");

        UUID bookingId = UUID.randomUUID();

        OutboxEventEntity event =
                new OutboxEventEntity(
                        UUID.randomUUID(),
                        AggregateType.BOOKING,
                        bookingId,
                        "seat-reservation-requested",
                        "1",
                        "seat-reservation-requested",
                        bookingId.toString(),
                        now,
                        UUID.randomUUID(),
                        null,
                        """
                        {
                          "bookingId": "%s",
                          "seatNumbers": ["H7"]
                        }
                        """
                                .formatted(bookingId),
                        now);

        event.claim("outbox-claim:" + UUID.randomUUID(), now, now.plusSeconds(30));

        return event;
    }
}
