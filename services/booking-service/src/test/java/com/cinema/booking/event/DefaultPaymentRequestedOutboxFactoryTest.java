package com.cinema.booking.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cinema.booking.entity.Booking;
import com.cinema.booking.enums.BookingStatus;
import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.InternalServerException;
import com.cinema.common.jackson.config.JacksonConfiguration;
import com.cinema.common.outbox.entity.OutboxEventEntity;
import com.cinema.common.outbox.enums.AggregateType;
import com.cinema.common.outbox.enums.OutboxStatus;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

class DefaultPaymentRequestedOutboxFactoryTest {

    private final ObjectMapper objectMapper = new JacksonConfiguration().objectMapper();

    private final DefaultPaymentRequestedOutboxFactory factory =
            new DefaultPaymentRequestedOutboxFactory(objectMapper);

    @Test
    void shouldCreateCanonicalPaymentRequestedOutboxEvent() throws Exception {

        OffsetDateTime createdAt = OffsetDateTime.parse("2026-08-25T10:00:00Z");

        OffsetDateTime holdExpiresAt = OffsetDateTime.parse("2026-08-25T10:10:00Z");

        OffsetDateTime requestedAt = OffsetDateTime.parse("2026-08-25T10:01:00Z");

        UUID userId = UuidGenerator.next();

        UUID showtimeId = UuidGenerator.next();

        Booking booking =
                new Booking(
                        userId, showtimeId, "request-1", "a".repeat(64), holdExpiresAt, createdAt);

        booking.reserve(new BigDecimal("180000.00"), "VND");

        UUID sourceEventId = UuidGenerator.next();

        UUID correlationId = UuidGenerator.next();

        OutboxEventMessage sourceEvent = mock(OutboxEventMessage.class);

        when(sourceEvent.eventId()).thenReturn(sourceEventId);

        when(sourceEvent.correlationId()).thenReturn(correlationId);

        OutboxEventEntity event = factory.create(booking, sourceEvent, requestedAt);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.RESERVED);

        assertThat(event.getId()).isNotNull();

        assertThat(event.getId().version()).isEqualTo(7);

        assertThat(event.getId()).isNotEqualTo(sourceEventId);

        assertThat(event.getAggregateType()).isEqualTo(AggregateType.BOOKING);

        assertThat(event.getAggregateId()).isEqualTo(booking.getId());

        assertThat(event.getEventType()).isEqualTo(BookingEventContract.PAYMENT_REQUESTED);

        assertThat(event.getEventVersion())
                .isEqualTo(BookingEventContract.PAYMENT_REQUESTED_VERSION);

        assertThat(event.getTopic()).isEqualTo(BookingEventContract.PAYMENT_REQUESTED);

        assertThat(event.getPartitionKey()).isEqualTo(booking.getId().toString());

        assertThat(event.getOccurredAt()).isEqualTo(requestedAt);

        assertThat(event.getCorrelationId()).isEqualTo(correlationId);

        assertThat(event.getCausationId()).isEqualTo(sourceEventId);

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);

        assertThat(event.getRetryCount()).isZero();

        assertThat(event.getNextAttemptAt()).isEqualTo(requestedAt);

        assertThat(event.getCreatedAt()).isEqualTo(requestedAt);

        JsonNode payload = objectMapper.readTree(event.getPayload());

        assertThat(payload.size()).isEqualTo(7);

        assertThat(payload.get("bookingId").asText()).isEqualTo(booking.getId().toString());

        assertThat(payload.get("userId").asText()).isEqualTo(userId.toString());

        assertThat(payload.get("amount").decimalValue())
                .isEqualByComparingTo(new BigDecimal("180000.00"));

        assertThat(payload.get("currency").asText()).isEqualTo("VND");

        assertThat(payload.get("paymentAttempt").asInt()).isEqualTo(1);

        assertThat(OffsetDateTime.parse(payload.get("holdExpiresAt").asText()))
                .isEqualTo(holdExpiresAt);

        assertThat(OffsetDateTime.parse(payload.get("requestedAt").asText()))
                .isEqualTo(requestedAt);

        assertThat(payload.has("showtimeId")).isFalse();

        assertThat(payload.has("seats")).isFalse();

        assertThat(payload.has("provider")).isFalse();

        assertThat(payload.has("providerReference")).isFalse();

        assertThat(payload.has("cardNumber")).isFalse();

        assertThat(payload.has("cvv")).isFalse();
    }

    @Test
    void shouldPreserveSourceCorrelationAndCausation() {

        OffsetDateTime createdAt = OffsetDateTime.parse("2026-08-25T10:00:00Z");

        OffsetDateTime requestedAt = createdAt.plusMinutes(1);

        Booking booking = reservedBooking(createdAt);

        UUID sourceEventId = UuidGenerator.next();

        UUID correlationId = UuidGenerator.next();

        OutboxEventMessage sourceEvent = mock(OutboxEventMessage.class);

        when(sourceEvent.eventId()).thenReturn(sourceEventId);

        when(sourceEvent.correlationId()).thenReturn(correlationId);

        OutboxEventEntity event = factory.create(booking, sourceEvent, requestedAt);

        assertThat(event.getCorrelationId()).isEqualTo(correlationId);

        assertThat(event.getCausationId()).isEqualTo(sourceEventId);
    }

    @Test
    void serializationFailureShouldUseStableBookingError() throws Exception {

        ObjectMapper failingMapper = mock(ObjectMapper.class);

        when(failingMapper.writeValueAsString(any()))
                .thenThrow(new JsonProcessingException("Serialization failure") {});

        DefaultPaymentRequestedOutboxFactory failingFactory =
                new DefaultPaymentRequestedOutboxFactory(failingMapper);

        OffsetDateTime createdAt = OffsetDateTime.parse("2026-08-25T10:00:00Z");

        Booking booking = reservedBooking(createdAt);

        OutboxEventMessage sourceEvent = mock(OutboxEventMessage.class);

        when(sourceEvent.eventId()).thenReturn(UuidGenerator.next());

        when(sourceEvent.correlationId()).thenReturn(UuidGenerator.next());

        InternalServerException exception =
                assertThrows(
                        InternalServerException.class,
                        () ->
                                failingFactory.create(
                                        booking, sourceEvent, createdAt.plusMinutes(1)));

        assertThat(exception.getErrorCode().code())
                .isEqualTo("BOOKING_OUTBOX_PAYLOAD_SERIALIZATION_FAILED");
    }

    private static Booking reservedBooking(OffsetDateTime createdAt) {

        Booking booking =
                new Booking(
                        UuidGenerator.next(),
                        UuidGenerator.next(),
                        "request-1",
                        "a".repeat(64),
                        createdAt.plusMinutes(10),
                        createdAt);

        booking.reserve(new BigDecimal("180000.00"), "VND");

        return booking;
    }
}
