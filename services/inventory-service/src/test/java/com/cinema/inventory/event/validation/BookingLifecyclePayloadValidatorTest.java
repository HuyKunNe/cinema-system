package com.cinema.inventory.event.validation;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.cinema.inventory.event.InventoryEventContract;
import com.cinema.inventory.event.payload.BookingCancelledPayload;
import com.cinema.inventory.event.payload.BookingExpiredPayload;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

class BookingLifecyclePayloadValidatorTest {

    private static final OffsetDateTime CANCELLED_AT = OffsetDateTime.parse("2026-09-16T08:30:00Z");

    private static final OffsetDateTime EXPIRED_AT = OffsetDateTime.parse("2026-09-16T08:40:00Z");

    private final DefaultBookingCancelledPayloadValidator cancelledValidator =
            new DefaultBookingCancelledPayloadValidator();

    private final DefaultBookingExpiredPayloadValidator expiredValidator =
            new DefaultBookingExpiredPayloadValidator();

    @Test
    void canonicalCancelledPayloadShouldPass() {

        CancelledContext context = cancelledContext();

        assertThatCode(
                        () ->
                                cancelledValidator.validate(
                                        context.bookingId().toString(),
                                        context.message(),
                                        context.payload()))
                .doesNotThrowAnyException();
    }

    @Test
    void canonicalExpiredPayloadShouldPass() {

        ExpiredContext context = expiredContext();

        assertThatCode(
                        () ->
                                expiredValidator.validate(
                                        context.bookingId().toString(),
                                        context.message(),
                                        context.payload()))
                .doesNotThrowAnyException();
    }

    @Test
    void nullPayloadsShouldBeRejected() {

        CancelledContext cancelled = cancelledContext();

        ExpiredContext expired = expiredContext();

        assertCancelledRejected(cancelled.bookingId().toString(), cancelled.message(), null);

        assertExpiredRejected(expired.bookingId().toString(), expired.message(), null);
    }

    @Test
    void bookingIdDifferentFromAggregateShouldBeRejected() {

        CancelledContext cancelled = cancelledContext();

        UUID differentCancelledBookingId = UuidGenerator.next();

        BookingCancelledPayload cancelledPayload =
                new BookingCancelledPayload(
                        differentCancelledBookingId,
                        cancelled.payload().userId(),
                        cancelled.payload().showtimeId(),
                        cancelled.payload().reason(),
                        cancelled.payload().cancelledAt());

        assertCancelledRejected(
                differentCancelledBookingId.toString(), cancelled.message(), cancelledPayload);

        ExpiredContext expired = expiredContext();

        UUID differentExpiredBookingId = UuidGenerator.next();

        BookingExpiredPayload expiredPayload =
                new BookingExpiredPayload(
                        differentExpiredBookingId,
                        expired.payload().userId(),
                        expired.payload().showtimeId(),
                        expired.payload().expiredAt());

        assertExpiredRejected(
                differentExpiredBookingId.toString(), expired.message(), expiredPayload);
    }

    @Test
    void incorrectPartitionKeysShouldBeRejected() {

        CancelledContext cancelled = cancelledContext();

        ExpiredContext expired = expiredContext();

        assertCancelledRejected(
                UuidGenerator.next().toString(), cancelled.message(), cancelled.payload());

        assertExpiredRejected(
                UuidGenerator.next().toString(), expired.message(), expired.payload());
    }

    @Test
    void nonUuidV7UserIdsShouldBeRejected() {

        CancelledContext cancelled = cancelledContext();

        BookingCancelledPayload cancelledPayload =
                new BookingCancelledPayload(
                        cancelled.payload().bookingId(),
                        UUID.randomUUID(),
                        cancelled.payload().showtimeId(),
                        cancelled.payload().reason(),
                        cancelled.payload().cancelledAt());

        assertCancelledRejected(
                cancelled.bookingId().toString(), cancelled.message(), cancelledPayload);

        ExpiredContext expired = expiredContext();

        BookingExpiredPayload expiredPayload =
                new BookingExpiredPayload(
                        expired.payload().bookingId(),
                        UUID.randomUUID(),
                        expired.payload().showtimeId(),
                        expired.payload().expiredAt());

        assertExpiredRejected(expired.bookingId().toString(), expired.message(), expiredPayload);
    }

