package com.cinema.inventory.event.validation;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.cinema.inventory.event.InventoryEventContract;
import com.cinema.inventory.event.payload.SeatReleaseRequestedPayload;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

class DefaultSeatReleaseRequestedPayloadValidatorTest {

    private static final OffsetDateTime REQUESTED_AT = OffsetDateTime.parse("2026-09-15T10:00:00Z");

    private final DefaultSeatReleaseRequestedPayloadValidator validator =
            new DefaultSeatReleaseRequestedPayloadValidator();

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

        SeatReleaseRequestedPayload payload =
                copy(
                        context.payload(),
                        UUID.randomUUID(),
                        context.payload().showtimeId(),
                        context.payload().seatIds(),
                        context.payload().reason(),
                        context.payload().requestedAt());

        assertRejected(context.bookingId().toString(), context.message(), payload);
    }

    @Test
    void bookingIdDifferentFromAggregateIdShouldBeRejected() {

        TestContext context = validContext();

        UUID differentBookingId = UuidGenerator.next();

        SeatReleaseRequestedPayload payload =
                copy(
                        context.payload(),
                        differentBookingId,
                        context.payload().showtimeId(),
                        context.payload().seatIds(),
                        context.payload().reason(),
                        context.payload().requestedAt());

        assertRejected(differentBookingId.toString(), context.message(), payload);
    }

    @Test
    void incorrectPartitionKeyShouldBeRejected() {

        TestContext context = validContext();

        assertRejected(UuidGenerator.next().toString(), context.message(), context.payload());
    }

    @Test
    void nonUuidV7ShowtimeIdShouldBeRejected() {

        TestContext context = validContext();

        SeatReleaseRequestedPayload payload =
                copy(
                        context.payload(),
                        context.payload().bookingId(),
                        UUID.randomUUID(),
                        context.payload().seatIds(),
                        context.payload().reason(),
                        context.payload().requestedAt());

        assertRejected(context.bookingId().toString(), context.message(), payload);
    }

    @Test
    void emptySeatIdSetShouldBeRejected() {

        TestContext context = validContext();

        SeatReleaseRequestedPayload payload =
                copy(
                        context.payload(),
                        context.payload().bookingId(),
                        context.payload().showtimeId(),
                        List.of(),
                        context.payload().reason(),
                        context.payload().requestedAt());

        assertRejected(context.bookingId().toString(), context.message(), payload);
    }

    @Test
    void duplicateSeatIdShouldBeRejected() {

        TestContext context = validContext();

        UUID seatId = UuidGenerator.next();

        SeatReleaseRequestedPayload payload =
                copy(
                        context.payload(),
                        context.payload().bookingId(),
                        context.payload().showtimeId(),
                        List.of(seatId, seatId),
                        context.payload().reason(),
                        context.payload().requestedAt());

        assertRejected(context.bookingId().toString(), context.message(), payload);
    }

    @Test
    void nonUuidV7SeatIdShouldBeRejected() {

        TestContext context = validContext();

        SeatReleaseRequestedPayload payload =
                copy(
                        context.payload(),
                        context.payload().bookingId(),
                        context.payload().showtimeId(),
                        List.of(UUID.randomUUID()),
                        context.payload().reason(),
                        context.payload().requestedAt());

        assertRejected(context.bookingId().toString(), context.message(), payload);
    }

    @Test
    void unsupportedReasonShouldBeRejected() {

        TestContext context = validContext();

        SeatReleaseRequestedPayload payload =
                copy(
                        context.payload(),
                        context.payload().bookingId(),
                        context.payload().showtimeId(),
                        context.payload().seatIds(),
                        "CANCELLED",
                        context.payload().requestedAt());

        assertRejected(context.bookingId().toString(), context.message(), payload);
    }

    @Test
    void nonCanonicalReasonShouldBeRejected() {

        TestContext context = validContext();

        SeatReleaseRequestedPayload payload =
                copy(
                        context.payload(),
                        context.payload().bookingId(),
                        context.payload().showtimeId(),
                        context.payload().seatIds(),
                        " payment_failed ",
                        context.payload().requestedAt());

        assertRejected(context.bookingId().toString(), context.message(), payload);
    }

    @Test
    void requestedAtDifferentFromOccurredAtShouldBeRejected() {

        TestContext context = validContext();

        SeatReleaseRequestedPayload payload =
                copy(
                        context.payload(),
                        context.payload().bookingId(),
                        context.payload().showtimeId(),
                        context.payload().seatIds(),
                        context.payload().reason(),
                        REQUESTED_AT.plusSeconds(1));

        assertRejected(context.bookingId().toString(), context.message(), payload);
    }

    private void assertRejected(
            String partitionKey, OutboxEventMessage message, SeatReleaseRequestedPayload payload) {

        assertThatThrownBy(() -> validator.validate(partitionKey, message, payload))
                .isInstanceOf(ValidationException.class);
    }

    private TestContext validContext() {

        UUID bookingId = UuidGenerator.next();

        SeatReleaseRequestedPayload payload =
                new SeatReleaseRequestedPayload(
                        bookingId,
                        UuidGenerator.next(),
                        List.of(UuidGenerator.next(), UuidGenerator.next()),
                        InventoryEventContract.SEAT_RELEASE_REASON_PAYMENT_FAILED,
                        REQUESTED_AT);

        OutboxEventMessage message =
                new OutboxEventMessage(
                        UuidGenerator.next(),
                        bookingId,
                        InventoryEventContract.BOOKING_AGGREGATE_TYPE,
                        InventoryEventContract.SEAT_RELEASE_REQUESTED,
                        InventoryEventContract.SEAT_RELEASE_REQUESTED_VERSION,
                        REQUESTED_AT,
                        InventoryEventContract.BOOKING_PRODUCER,
                        UuidGenerator.next(),
                        UuidGenerator.next(),
                        JsonNodeFactory.instance.objectNode());

        return new TestContext(bookingId, message, payload);
    }

    private SeatReleaseRequestedPayload copy(
            SeatReleaseRequestedPayload source,
            UUID bookingId,
            UUID showtimeId,
            List<UUID> seatIds,
            String reason,
            OffsetDateTime requestedAt) {

        return new SeatReleaseRequestedPayload(bookingId, showtimeId, seatIds, reason, requestedAt);
    }

    private record TestContext(
            UUID bookingId, OutboxEventMessage message, SeatReleaseRequestedPayload payload) {}
}
