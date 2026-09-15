package com.cinema.inventory.event.payload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cinema.common.core.id.UuidGenerator;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

class BookingConfirmedPayloadTest {

    private static final OffsetDateTime CONFIRMED_AT =
            OffsetDateTime.of(2026, 9, 15, 10, 0, 0, 0, ZoneOffset.UTC);

    @Test
    void shouldCreateImmutableSeatSnapshot() {

        UUID bookingId = UuidGenerator.next();

        UUID userId = UuidGenerator.next();

        UUID showtimeId = UuidGenerator.next();

        UUID paymentId = UuidGenerator.next();

        List<ConfirmedSeatPayload> sourceSeats =
                new ArrayList<>(
                        List.of(
                                new ConfirmedSeatPayload(
                                        "H7", "STANDARD", new BigDecimal("90000.00")),
                                new ConfirmedSeatPayload(
                                        "H8", "VIP", new BigDecimal("120000.00"))));

        BookingConfirmedPayload payload =
                new BookingConfirmedPayload(
                        bookingId,
                        userId,
                        showtimeId,
                        paymentId,
                        sourceSeats,
                        new BigDecimal("210000.00"),
                        "VND",
                        CONFIRMED_AT);

        sourceSeats.add(new ConfirmedSeatPayload("H9", "STANDARD", new BigDecimal("90000.00")));

        assertThat(payload.bookingId()).isEqualTo(bookingId);

        assertThat(payload.userId()).isEqualTo(userId);

        assertThat(payload.showtimeId()).isEqualTo(showtimeId);

        assertThat(payload.paymentId()).isEqualTo(paymentId);

        assertThat(payload.seats())
                .containsExactly(
                        new ConfirmedSeatPayload("H7", "STANDARD", new BigDecimal("90000.00")),
                        new ConfirmedSeatPayload("H8", "VIP", new BigDecimal("120000.00")));

        assertThat(payload.totalAmount()).isEqualByComparingTo("210000.00");

        assertThat(payload.currency()).isEqualTo("VND");

        assertThat(payload.confirmedAt()).isEqualTo(CONFIRMED_AT);
    }

    @Test
    void shouldPreventMutationThroughPayloadSeatList() {

        BookingConfirmedPayload payload =
                new BookingConfirmedPayload(
                        UuidGenerator.next(),
                        UuidGenerator.next(),
                        UuidGenerator.next(),
                        UuidGenerator.next(),
                        List.of(
                                new ConfirmedSeatPayload(
                                        "H7", "STANDARD", new BigDecimal("90000.00"))),
                        new BigDecimal("90000.00"),
                        "VND",
                        CONFIRMED_AT);

        assertThatThrownBy(
                        () ->
                                payload.seats()
                                        .add(
                                                new ConfirmedSeatPayload(
                                                        "H8", "VIP", new BigDecimal("120000.00"))))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void nullSeatListShouldBecomeImmutableEmptyList() {

        BookingConfirmedPayload payload =
                new BookingConfirmedPayload(
                        UuidGenerator.next(),
                        UuidGenerator.next(),
                        UuidGenerator.next(),
                        UuidGenerator.next(),
                        null,
                        new BigDecimal("210000.00"),
                        "VND",
                        CONFIRMED_AT);

        assertThat(payload.seats()).isEmpty();

        assertThatThrownBy(
                        () ->
                                payload.seats()
                                        .add(
                                                new ConfirmedSeatPayload(
                                                        "H7",
                                                        "STANDARD",
                                                        new BigDecimal("90000.00"))))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void confirmedSeatPayloadShouldPreserveCanonicalValues() {

        ConfirmedSeatPayload seat =
                new ConfirmedSeatPayload("H7", "STANDARD", new BigDecimal("90000.00"));

        assertThat(seat.seatNumber()).isEqualTo("H7");

        assertThat(seat.seatType()).isEqualTo("STANDARD");

        assertThat(seat.price()).isEqualByComparingTo("90000.00");
    }
}
