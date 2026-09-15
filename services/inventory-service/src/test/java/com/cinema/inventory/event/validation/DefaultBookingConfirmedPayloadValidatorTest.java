package com.cinema.inventory.event.validation;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.cinema.inventory.event.InventoryEventContract;
import com.cinema.inventory.event.payload.BookingConfirmedPayload;
import com.cinema.inventory.event.payload.ConfirmedSeatPayload;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

class DefaultBookingConfirmedPayloadValidatorTest {

    private static final OffsetDateTime CONFIRMED_AT = OffsetDateTime.parse("2026-09-15T10:00:00Z");

    private static final BigDecimal FIRST_SEAT_PRICE = new BigDecimal("90000.00");

    private static final BigDecimal SECOND_SEAT_PRICE = new BigDecimal("120000.00");

    private static final BigDecimal TOTAL_AMOUNT = new BigDecimal("210000.00");

    private final DefaultBookingConfirmedPayloadValidator validator =
            new DefaultBookingConfirmedPayloadValidator();

    @Test
    void canonicalPayloadShouldPassValidation() {

        TestContext context = validContext();

        assertThatCode(
                        () ->
                                validator.validate(
                                        context.bookingId().toString(),
                                        context.message(),
                                        context.payload()))
                .doesNotThrowAnyException();
    }

    @Test
    void nullPayloadShouldBeRejected() {

        TestContext context = validContext();

        assertRejected(context.bookingId().toString(), context.message(), null);
    }

    @Test
    void nonUuidV7BookingIdShouldBeRejected() {

        TestContext context = validContext();

        BookingConfirmedPayload payload =
                copy(
                        context.payload(),
                        UUID.randomUUID(),
                        context.payload().userId(),
                        context.payload().showtimeId(),
                        context.payload().paymentId(),
                        context.payload().seats(),
                        context.payload().totalAmount(),
                        context.payload().currency(),
                        context.payload().confirmedAt());

        assertRejected(context.bookingId().toString(), context.message(), payload);
    }

    @Test
    void bookingIdDifferentFromAggregateIdShouldBeRejected() {

        TestContext context = validContext();

        BookingConfirmedPayload payload =
                copy(
                        context.payload(),
                        UuidGenerator.next(),
                        context.payload().userId(),
                        context.payload().showtimeId(),
                        context.payload().paymentId(),
                        context.payload().seats(),
                        context.payload().totalAmount(),
                        context.payload().currency(),
                        context.payload().confirmedAt());

        assertRejected(context.bookingId().toString(), context.message(), payload);
    }

    @Test
    void incorrectPartitionKeyShouldBeRejected() {

        TestContext context = validContext();

        assertRejected(UuidGenerator.next().toString(), context.message(), context.payload());
    }

    @Test
    void nonUuidV7UserIdShouldBeRejected() {

        TestContext context = validContext();

        BookingConfirmedPayload payload =
                copy(
                        context.payload(),
                        context.payload().bookingId(),
                        UUID.randomUUID(),
                        context.payload().showtimeId(),
                        context.payload().paymentId(),
                        context.payload().seats(),
                        context.payload().totalAmount(),
                        context.payload().currency(),
                        context.payload().confirmedAt());

        assertRejected(context.bookingId().toString(), context.message(), payload);
    }

    @Test
    void nonUuidV7ShowtimeIdShouldBeRejected() {

        TestContext context = validContext();

        BookingConfirmedPayload payload =
                copy(
                        context.payload(),
                        context.payload().bookingId(),
                        context.payload().userId(),
                        UUID.randomUUID(),
                        context.payload().paymentId(),
                        context.payload().seats(),
                        context.payload().totalAmount(),
                        context.payload().currency(),
                        context.payload().confirmedAt());

        assertRejected(context.bookingId().toString(), context.message(), payload);
    }

    @Test
    void nonUuidV7PaymentIdShouldBeRejected() {

        TestContext context = validContext();

        BookingConfirmedPayload payload =
                copy(
                        context.payload(),
                        context.payload().bookingId(),
                        context.payload().userId(),
                        context.payload().showtimeId(),
                        UUID.randomUUID(),
                        context.payload().seats(),
                        context.payload().totalAmount(),
                        context.payload().currency(),
                        context.payload().confirmedAt());

        assertRejected(context.bookingId().toString(), context.message(), payload);
    }

