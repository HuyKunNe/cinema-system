package com.cinema.booking.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cinema.booking.entity.Booking;
import com.cinema.booking.entity.BookingSeat;
import com.cinema.booking.exception.BookingErrorCode;
import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.ConflictException;
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
import java.util.List;
import java.util.UUID;

class DefaultBookingConfirmedOutboxFactoryTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-09-14T09:00:00Z");

    private static final OffsetDateTime CONFIRMED_AT = NOW.plusMinutes(1);

    private final ObjectMapper objectMapper = new JacksonConfiguration().objectMapper();

    private final DefaultBookingConfirmedOutboxFactory factory =
            new DefaultBookingConfirmedOutboxFactory(objectMapper);

    @Test
    void shouldCreateCanonicalBookingConfirmedOutboxEvent() throws Exception {

        Booking booking = confirmedBooking();

        UUID paymentId = UuidGenerator.next();
        UUID sourceEventId = UuidGenerator.next();
        UUID correlationId = UuidGenerator.next();

        OutboxEventMessage sourceEvent = sourceEvent(paymentId, sourceEventId, correlationId);

        List<BookingSeat> seats = completedSeats(booking);

        OutboxEventEntity event = factory.create(booking, seats, paymentId, sourceEvent);

        assertThat(event.getId()).isNotNull();

        assertThat(event.getId().version()).isEqualTo(7);

        assertThat(event.getId()).isNotEqualTo(sourceEventId);

        assertThat(event.getAggregateType()).isEqualTo(AggregateType.BOOKING);

        assertThat(event.getAggregateId()).isEqualTo(booking.getId());

        assertThat(event.getEventType()).isEqualTo(BookingEventContract.BOOKING_CONFIRMED);

        assertThat(event.getEventVersion())
                .isEqualTo(BookingEventContract.BOOKING_CONFIRMED_VERSION);

        assertThat(event.getTopic()).isEqualTo(BookingEventContract.BOOKING_CONFIRMED);

        assertThat(event.getPartitionKey()).isEqualTo(booking.getId().toString());

        assertThat(event.getOccurredAt()).isEqualTo(CONFIRMED_AT);

        assertThat(event.getCorrelationId()).isEqualTo(correlationId);

        assertThat(event.getCausationId()).isEqualTo(sourceEventId);

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);

        assertThat(event.getRetryCount()).isZero();

        assertThat(event.getNextAttemptAt()).isEqualTo(CONFIRMED_AT);

        assertThat(event.getCreatedAt()).isEqualTo(CONFIRMED_AT);

        JsonNode payload = objectMapper.readTree(event.getPayload());

        assertThat(payload.size()).isEqualTo(8);

        assertThat(payload.get("bookingId").asText()).isEqualTo(booking.getId().toString());

        assertThat(payload.get("userId").asText()).isEqualTo(booking.getUserId().toString());

        assertThat(payload.get("showtimeId").asText())
                .isEqualTo(booking.getShowtimeId().toString());

        assertThat(payload.get("paymentId").asText()).isEqualTo(paymentId.toString());

        assertThat(payload.get("totalAmount").decimalValue()).isEqualByComparingTo("180000.00");

        assertThat(payload.get("currency").asText()).isEqualTo("VND");

        assertThat(OffsetDateTime.parse(payload.get("confirmedAt").asText()))
                .isEqualTo(CONFIRMED_AT);

        JsonNode seatPayloads = payload.get("seats");

        assertThat(seatPayloads).hasSize(2);

        assertThat(seatPayloads.get(0).size()).isEqualTo(3);

        assertThat(seatPayloads.get(0).get("seatNumber").asText()).isEqualTo("H7");

        assertThat(seatPayloads.get(0).get("seatType").asText()).isEqualTo("STANDARD");

        assertThat(seatPayloads.get(0).get("price").decimalValue())
                .isEqualByComparingTo("90000.00");

        assertThat(seatPayloads.get(0).has("inventorySeatId")).isFalse();

        assertThat(seatPayloads.get(1).get("seatNumber").asText()).isEqualTo("H8");

        assertThat(payload.has("provider")).isFalse();

        assertThat(payload.has("providerReference")).isFalse();

        assertThat(payload.has("cardNumber")).isFalse();

        assertThat(payload.has("cvv")).isFalse();
    }

    @Test
    void nonConfirmedBookingShouldBeRejected() {

        Booking booking = reservedBooking();

        assertThatThrownBy(
                        () ->
                                factory.create(
                                        booking,
                                        completedSeats(booking),
                                        UuidGenerator.next(),
                                        mock(OutboxEventMessage.class)))
                .isInstanceOf(ConflictException.class)
                .satisfies(
                        throwable ->
                                assertThat(((ConflictException) throwable).getErrorCode())
                                        .isEqualTo(BookingErrorCode.BOOKING_NOT_CONFIRMED));
    }

    @Test
    void incompleteSeatSnapshotShouldBeRejected() {

        Booking booking = confirmedBooking();

        BookingSeat incompleteSeat =
                new BookingSeat(booking.getId(), booking.getShowtimeId(), "H7");

        assertThatThrownBy(
                        () ->
                                factory.create(
                                        booking,
                                        List.of(incompleteSeat),
                                        UuidGenerator.next(),
                                        mock(OutboxEventMessage.class)))
                .isInstanceOf(ConflictException.class)
                .satisfies(
                        throwable ->
                                assertThat(((ConflictException) throwable).getErrorCode())
                                        .isEqualTo(BookingErrorCode.RESERVATION_RESULT_MISMATCH));
    }

    @Test
    void serializationFailureShouldUseStableBookingError() throws Exception {

        ObjectMapper failingMapper = mock(ObjectMapper.class);

        when(failingMapper.writeValueAsString(any()))
                .thenThrow(new JsonProcessingException("Serialization failure") {});

        DefaultBookingConfirmedOutboxFactory failingFactory =
                new DefaultBookingConfirmedOutboxFactory(failingMapper);

        Booking booking = confirmedBooking();

        assertThatThrownBy(
                        () ->
                                failingFactory.create(
                                        booking,
                                        completedSeats(booking),
                                        UuidGenerator.next(),
                                        sourceEvent(
                                                UuidGenerator.next(),
                                                UuidGenerator.next(),
                                                UuidGenerator.next())))
                .isInstanceOf(InternalServerException.class)
                .satisfies(
                        throwable ->
                                assertThat(((InternalServerException) throwable).getErrorCode())
                                        .isEqualTo(
                                                BookingErrorCode
                                                        .OUTBOX_PAYLOAD_SERIALIZATION_FAILED));
    }

    private static Booking confirmedBooking() {

        Booking booking = reservedBooking();

        booking.confirm(CONFIRMED_AT);

        return booking;
    }

    private static Booking reservedBooking() {

        Booking booking =
                new Booking(
                        UuidGenerator.next(),
                        UuidGenerator.next(),
                        "booking-confirmed-request",
                        "a".repeat(64),
                        NOW.plusMinutes(10),
                        NOW);

        booking.reserve(new BigDecimal("180000.00"), "VND");

        return booking;
    }

    private static List<BookingSeat> completedSeats(Booking booking) {

        BookingSeat firstSeat = completedSeat(booking, "H7", "STANDARD", "90000.00");

        BookingSeat secondSeat = completedSeat(booking, "H8", "STANDARD", "90000.00");

        return List.of(firstSeat, secondSeat);
    }

    private static BookingSeat completedSeat(
            Booking booking, String seatNumber, String seatType, String price) {

        BookingSeat seat = new BookingSeat(booking.getId(), booking.getShowtimeId(), seatNumber);

        seat.completeSnapshot(UuidGenerator.next(), seatType, new BigDecimal(price));

        return seat;
    }

    private static OutboxEventMessage sourceEvent(
            UUID paymentId, UUID sourceEventId, UUID correlationId) {

        return new OutboxEventMessage(
                sourceEventId,
                paymentId,
                BookingEventContract.PAYMENT_AGGREGATE_TYPE,
                BookingEventContract.PAYMENT_SUCCEEDED,
                BookingEventContract.PAYMENT_SUCCEEDED_VERSION,
                CONFIRMED_AT.minusSeconds(1),
                BookingEventContract.PAYMENT_PRODUCER,
                correlationId,
                UuidGenerator.next(),
                com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode());
    }
}
