package com.cinema.booking.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.cinema.booking.entity.Booking;
import com.cinema.booking.enums.BookingCancellationReason;
import com.cinema.booking.event.payload.BookingCancelledPayload;
import com.cinema.booking.event.payload.BookingExpiredPayload;
import com.cinema.booking.exception.BookingErrorCode;
import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.InternalServerException;
import com.cinema.common.jackson.config.JacksonConfiguration;
import com.cinema.common.outbox.entity.OutboxEventEntity;
import com.cinema.common.outbox.enums.AggregateType;
import com.cinema.common.outbox.enums.OutboxStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

class BookingLifecycleOutboxFactoryTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-08-24T10:00:00Z");

    private static final OffsetDateTime EXPIRES_AT = OffsetDateTime.parse("2026-08-24T10:10:00Z");

    private final ObjectMapper objectMapper = new JacksonConfiguration().objectMapper();

    @Test
    void cancelledFactoryShouldCreateCanonicalOutboxEvent() throws Exception {

        Booking booking = reservedBooking();

        OffsetDateTime cancelledAt = NOW.plusMinutes(5);

        booking.cancel(cancelledAt);

        DefaultBookingCancelledOutboxFactory factory =
                new DefaultBookingCancelledOutboxFactory(objectMapper);

        OutboxEventEntity event = factory.create(booking, BookingCancellationReason.USER_REQUESTED);

        assertCommonEventMetadata(
                event,
                booking,
                BookingEventContract.BOOKING_CANCELLED,
                BookingEventContract.BOOKING_CANCELLED_VERSION,
                cancelledAt);

        assertThat(event.getCausationId()).isNull();

        JsonNode payload = objectMapper.readTree(event.getPayload());

        assertThat(payload.get("bookingId").asText()).isEqualTo(booking.getId().toString());

        assertThat(payload.get("userId").asText()).isEqualTo(booking.getUserId().toString());

        assertThat(payload.get("showtimeId").asText())
                .isEqualTo(booking.getShowtimeId().toString());

        assertThat(payload.get("reason").asText()).isEqualTo("USER_REQUESTED");

        assertThat(OffsetDateTime.parse(payload.get("cancelledAt").asText()))
                .isEqualTo(cancelledAt);

        assertThat(payload.has("email")).isFalse();

        assertThat(payload.has("exception")).isFalse();

        assertThat(payload.has("stackTrace")).isFalse();
    }

    @Test
    void expiredFactoryShouldCreateCanonicalOutboxEvent() throws Exception {

        Booking booking = reservedBooking();

        booking.expire(EXPIRES_AT);

        DefaultBookingExpiredOutboxFactory factory =
                new DefaultBookingExpiredOutboxFactory(objectMapper);

        OutboxEventEntity event = factory.create(booking, EXPIRES_AT);

        assertCommonEventMetadata(
                event,
                booking,
                BookingEventContract.BOOKING_EXPIRED,
                BookingEventContract.BOOKING_EXPIRED_VERSION,
                EXPIRES_AT);

        assertThat(event.getCausationId()).isNull();

        JsonNode payload = objectMapper.readTree(event.getPayload());

        assertThat(payload.get("bookingId").asText()).isEqualTo(booking.getId().toString());

        assertThat(payload.get("userId").asText()).isEqualTo(booking.getUserId().toString());

        assertThat(payload.get("showtimeId").asText())
                .isEqualTo(booking.getShowtimeId().toString());

        assertThat(OffsetDateTime.parse(payload.get("expiredAt").asText())).isEqualTo(EXPIRES_AT);

        assertThat(payload.has("reason")).isFalse();

        assertThat(payload.has("email")).isFalse();

        assertThat(payload.has("exception")).isFalse();

        assertThat(payload.has("stackTrace")).isFalse();
    }

    @Test
    void cancelledFactoryShouldTranslateSerializationFailure() throws Exception {

        ObjectMapper failingObjectMapper = org.mockito.Mockito.mock(ObjectMapper.class);

        JsonProcessingException cause = new JsonProcessingException("serialization failed") {};

        when(failingObjectMapper.writeValueAsString(any(BookingCancelledPayload.class)))
                .thenThrow(cause);

        Booking booking = reservedBooking();

        booking.cancel(NOW.plusMinutes(5));

        DefaultBookingCancelledOutboxFactory factory =
                new DefaultBookingCancelledOutboxFactory(failingObjectMapper);

        assertThatThrownBy(() -> factory.create(booking, BookingCancellationReason.USER_REQUESTED))
                .isInstanceOf(InternalServerException.class)
                .satisfies(
                        throwable -> {
                            InternalServerException exception = (InternalServerException) throwable;

                            assertThat(exception.getErrorCode())
                                    .isEqualTo(
                                            BookingErrorCode.OUTBOX_PAYLOAD_SERIALIZATION_FAILED);

                            assertThat(exception.getCause()).isSameAs(cause);
                        });
    }

    @Test
    void expiredFactoryShouldTranslateSerializationFailure() throws Exception {

        ObjectMapper failingObjectMapper = org.mockito.Mockito.mock(ObjectMapper.class);

        JsonProcessingException cause = new JsonProcessingException("serialization failed") {};

        when(failingObjectMapper.writeValueAsString(any(BookingExpiredPayload.class)))
                .thenThrow(cause);

        Booking booking = reservedBooking();

        booking.expire(EXPIRES_AT);

        DefaultBookingExpiredOutboxFactory factory =
                new DefaultBookingExpiredOutboxFactory(failingObjectMapper);

        assertThatThrownBy(() -> factory.create(booking, EXPIRES_AT))
                .isInstanceOf(InternalServerException.class)
                .satisfies(
                        throwable -> {
                            InternalServerException exception = (InternalServerException) throwable;

                            assertThat(exception.getErrorCode())
                                    .isEqualTo(
                                            BookingErrorCode.OUTBOX_PAYLOAD_SERIALIZATION_FAILED);

                            assertThat(exception.getCause()).isSameAs(cause);
                        });
    }

    private void assertCommonEventMetadata(
            OutboxEventEntity event,
            Booking booking,
            String eventType,
            String eventVersion,
            OffsetDateTime occurredAt) {

        assertThat(event.getId()).isNotNull();

        assertThat(event.getId().version()).isEqualTo(7);

        assertThat(event.getAggregateType()).isEqualTo(AggregateType.BOOKING);

        assertThat(event.getAggregateId()).isEqualTo(booking.getId());

        assertThat(event.getEventType()).isEqualTo(eventType);

        assertThat(event.getEventVersion()).isEqualTo(eventVersion);

        assertThat(event.getTopic()).isEqualTo(eventType);

        assertThat(event.getPartitionKey()).isEqualTo(booking.getId().toString());

        assertThat(event.getOccurredAt()).isEqualTo(occurredAt);

        assertThat(event.getCorrelationId()).isNotNull();

        assertThat(event.getCorrelationId().version()).isEqualTo(7);

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);

        assertThat(event.getRetryCount()).isZero();

        assertThat(event.getNextAttemptAt()).isEqualTo(occurredAt);

        assertThat(event.getCreatedAt()).isEqualTo(occurredAt);

        assertThat(event.getPayload()).isNotBlank();
    }

    private Booking reservedBooking() {

        Booking booking =
                new Booking(
                        UuidGenerator.next(),
                        UuidGenerator.next(),
                        "lifecycle-outbox-" + UuidGenerator.next(),
                        "a".repeat(64),
                        EXPIRES_AT,
                        NOW);

        booking.reserve(new BigDecimal("90000.00"), "VND");

        return booking;
    }
}
