package com.cinema.inventory.event.serialization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.jackson.config.JacksonConfiguration;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.cinema.inventory.event.InventoryEventContract;
import com.cinema.inventory.event.payload.BookingCancelledPayload;
import com.cinema.inventory.event.payload.BookingExpiredPayload;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

class JacksonBookingLifecyclePayloadReaderTest {

    private static final OffsetDateTime CANCELLED_AT = OffsetDateTime.parse("2026-09-16T08:30:00Z");

    private static final OffsetDateTime EXPIRED_AT = OffsetDateTime.parse("2026-09-16T08:40:00Z");

    private final ObjectMapper objectMapper = new JacksonConfiguration().objectMapper();

    private final JacksonBookingCancelledPayloadReader cancelledReader =
            new JacksonBookingCancelledPayloadReader(objectMapper);

    private final JacksonBookingExpiredPayloadReader expiredReader =
            new JacksonBookingExpiredPayloadReader(objectMapper);

    @Test
    void shouldReadCanonicalBookingCancelledPayload() {

        UUID bookingId = UuidGenerator.next();

        UUID userId = UuidGenerator.next();

        UUID showtimeId = UuidGenerator.next();

        BookingCancelledPayload source =
                new BookingCancelledPayload(
                        bookingId,
                        userId,
                        showtimeId,
                        InventoryEventContract.BOOKING_CANCELLATION_REASON_USER_REQUESTED,
                        CANCELLED_AT);

        BookingCancelledPayload result =
                cancelledReader.read(
                        message(
                                bookingId,
                                InventoryEventContract.BOOKING_CANCELLED,
                                CANCELLED_AT,
                                objectMapper.valueToTree(source)));

        assertThat(result.bookingId()).isEqualTo(bookingId);

        assertThat(result.userId()).isEqualTo(userId);

        assertThat(result.showtimeId()).isEqualTo(showtimeId);

        assertThat(result.reason())
                .isEqualTo(InventoryEventContract.BOOKING_CANCELLATION_REASON_USER_REQUESTED);

        assertThat(result.cancelledAt()).isEqualTo(CANCELLED_AT);
    }

    @Test
    void shouldReadCanonicalBookingExpiredPayload() {

        UUID bookingId = UuidGenerator.next();

        UUID userId = UuidGenerator.next();

        UUID showtimeId = UuidGenerator.next();

        BookingExpiredPayload source =
                new BookingExpiredPayload(bookingId, userId, showtimeId, EXPIRED_AT);

        BookingExpiredPayload result =
                expiredReader.read(
                        message(
                                bookingId,
                                InventoryEventContract.BOOKING_EXPIRED,
                                EXPIRED_AT,
                                objectMapper.valueToTree(source)));

        assertThat(result.bookingId()).isEqualTo(bookingId);

        assertThat(result.userId()).isEqualTo(userId);

        assertThat(result.showtimeId()).isEqualTo(showtimeId);

        assertThat(result.expiredAt()).isEqualTo(EXPIRED_AT);
    }

    @Test
    void cancelledReaderShouldRejectInvalidPayloadShapes() {

        UUID bookingId = UuidGenerator.next();

        assertThatThrownBy(() -> cancelledReader.read(null))
                .isInstanceOf(ValidationException.class);

        assertThatThrownBy(
                        () ->
                                cancelledReader.read(
                                        message(
                                                bookingId,
                                                InventoryEventContract.BOOKING_CANCELLED,
                                                CANCELLED_AT,
                                                null)))
                .isInstanceOf(ValidationException.class);

        assertThatThrownBy(
                        () ->
                                cancelledReader.read(
                                        message(
                                                bookingId,
                                                InventoryEventContract.BOOKING_CANCELLED,
                                                CANCELLED_AT,
                                                JsonNodeFactory.instance.textNode("invalid"))))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void expiredReaderShouldRejectInvalidPayloadShapes() {

        UUID bookingId = UuidGenerator.next();

        assertThatThrownBy(() -> expiredReader.read(null)).isInstanceOf(ValidationException.class);

        assertThatThrownBy(
                        () ->
                                expiredReader.read(
                                        message(
                                                bookingId,
                                                InventoryEventContract.BOOKING_EXPIRED,
                                                EXPIRED_AT,
                                                null)))
                .isInstanceOf(ValidationException.class);

        assertThatThrownBy(
                        () ->
                                expiredReader.read(
                                        message(
                                                bookingId,
                                                InventoryEventContract.BOOKING_EXPIRED,
                                                EXPIRED_AT,
                                                JsonNodeFactory.instance.textNode("invalid"))))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void cancelledReaderShouldRejectMalformedUuidField() {

        UUID bookingId = UuidGenerator.next();

        ObjectNode payload = JsonNodeFactory.instance.objectNode();

        payload.put("bookingId", bookingId.toString());

        payload.put("userId", "not-a-uuid");

        payload.put("showtimeId", UuidGenerator.next().toString());

        payload.put("reason", InventoryEventContract.BOOKING_CANCELLATION_REASON_USER_REQUESTED);

        payload.put("cancelledAt", CANCELLED_AT.toString());

        assertThatThrownBy(
                        () ->
                                cancelledReader.read(
                                        message(
                                                bookingId,
                                                InventoryEventContract.BOOKING_CANCELLED,
                                                CANCELLED_AT,
                                                payload)))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void expiredReaderShouldRejectMalformedTimestampField() {

        UUID bookingId = UuidGenerator.next();

        ObjectNode payload = JsonNodeFactory.instance.objectNode();

        payload.put("bookingId", bookingId.toString());

        payload.put("userId", UuidGenerator.next().toString());

        payload.put("showtimeId", UuidGenerator.next().toString());

        payload.put("expiredAt", "not-a-timestamp");

        assertThatThrownBy(
                        () ->
                                expiredReader.read(
                                        message(
                                                bookingId,
                                                InventoryEventContract.BOOKING_EXPIRED,
                                                EXPIRED_AT,
                                                payload)))
                .isInstanceOf(ValidationException.class);
    }

    private OutboxEventMessage message(
            UUID bookingId, String eventType, OffsetDateTime occurredAt, JsonNode payload) {

        String eventVersion =
                InventoryEventContract.BOOKING_CANCELLED.equals(eventType)
                        ? InventoryEventContract.BOOKING_CANCELLED_VERSION
                        : InventoryEventContract.BOOKING_EXPIRED_VERSION;

        return new OutboxEventMessage(
                UuidGenerator.next(),
                bookingId,
                InventoryEventContract.BOOKING_AGGREGATE_TYPE,
                eventType,
                eventVersion,
                occurredAt,
                InventoryEventContract.BOOKING_PRODUCER,
                UuidGenerator.next(),
                null,
                payload);
    }
}
