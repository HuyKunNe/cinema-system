package com.cinema.booking.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.cinema.booking.event.SeatReservationRequestedOutboxFactory;
import com.cinema.booking.exception.BookingErrorCode;
import com.cinema.booking.repository.BookingRepository;
import com.cinema.booking.repository.BookingSeatRepository;
import com.cinema.booking.service.BookingCreationService;
import com.cinema.common.exception.exception.InternalServerException;
import com.cinema.common.outbox.repository.OutboxRepository;
import com.cinema.common.test.container.AbstractMySqlIntegrationTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.UUID;

class BookingOutboxTransactionIntegrationTest extends AbstractMySqlIntegrationTest {

    @Autowired private BookingCreationService bookingCreationService;

    @Autowired private BookingRepository bookingRepository;

    @Autowired private BookingSeatRepository bookingSeatRepository;

    @Autowired private OutboxRepository outboxRepository;

    @MockitoBean private SeatReservationRequestedOutboxFactory outboxFactory;

    @BeforeEach
    void cleanDatabase() {

        outboxRepository.deleteAll();
        bookingSeatRepository.deleteAll();
        bookingRepository.deleteAll();
    }

    @Test
    void outboxCreationFailureShouldRollbackBookingAndSeats() {

        UUID userId = UUID.randomUUID();
        UUID showtimeId = UUID.randomUUID();

        when(outboxFactory.create(
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.anyList(),
                        org.mockito.ArgumentMatchers.any()))
                .thenThrow(
                        new InternalServerException(
                                BookingErrorCode.OUTBOX_PAYLOAD_SERIALIZATION_FAILED));

        assertThrows(
                InternalServerException.class,
                () ->
                        bookingCreationService.createNew(
                                userId,
                                showtimeId,
                                "request-1",
                                "a".repeat(64),
                                List.of("H7", "H8")));

        assertThat(bookingRepository.count()).isZero();
        assertThat(bookingSeatRepository.count()).isZero();
        assertThat(outboxRepository.count()).isZero();
    }

    @Test
    void bookingSeatsAndOutboxShouldCommitTogether() {

        UUID userId = UUID.randomUUID();
        UUID showtimeId = UUID.randomUUID();

        when(outboxFactory.create(
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.anyList(),
                        org.mockito.ArgumentMatchers.any()))
                .thenAnswer(
                        invocation -> {
                            com.cinema.booking.entity.Booking booking = invocation.getArgument(0);

                            java.time.OffsetDateTime occurredAt = invocation.getArgument(2);

                            return new com.cinema.common.outbox.entity.OutboxEventEntity(
                                    com.cinema.common.core.id.UuidGenerator.next(),
                                    com.cinema.common.outbox.enums.AggregateType.BOOKING,
                                    booking.getId(),
                                    "seat-reservation-requested",
                                    "1",
                                    "seat-reservation-requested",
                                    booking.getId().toString(),
                                    occurredAt,
                                    com.cinema.common.core.id.UuidGenerator.next(),
                                    null,
                                    """
                                    {
                                      "bookingId": "%s",
                                      "seatNumbers": ["H7", "H8"]
                                    }
                                    """
                                            .formatted(booking.getId()),
                                    occurredAt);
                        });

        bookingCreationService.createNew(
                userId, showtimeId, "request-1", "a".repeat(64), List.of("H7", "H8"));

        assertThat(bookingRepository.count()).isEqualTo(1);
        assertThat(bookingSeatRepository.count()).isEqualTo(2);
        assertThat(outboxRepository.count()).isEqualTo(1);

        com.cinema.common.outbox.entity.OutboxEventEntity outboxEvent =
                outboxRepository.findAll().getFirst();

        com.cinema.booking.entity.Booking booking = bookingRepository.findAll().getFirst();

        assertThat(outboxEvent.getAggregateId()).isEqualTo(booking.getId());

        assertThat(outboxEvent.getEventType()).isEqualTo("seat-reservation-requested");

        assertThat(outboxEvent.getTopic()).isEqualTo("seat-reservation-requested");

        assertThat(outboxEvent.getPartitionKey()).isEqualTo(booking.getId().toString());

        assertThat(outboxEvent.getStatus())
                .isEqualTo(com.cinema.common.outbox.enums.OutboxStatus.PENDING);
    }
}
