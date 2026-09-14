package com.cinema.booking.event.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cinema.booking.event.BookingEventContract;
import com.cinema.booking.exception.BookingErrorCode;
import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

class DefaultPaymentResultMessageValidatorTest {

    private static final OffsetDateTime OCCURRED_AT = OffsetDateTime.parse("2026-09-14T08:00:00Z");

    private final DefaultPaymentResultMessageValidator validator =
            new DefaultPaymentResultMessageValidator();

    @Test
    void canonicalPaymentSucceededEnvelopeShouldPass() {

        UUID bookingId = UuidGenerator.next();

        OutboxEventMessage message =
                validMessage(
                        BookingEventContract.PAYMENT_SUCCEEDED,
                        BookingEventContract.PAYMENT_SUCCEEDED_VERSION);

        assertThatCode(
                        () ->
                                validator.validateSucceeded(
                                        bookingId.toString(), withBookingId(message, bookingId)))
                .doesNotThrowAnyException();
    }

    @Test
    void canonicalPaymentFailedEnvelopeShouldPass() {

        UUID bookingId = UuidGenerator.next();

        OutboxEventMessage message =
                validMessage(
                        BookingEventContract.PAYMENT_FAILED,
                        BookingEventContract.PAYMENT_FAILED_VERSION);

        assertThatCode(
                        () ->
                                validator.validateFailed(
                                        bookingId.toString(), withBookingId(message, bookingId)))
                .doesNotThrowAnyException();
    }

    @Test
    void succeededValidatorShouldRejectFailedEventType() {

        UUID bookingId = UuidGenerator.next();

        OutboxEventMessage message =
                withBookingId(
                        validMessage(
                                BookingEventContract.PAYMENT_FAILED,
                                BookingEventContract.PAYMENT_FAILED_VERSION),
                        bookingId);

        assertValidation(
                () -> validator.validateSucceeded(bookingId.toString(), message),
                BookingErrorCode.EVENT_TYPE_INVALID);
    }

    @Test
    void failedValidatorShouldRejectSucceededEventType() {

        UUID bookingId = UuidGenerator.next();

        OutboxEventMessage message =
                withBookingId(
                        validMessage(
                                BookingEventContract.PAYMENT_SUCCEEDED,
                                BookingEventContract.PAYMENT_SUCCEEDED_VERSION),
                        bookingId);

        assertValidation(
                () -> validator.validateFailed(bookingId.toString(), message),
                BookingErrorCode.EVENT_TYPE_INVALID);
    }

    @Test
    void nonPaymentAggregateShouldBeRejected() {

        UUID bookingId = UuidGenerator.next();

        OutboxEventMessage source =
                withBookingId(
                        validMessage(
                                BookingEventContract.PAYMENT_SUCCEEDED,
                                BookingEventContract.PAYMENT_SUCCEEDED_VERSION),
                        bookingId);

        OutboxEventMessage message =
                copy(
                        source,
                        source.eventId(),
                        source.aggregateId(),
                        "BOOKING",
                        source.eventType(),
                        source.eventVersion(),
                        source.occurredAt(),
                        source.producer(),
                        source.correlationId(),
                        source.causationId(),
                        source.payload());

        assertValidation(
                () -> validator.validateSucceeded(bookingId.toString(), message),
                BookingErrorCode.EVENT_AGGREGATE_INVALID);
    }

    @Test
    void unexpectedProducerShouldBeRejected() {

        UUID bookingId = UuidGenerator.next();

        OutboxEventMessage source =
                withBookingId(
                        validMessage(
                                BookingEventContract.PAYMENT_SUCCEEDED,
                                BookingEventContract.PAYMENT_SUCCEEDED_VERSION),
                        bookingId);

        OutboxEventMessage message =
                copy(
                        source,
                        source.eventId(),
                        source.aggregateId(),
                        source.aggregateType(),
                        source.eventType(),
                        source.eventVersion(),
                        source.occurredAt(),
                        "booking-service",
                        source.correlationId(),
                        source.causationId(),
                        source.payload());

        assertValidation(
                () -> validator.validateSucceeded(bookingId.toString(), message),
                BookingErrorCode.EVENT_PRODUCER_INVALID);
    }

    @Test
    void missingCausationIdShouldBeRejected() {

        UUID bookingId = UuidGenerator.next();

        OutboxEventMessage source =
                withBookingId(
                        validMessage(
                                BookingEventContract.PAYMENT_SUCCEEDED,
                                BookingEventContract.PAYMENT_SUCCEEDED_VERSION),
                        bookingId);

        OutboxEventMessage message =
                copy(
                        source,
                        source.eventId(),
                        source.aggregateId(),
                        source.aggregateType(),
                        source.eventType(),
                        source.eventVersion(),
                        source.occurredAt(),
                        source.producer(),
                        source.correlationId(),
                        null,
                        source.payload());

        assertValidation(
                () -> validator.validateSucceeded(bookingId.toString(), message),
                BookingErrorCode.EVENT_CAUSATION_ID_REQUIRED);
    }

