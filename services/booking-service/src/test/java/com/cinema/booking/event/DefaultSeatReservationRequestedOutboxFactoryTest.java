package com.cinema.booking.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cinema.booking.entity.Booking;
import com.cinema.booking.entity.BookingSeat;
import com.cinema.booking.enums.BookingStatus;
import com.cinema.common.exception.exception.InternalServerException;
import com.cinema.common.jackson.config.JacksonConfiguration;
import com.cinema.common.outbox.entity.OutboxEventEntity;
import com.cinema.common.outbox.enums.AggregateType;
import com.cinema.common.outbox.enums.OutboxStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

class DefaultSeatReservationRequestedOutboxFactoryTest {

    private final ObjectMapper objectMapper = new JacksonConfiguration().objectMapper();

    private final DefaultSeatReservationRequestedOutboxFactory factory =
            new DefaultSeatReservationRequestedOutboxFactory(objectMapper);

    @Test
    void shouldCreateCanonicalSeatReservationRequestedOutboxEvent() throws Exception {

        OffsetDateTime occurredAt = OffsetDateTime.parse("2026-08-21T10:00:00Z");

        OffsetDateTime holdExpiresAt = occurredAt.plusMinutes(10);

        UUID userId = UUID.randomUUID();
        UUID showtimeId = UUID.randomUUID();

        Booking booking =
                new Booking(
                        userId, showtimeId, "request-1", "a".repeat(64), holdExpiresAt, occurredAt);

        BookingSeat h8 = new BookingSeat(booking.getId(), showtimeId, "H8");

        BookingSeat h7 = new BookingSeat(booking.getId(), showtimeId, "H7");

        OutboxEventEntity event = factory.create(booking, List.of(h8, h7), occurredAt);

        assertThat(event.getId().version()).isEqualTo(7);
        assertThat(event.getAggregateType()).isEqualTo(AggregateType.BOOKING);
        assertThat(event.getAggregateId()).isEqualTo(booking.getId());
        assertThat(event.getEventType()).isEqualTo(BookingEventContract.SEAT_RESERVATION_REQUESTED);
        assertThat(event.getEventVersion())
                .isEqualTo(BookingEventContract.SEAT_RESERVATION_REQUESTED_VERSION);
        assertThat(event.getTopic()).isEqualTo(BookingEventContract.SEAT_RESERVATION_REQUESTED);
        assertThat(event.getPartitionKey()).isEqualTo(booking.getId().toString());
        assertThat(event.getOccurredAt()).isEqualTo(occurredAt);
        assertThat(event.getCorrelationId()).isNotNull();
        assertThat(event.getCorrelationId().version()).isEqualTo(7);
        assertThat(event.getCausationId()).isNull();
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(event.getRetryCount()).isZero();
        assertThat(event.getNextAttemptAt()).isEqualTo(occurredAt);
        assertThat(event.getCreatedAt()).isEqualTo(occurredAt);

        JsonNode payload = objectMapper.readTree(event.getPayload());

        assertThat(payload.get("bookingId").asText()).isEqualTo(booking.getId().toString());
        assertThat(payload.get("userId").asText()).isEqualTo(userId.toString());
        assertThat(payload.get("showtimeId").asText()).isEqualTo(showtimeId.toString());
        assertThat(OffsetDateTime.parse(payload.get("requestedAt").asText())).isEqualTo(occurredAt);

        assertThat(OffsetDateTime.parse(payload.get("holdExpiresAt").asText()))
                .isEqualTo(holdExpiresAt);

        assertThat(payload.get("seats"))
                .extracting(seat -> seat.get("seatNumber").asText())
                .containsExactly("H7", "H8");

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING);
    }

    @Test
    void serializationFailureShouldUseStableBookingError() throws Exception {

        ObjectMapper failingMapper = mock(ObjectMapper.class);

        when(failingMapper.writeValueAsString(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new JsonProcessingException("Serialization failure") {});

        DefaultSeatReservationRequestedOutboxFactory failingFactory =
                new DefaultSeatReservationRequestedOutboxFactory(failingMapper);

        OffsetDateTime occurredAt = OffsetDateTime.parse("2026-08-21T10:00:00Z");

        UUID showtimeId = UUID.randomUUID();

        Booking booking =
                new Booking(
                        UUID.randomUUID(),
                        showtimeId,
                        "request-1",
                        "a".repeat(64),
                        occurredAt.plusMinutes(10),
                        occurredAt);

        BookingSeat bookingSeat = new BookingSeat(booking.getId(), showtimeId, "H7");

        InternalServerException exception =
                assertThrows(
                        InternalServerException.class,
                        () -> failingFactory.create(booking, List.of(bookingSeat), occurredAt));

        assertThat(exception.getErrorCode().code())
                .isEqualTo("BOOKING_OUTBOX_PAYLOAD_SERIALIZATION_FAILED");
    }
}
