package com.cinema.booking.event.serialization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cinema.booking.event.payload.SeatReservationRejectedPayload;
import com.cinema.booking.exception.BookingErrorCode;
import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.jackson.config.JacksonConfiguration;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

class JacksonSeatReservationRejectedPayloadReaderTest {

    private static final OffsetDateTime REJECTED_AT = OffsetDateTime.parse("2026-08-24T10:00:00Z");

    private final ObjectMapper objectMapper = new JacksonConfiguration().objectMapper();

    private final JacksonSeatReservationRejectedPayloadReader reader =
            new JacksonSeatReservationRejectedPayloadReader(objectMapper);

    @Test
    void canonicalPayloadShouldBeDeserialized() {

        UUID bookingId = UuidGenerator.next();

        UUID showtimeId = UuidGenerator.next();

        ObjectNode payload = JsonNodeFactory.instance.objectNode();

        payload.put("bookingId", bookingId.toString());

        payload.put("showtimeId", showtimeId.toString());

        payload.put("reasonCode", "SEAT_UNAVAILABLE");

        payload.put("message", "One or more requested seats are unavailable");

        payload.putArray("unavailableSeats").add("H7").add("H8");

        payload.put("rejectedAt", REJECTED_AT.toString());

        SeatReservationRejectedPayload result = reader.read(message(bookingId, payload));

        assertThat(result.bookingId()).isEqualTo(bookingId);

        assertThat(result.showtimeId()).isEqualTo(showtimeId);

        assertThat(result.reasonCode()).isEqualTo("SEAT_UNAVAILABLE");

        assertThat(result.message()).isEqualTo("One or more requested seats are unavailable");

        assertThat(result.unavailableSeats()).containsExactly("H7", "H8");

        assertThat(result.rejectedAt()).isEqualTo(REJECTED_AT);
    }

    @Test
    void missingPayloadShouldBeRejected() {

        assertInvalidPayload(null);
    }

    @Test
    void nullJsonPayloadShouldBeRejected() {

        assertInvalidPayload(JsonNodeFactory.instance.nullNode());
    }

    @Test
    void nonObjectPayloadShouldBeRejected() {

        assertInvalidPayload(JsonNodeFactory.instance.textNode("invalid-payload"));
    }

    @Test
    void invalidBookingIdShouldBeRejected() {

        ObjectNode payload = JsonNodeFactory.instance.objectNode();

        payload.put("bookingId", "not-a-uuid");

        payload.put("showtimeId", UuidGenerator.next().toString());

        payload.put("reasonCode", "SEAT_UNAVAILABLE");

        payload.put("message", "Seat unavailable");

        payload.putArray("unavailableSeats").add("H7");

        payload.put("rejectedAt", REJECTED_AT.toString());

        assertInvalidPayload(payload);
    }

    @Test
    void invalidTimestampShouldBeRejected() {

        ObjectNode payload = JsonNodeFactory.instance.objectNode();

        payload.put("bookingId", UuidGenerator.next().toString());

        payload.put("showtimeId", UuidGenerator.next().toString());

        payload.put("reasonCode", "SEAT_UNAVAILABLE");

        payload.put("message", "Seat unavailable");

        payload.putArray("unavailableSeats").add("H7");

        payload.put("rejectedAt", "not-a-timestamp");

        assertInvalidPayload(payload);
    }

    @Test
    void nullUnavailableSeatsShouldBecomeEmptyImmutableList() {

        UUID bookingId = UuidGenerator.next();

        ObjectNode payload = JsonNodeFactory.instance.objectNode();

        payload.put("bookingId", bookingId.toString());

        payload.put("showtimeId", UuidGenerator.next().toString());

        payload.put("reasonCode", "INVALID_REQUEST");

        payload.put("message", "Reservation request is invalid");

        payload.putNull("unavailableSeats");

        payload.put("rejectedAt", REJECTED_AT.toString());

        SeatReservationRejectedPayload result = reader.read(message(bookingId, payload));

        assertThat(result.unavailableSeats()).isEmpty();

        assertThatThrownBy(() -> result.unavailableSeats().add("H7"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private void assertInvalidPayload(JsonNode payload) {

        assertThatThrownBy(() -> reader.read(message(UuidGenerator.next(), payload)))
                .isInstanceOf(ValidationException.class)
                .satisfies(
                        throwable ->
                                assertThat(((ValidationException) throwable).getErrorCode())
                                        .isEqualTo(BookingErrorCode.EVENT_PAYLOAD_INVALID));
    }

    private OutboxEventMessage message(UUID bookingId, JsonNode payload) {

        return new OutboxEventMessage(
                UuidGenerator.next(),
                bookingId,
                "BOOKING",
                "seat-reservation-rejected",
                "1",
                REJECTED_AT,
                "inventory-service",
                UuidGenerator.next(),
                null,
                payload);
    }
}