    @Test
    void nonUuidV7CausationIdShouldBeRejected() {

        UUID bookingId = UuidGenerator.next();

        OutboxEventMessage source =
                withBookingId(
                        validMessage(
                                BookingEventContract.PAYMENT_SUCCEEDED,
                                BookingEventContract.PAYMENT_SUCCEEDED_VERSION),
                        bookingId);

        OutboxEventMessage message =
                copy(
                        source,
                        source.eventId(),
                        source.aggregateId(),
                        source.aggregateType(),
                        source.eventType(),
                        source.eventVersion(),
                        source.occurredAt(),
                        source.producer(),
                        source.correlationId(),
                        UUID.randomUUID(),
                        source.payload());

        assertValidation(
                () -> validator.validateSucceeded(bookingId.toString(), message),
                BookingErrorCode.EVENT_CAUSATION_ID_INVALID);
    }

    @Test
    void malformedPartitionKeyShouldBeRejected() {

        OutboxEventMessage message =
                validMessage(
                        BookingEventContract.PAYMENT_SUCCEEDED,
                        BookingEventContract.PAYMENT_SUCCEEDED_VERSION);

        assertValidation(
                () -> validator.validateSucceeded("not-a-uuid", message),
                BookingErrorCode.EVENT_PARTITION_KEY_INVALID);
    }

    @Test
    void nonUuidV7PartitionKeyShouldBeRejected() {

        OutboxEventMessage message =
                validMessage(
                        BookingEventContract.PAYMENT_SUCCEEDED,
                        BookingEventContract.PAYMENT_SUCCEEDED_VERSION);

        assertValidation(
                () -> validator.validateSucceeded(UUID.randomUUID().toString(), message),
                BookingErrorCode.EVENT_PARTITION_KEY_INVALID);
    }

    @Test
    void missingPayloadShouldBeRejected() {

        UUID bookingId = UuidGenerator.next();

        OutboxEventMessage source =
                validMessage(
                        BookingEventContract.PAYMENT_SUCCEEDED,
                        BookingEventContract.PAYMENT_SUCCEEDED_VERSION);

        OutboxEventMessage message =
                copy(
                        source,
                        source.eventId(),
                        source.aggregateId(),
                        source.aggregateType(),
                        source.eventType(),
                        source.eventVersion(),
                        source.occurredAt(),
                        source.producer(),
                        source.correlationId(),
                        source.causationId(),
                        null);

        assertValidation(
                () -> validator.validateSucceeded(bookingId.toString(), message),
                BookingErrorCode.EVENT_PAYLOAD_INVALID);
    }

    @Test
    void nonObjectPayloadShouldBeRejected() {

        UUID bookingId = UuidGenerator.next();

        OutboxEventMessage source =
                validMessage(
                        BookingEventContract.PAYMENT_SUCCEEDED,
                        BookingEventContract.PAYMENT_SUCCEEDED_VERSION);

        OutboxEventMessage message =
                copy(
                        source,
                        source.eventId(),
                        source.aggregateId(),
                        source.aggregateType(),
                        source.eventType(),
                        source.eventVersion(),
                        source.occurredAt(),
                        source.producer(),
                        source.correlationId(),
                        source.causationId(),
                        JsonNodeFactory.instance.textNode("invalid"));

        assertValidation(
                () -> validator.validateSucceeded(bookingId.toString(), message),
                BookingErrorCode.EVENT_PAYLOAD_INVALID);
    }

    private static OutboxEventMessage validMessage(String eventType, String eventVersion) {

        UUID paymentId = UuidGenerator.next();
        UUID bookingId = UuidGenerator.next();

        ObjectNode payload = JsonNodeFactory.instance.objectNode();

        payload.put("paymentId", paymentId.toString());
        payload.put("bookingId", bookingId.toString());

        return new OutboxEventMessage(
                UuidGenerator.next(),
                paymentId,
                BookingEventContract.PAYMENT_AGGREGATE_TYPE,
                eventType,
                eventVersion,
                OCCURRED_AT,
                BookingEventContract.PAYMENT_PRODUCER,
                UuidGenerator.next(),
                UuidGenerator.next(),
                payload);
    }

    private static OutboxEventMessage withBookingId(OutboxEventMessage source, UUID bookingId) {

        ObjectNode payload = (ObjectNode) source.payload().deepCopy();

        payload.put("bookingId", bookingId.toString());

        return copy(
                source,
                source.eventId(),
                source.aggregateId(),
                source.aggregateType(),
                source.eventType(),
                source.eventVersion(),
                source.occurredAt(),
                source.producer(),
                source.correlationId(),
                source.causationId(),
                payload);
    }

    private static OutboxEventMessage copy(
            OutboxEventMessage source,
            UUID eventId,
            UUID aggregateId,
            String aggregateType,
            String eventType,
            String eventVersion,
            OffsetDateTime occurredAt,
            String producer,
            UUID correlationId,
            UUID causationId,
            JsonNode payload) {

        return new OutboxEventMessage(
                eventId,
                aggregateId,
                aggregateType,
                eventType,
                eventVersion,
                occurredAt,
                producer,
                correlationId,
                causationId,
                payload);
    }

    private static void assertValidation(
            ThrowingCallable callable, BookingErrorCode expectedErrorCode) {

        assertThatThrownBy(callable)
                .isInstanceOf(ValidationException.class)
                .satisfies(
                        throwable ->
                                assertThat(((ValidationException) throwable).getErrorCode())
                                        .isEqualTo(expectedErrorCode));
    }
}
