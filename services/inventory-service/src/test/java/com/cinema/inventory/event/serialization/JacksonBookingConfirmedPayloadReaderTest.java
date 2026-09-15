package com.cinema.inventory.event.serialization;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.jackson.config.JacksonConfiguration;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.cinema.inventory.event.InventoryEventContract;
import com.cinema.inventory.event.payload.BookingConfirmedPayload;
import com.cinema.inventory.event.payload.ConfirmedSeatPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

class JacksonBookingConfirmedPayloadReaderTest {

    private static final OffsetDateTime CONFIRMED_AT = OffsetDateTime.parse("2026-09-15T10:00:00Z");

    private final ObjectMapper objectMapper = new JacksonConfiguration().objectMapper();

    private final JacksonBookingConfirmedPayloadReader reader =
            new JacksonBookingConfirmedPayloadReader(objectMapper);

    @Test
    void shouldReadCanonicalBookingConfirmedPayload() {

        UUID bookingId = UuidGenerator.next();

        UUID userId = UuidGenerator.next();

        UUID showtimeId = UuidGenerator.next();

        UUID paymentId = UuidGenerator.next();

        BookingConfirmedPayload source =
                new BookingConfirmedPayload(
                        bookingId,
                        userId,
                        showtimeId,
                        paymentId,
                        List.of(
                                new ConfirmedSeatPayload(
                                        "H7", "STANDARD", new BigDecimal("90000.00")),
                                new ConfirmedSeatPayload("H8", "VIP", new BigDecimal("120000.00"))),
                        new BigDecimal("210000.00"),
                        "VND",
                        CONFIRMED_AT);

        OutboxEventMessage message = message(bookingId, objectMapper.valueToTree(source));

        BookingConfirmedPayload result = reader.read(message);

        assertThat(result.bookingId()).isEqualTo(bookingId);

        assertThat(result.userId()).isEqualTo(userId);

        assertThat(result.showtimeId()).isEqualTo(showtimeId);

        assertThat(result.paymentId()).isEqualTo(paymentId);

        assertThat(result.seats()).hasSize(2);

        ConfirmedSeatPayload firstSeat = result.seats().get(0);

        assertThat(firstSeat.seatNumber()).isEqualTo("H7");

        assertThat(firstSeat.seatType()).isEqualTo("STANDARD");

        assertThat(firstSeat.price()).isEqualByComparingTo("90000.00");

        ConfirmedSeatPayload secondSeat = result.seats().get(1);

        assertThat(secondSeat.seatNumber()).isEqualTo("H8");

        assertThat(secondSeat.seatType()).isEqualTo("VIP");

        assertThat(secondSeat.price()).isEqualByComparingTo("120000.00");

        assertThat(result.totalAmount()).isEqualByComparingTo("210000.00");

        assertThat(result.currency()).isEqualTo("VND");

        assertThat(result.confirmedAt()).isEqualTo(CONFIRMED_AT);
    }

    @Test
    void nullMessageShouldBeRejected() {

        assertThatThrownBy(() -> reader.read(null)).isInstanceOf(ValidationException.class);
    }

    @Test
    void nullPayloadShouldBeRejected() {

        OutboxEventMessage message = message(UuidGenerator.next(), null);

        assertThatThrownBy(() -> reader.read(message)).isInstanceOf(ValidationException.class);
    }

    @Test
    void nonObjectPayloadShouldBeRejected() {

        OutboxEventMessage message =
                message(UuidGenerator.next(), JsonNodeFactory.instance.textNode("invalid"));

        assertThatThrownBy(() -> reader.read(message)).isInstanceOf(ValidationException.class);
    }

    @Test
    void malformedPayloadFieldShouldBeRejected() {

        UUID bookingId = UuidGenerator.next();

        ObjectNode payload = JsonNodeFactory.instance.objectNode();

        payload.put("bookingId", bookingId.toString());

        /*
         * Invalid UUID text must fail during Jackson deserialization.
         */
        payload.put("userId", "not-a-uuid");

        payload.put("showtimeId", UuidGenerator.next().toString());

        payload.put("paymentId", UuidGenerator.next().toString());

        payload.putArray("seats")
                .addObject()
                .put("seatNumber", "H7")
                .put("seatType", "STANDARD")
                .put("price", 90000);

        payload.put("totalAmount", 90000);

        payload.put("currency", "VND");

        payload.put("confirmedAt", CONFIRMED_AT.toString());

        OutboxEventMessage message = message(bookingId, payload);

        assertThatThrownBy(() -> reader.read(message)).isInstanceOf(ValidationException.class);
    }

    private OutboxEventMessage message(
            UUID bookingId, com.fasterxml.jackson.databind.JsonNode payload) {

        return new OutboxEventMessage(
                UuidGenerator.next(),
                bookingId,
                InventoryEventContract.BOOKING_AGGREGATE_TYPE,
                InventoryEventContract.BOOKING_CONFIRMED,
                InventoryEventContract.BOOKING_CONFIRMED_VERSION,
                CONFIRMED_AT,
                InventoryEventContract.BOOKING_PRODUCER,
                UuidGenerator.next(),
                UuidGenerator.next(),
                payload);
    }
}