    @Test
    void emptySeatSetShouldBeRejected() {

        TestContext context = validContext();

        BookingConfirmedPayload payload =
                copy(
                        context.payload(),
                        context.payload().bookingId(),
                        context.payload().userId(),
                        context.payload().showtimeId(),
                        context.payload().paymentId(),
                        List.of(),
                        BigDecimal.ZERO,
                        context.payload().currency(),
                        context.payload().confirmedAt());

        assertRejected(context.bookingId().toString(), context.message(), payload);
    }

    @Test
    void duplicateSeatNumberShouldBeRejected() {

        TestContext context = validContext();

        BookingConfirmedPayload payload =
                copy(
                        context.payload(),
                        context.payload().bookingId(),
                        context.payload().userId(),
                        context.payload().showtimeId(),
                        context.payload().paymentId(),
                        List.of(
                                new ConfirmedSeatPayload("H7", "STANDARD", FIRST_SEAT_PRICE),
                                new ConfirmedSeatPayload("H7", "VIP", SECOND_SEAT_PRICE)),
                        TOTAL_AMOUNT,
                        context.payload().currency(),
                        context.payload().confirmedAt());

        assertRejected(context.bookingId().toString(), context.message(), payload);
    }

    @Test
    void nonCanonicalSeatNumberShouldBeRejected() {

        TestContext context = validContext();

        BookingConfirmedPayload payload =
                copy(
                        context.payload(),
                        context.payload().bookingId(),
                        context.payload().userId(),
                        context.payload().showtimeId(),
                        context.payload().paymentId(),
                        List.of(
                                new ConfirmedSeatPayload(" h7 ", "STANDARD", FIRST_SEAT_PRICE),
                                new ConfirmedSeatPayload("H8", "VIP", SECOND_SEAT_PRICE)),
                        TOTAL_AMOUNT,
                        context.payload().currency(),
                        context.payload().confirmedAt());

        assertRejected(context.bookingId().toString(), context.message(), payload);
    }

    @Test
    void unsupportedSeatTypeShouldBeRejected() {

        TestContext context = validContext();

        BookingConfirmedPayload payload =
                copy(
                        context.payload(),
                        context.payload().bookingId(),
                        context.payload().userId(),
                        context.payload().showtimeId(),
                        context.payload().paymentId(),
                        List.of(
                                new ConfirmedSeatPayload("H7", "PREMIUM", FIRST_SEAT_PRICE),
                                new ConfirmedSeatPayload("H8", "VIP", SECOND_SEAT_PRICE)),
                        TOTAL_AMOUNT,
                        context.payload().currency(),
                        context.payload().confirmedAt());

        assertRejected(context.bookingId().toString(), context.message(), payload);
    }

    @Test
    void negativeSeatPriceShouldBeRejected() {

        TestContext context = validContext();

        BookingConfirmedPayload payload =
                copy(
                        context.payload(),
                        context.payload().bookingId(),
                        context.payload().userId(),
                        context.payload().showtimeId(),
                        context.payload().paymentId(),
                        List.of(
                                new ConfirmedSeatPayload("H7", "STANDARD", new BigDecimal("-1.00")),
                                new ConfirmedSeatPayload("H8", "VIP", SECOND_SEAT_PRICE)),
                        new BigDecimal("119999.00"),
                        context.payload().currency(),
                        context.payload().confirmedAt());

        assertRejected(context.bookingId().toString(), context.message(), payload);
    }

    @Test
    void seatPriceWithUnsupportedScaleShouldBeRejected() {

        TestContext context = validContext();

        BookingConfirmedPayload payload =
                copy(
                        context.payload(),
                        context.payload().bookingId(),
                        context.payload().userId(),
                        context.payload().showtimeId(),
                        context.payload().paymentId(),
                        List.of(
                                new ConfirmedSeatPayload(
                                        "H7", "STANDARD", new BigDecimal("90000.001")),
                                new ConfirmedSeatPayload("H8", "VIP", SECOND_SEAT_PRICE)),
                        new BigDecimal("210000.001"),
                        context.payload().currency(),
                        context.payload().confirmedAt());

        assertRejected(context.bookingId().toString(), context.message(), payload);
    }