    @Test
    void nonUuidV7ShowtimeIdsShouldBeRejected() {

        CancelledContext cancelled = cancelledContext();

        BookingCancelledPayload cancelledPayload =
                new BookingCancelledPayload(
                        cancelled.payload().bookingId(),
                        cancelled.payload().userId(),
                        UUID.randomUUID(),
                        cancelled.payload().reason(),
                        cancelled.payload().cancelledAt());

        assertCancelledRejected(
                cancelled.bookingId().toString(), cancelled.message(), cancelledPayload);

        ExpiredContext expired = expiredContext();

        BookingExpiredPayload expiredPayload =
                new BookingExpiredPayload(
                        expired.payload().bookingId(),
                        expired.payload().userId(),
                        UUID.randomUUID(),
                        expired.payload().expiredAt());

        assertExpiredRejected(expired.bookingId().toString(), expired.message(), expiredPayload);
    }

    @Test
    void unsupportedCancellationReasonShouldBeRejected() {

        CancelledContext context = cancelledContext();

        BookingCancelledPayload payload =
                new BookingCancelledPayload(
                        context.payload().bookingId(),
                        context.payload().userId(),
                        context.payload().showtimeId(),
                        "ADMINISTRATIVE",
                        context.payload().cancelledAt());

        assertCancelledRejected(context.bookingId().toString(), context.message(), payload);
    }

    @Test
    void nonCanonicalCancellationReasonShouldBeRejected() {

        CancelledContext context = cancelledContext();

        BookingCancelledPayload payload =
                new BookingCancelledPayload(
                        context.payload().bookingId(),
                        context.payload().userId(),
                        context.payload().showtimeId(),
                        " user_requested ",
                        context.payload().cancelledAt());

        assertCancelledRejected(context.bookingId().toString(), context.message(), payload);
    }

    @Test
    void cancelledAtDifferentFromOccurredAtShouldBeRejected() {

        CancelledContext context = cancelledContext();

        BookingCancelledPayload payload =
                new BookingCancelledPayload(
                        context.payload().bookingId(),
                        context.payload().userId(),
                        context.payload().showtimeId(),
                        context.payload().reason(),
                        CANCELLED_AT.plusSeconds(1));

        assertCancelledRejected(context.bookingId().toString(), context.message(), payload);
    }

    @Test
    void expiredAtDifferentFromOccurredAtShouldBeRejected() {

        ExpiredContext context = expiredContext();

        BookingExpiredPayload payload =
                new BookingExpiredPayload(
                        context.payload().bookingId(),
                        context.payload().userId(),
                        context.payload().showtimeId(),
                        EXPIRED_AT.plusSeconds(1));

        assertExpiredRejected(context.bookingId().toString(), context.message(), payload);
    }

    private void assertCancelledRejected(
            String partitionKey, OutboxEventMessage message, BookingCancelledPayload payload) {

        assertThatThrownBy(() -> cancelledValidator.validate(partitionKey, message, payload))
                .isInstanceOf(ValidationException.class);
    }

    private void assertExpiredRejected(
            String partitionKey, OutboxEventMessage message, BookingExpiredPayload payload) {

        assertThatThrownBy(() -> expiredValidator.validate(partitionKey, message, payload))
                .isInstanceOf(ValidationException.class);
    }

    private CancelledContext cancelledContext() {

        UUID bookingId = UuidGenerator.next();

        BookingCancelledPayload payload =
                new BookingCancelledPayload(
                        bookingId,
                        UuidGenerator.next(),
                        UuidGenerator.next(),
                        InventoryEventContract.BOOKING_CANCELLATION_REASON_USER_REQUESTED,
                        CANCELLED_AT);

        OutboxEventMessage message =
                lifecycleMessage(
                        bookingId,
                        InventoryEventContract.BOOKING_CANCELLED,
                        InventoryEventContract.BOOKING_CANCELLED_VERSION,
                        CANCELLED_AT);

        return new CancelledContext(bookingId, message, payload);
    }

    private ExpiredContext expiredContext() {

        UUID bookingId = UuidGenerator.next();

        BookingExpiredPayload payload =
                new BookingExpiredPayload(
                        bookingId, UuidGenerator.next(), UuidGenerator.next(), EXPIRED_AT);

        OutboxEventMessage message =
                lifecycleMessage(
                        bookingId,
                        InventoryEventContract.BOOKING_EXPIRED,
                        InventoryEventContract.BOOKING_EXPIRED_VERSION,
                        EXPIRED_AT);

        return new ExpiredContext(bookingId, message, payload);
    }

    private OutboxEventMessage lifecycleMessage(
            UUID bookingId, String eventType, String eventVersion, OffsetDateTime occurredAt) {

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
                JsonNodeFactory.instance.objectNode());
    }

    private record CancelledContext(
            UUID bookingId, OutboxEventMessage message, BookingCancelledPayload payload) {}

    private record ExpiredContext(
            UUID bookingId, OutboxEventMessage message, BookingExpiredPayload payload) {}
}