    @Test
    void totalDifferentFromSeatPriceSumShouldBeRejected() {

        TestContext context = validContext();

        BookingConfirmedPayload payload =
                copy(
                        context.payload(),
                        context.payload().bookingId(),
                        context.payload().userId(),
                        context.payload().showtimeId(),
                        context.payload().paymentId(),
                        context.payload().seats(),
                        new BigDecimal("200000.00"),
                        context.payload().currency(),
                        context.payload().confirmedAt());

        assertRejected(context.bookingId().toString(), context.message(), payload);
    }

    @Test
    void unsupportedCurrencyShouldBeRejected() {

        TestContext context = validContext();

        BookingConfirmedPayload payload =
                copy(
                        context.payload(),
                        context.payload().bookingId(),
                        context.payload().userId(),
                        context.payload().showtimeId(),
                        context.payload().paymentId(),
                        context.payload().seats(),
                        context.payload().totalAmount(),
                        "USD",
                        context.payload().confirmedAt());

        assertRejected(context.bookingId().toString(), context.message(), payload);
    }

    @Test
    void confirmedAtDifferentFromOccurredAtShouldBeRejected() {

        TestContext context = validContext();

        BookingConfirmedPayload payload =
                copy(
                        context.payload(),
                        context.payload().bookingId(),
                        context.payload().userId(),
                        context.payload().showtimeId(),
                        context.payload().paymentId(),
                        context.payload().seats(),
                        context.payload().totalAmount(),
                        context.payload().currency(),
                        CONFIRMED_AT.plusSeconds(1));

        assertRejected(context.bookingId().toString(), context.message(), payload);
    }

    private void assertRejected(
            String partitionKey, OutboxEventMessage message, BookingConfirmedPayload payload) {

        assertThatThrownBy(() -> validator.validate(partitionKey, message, payload))
                .isInstanceOf(ValidationException.class);
    }

    private TestContext validContext() {

        UUID bookingId = UuidGenerator.next();

        BookingConfirmedPayload payload =
                new BookingConfirmedPayload(
                        bookingId,
                        UuidGenerator.next(),
                        UuidGenerator.next(),
                        UuidGenerator.next(),
                        List.of(
                                new ConfirmedSeatPayload("H7", "STANDARD", FIRST_SEAT_PRICE),
                                new ConfirmedSeatPayload("H8", "VIP", SECOND_SEAT_PRICE)),
                        TOTAL_AMOUNT,
                        InventoryEventContract.CURRENCY_VND,
                        CONFIRMED_AT);

        OutboxEventMessage message =
                new OutboxEventMessage(
                        UuidGenerator.next(),
                        bookingId,
                        InventoryEventContract.BOOKING_AGGREGATE_TYPE,
                        InventoryEventContract.BOOKING_CONFIRMED,
                        InventoryEventContract.BOOKING_CONFIRMED_VERSION,
                        CONFIRMED_AT,
                        InventoryEventContract.BOOKING_PRODUCER,
                        UuidGenerator.next(),
                        UuidGenerator.next(),
                        JsonNodeFactory.instance.objectNode());

        return new TestContext(bookingId, message, payload);
    }

    private BookingConfirmedPayload copy(
            BookingConfirmedPayload source,
            UUID bookingId,
            UUID userId,
            UUID showtimeId,
            UUID paymentId,
            List<ConfirmedSeatPayload> seats,
            BigDecimal totalAmount,
            String currency,
            OffsetDateTime confirmedAt) {

        return new BookingConfirmedPayload(
                bookingId,
                userId,
                showtimeId,
                paymentId,
                seats,
                totalAmount,
                currency,
                confirmedAt);
    }

    private record TestContext(
            UUID bookingId, OutboxEventMessage message, BookingConfirmedPayload payload) {}
}
