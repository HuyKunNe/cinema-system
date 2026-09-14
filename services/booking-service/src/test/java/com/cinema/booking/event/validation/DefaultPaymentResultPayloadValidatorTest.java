package com.cinema.booking.event.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cinema.booking.event.BookingEventContract;
import com.cinema.booking.event.payload.PaymentFailedPayload;
import com.cinema.booking.event.payload.PaymentSucceededPayload;
import com.cinema.booking.exception.BookingErrorCode;
import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

class DefaultPaymentResultPayloadValidatorTest {

    private static final UUID PAYMENT_ID = UuidGenerator.next();

    private static final UUID BOOKING_ID = UuidGenerator.next();

    private static final OffsetDateTime OCCURRED_AT = OffsetDateTime.parse("2026-09-14T08:00:00Z");

    private final DefaultPaymentResultPayloadValidator validator =
            new DefaultPaymentResultPayloadValidator();

    @Test
    void canonicalPaymentSucceededPayloadShouldPass() {

        assertThatCode(
                        () ->
                                validator.validateSucceeded(
                                        BOOKING_ID.toString(),
                                        succeededMessage(),
                                        succeededPayload()))
                .doesNotThrowAnyException();
    }

    @Test
    void succeededPaymentIdMustMatchAggregateId() {

        PaymentSucceededPayload payload =
                new PaymentSucceededPayload(
                        UuidGenerator.next(),
                        BOOKING_ID,
                        new BigDecimal("180000.00"),
                        "VND",
                        "MOMO",
                        "momo-reference-001",
                        OCCURRED_AT);

        assertInvalidPayload(
                () ->
                        validator.validateSucceeded(
                                BOOKING_ID.toString(), succeededMessage(), payload));
    }

    @Test
    void succeededBookingIdMustMatchPartitionKey() {

        assertValidation(
                () ->
                        validator.validateSucceeded(
                                UuidGenerator.next().toString(),
                                succeededMessage(),
                                succeededPayload()),
                BookingErrorCode.EVENT_PARTITION_KEY_INVALID);
    }

    @Test
    void succeededPaymentIdMustBeUuidV7() {

        PaymentSucceededPayload payload =
                new PaymentSucceededPayload(
                        UUID.randomUUID(),
                        BOOKING_ID,
                        new BigDecimal("180000.00"),
                        "VND",
                        "MOMO",
                        "momo-reference-001",
                        OCCURRED_AT);

        assertInvalidPayload(
                () ->
                        validator.validateSucceeded(
                                BOOKING_ID.toString(), succeededMessage(), payload));
    }

    @Test
    void succeededBookingIdMustBeUuidV7() {

        UUID nonUuidV7BookingId = UUID.randomUUID();

        PaymentSucceededPayload payload =
                new PaymentSucceededPayload(
                        PAYMENT_ID,
                        nonUuidV7BookingId,
                        new BigDecimal("180000.00"),
                        "VND",
                        "MOMO",
                        "momo-reference-001",
                        OCCURRED_AT);

        assertInvalidPayload(
                () ->
                        validator.validateSucceeded(
                                nonUuidV7BookingId.toString(), succeededMessage(), payload));
    }

    @Test
    void missingSucceededAmountShouldBeRejected() {

        PaymentSucceededPayload payload =
                new PaymentSucceededPayload(
                        PAYMENT_ID,
                        BOOKING_ID,
                        null,
                        "VND",
                        "MOMO",
                        "momo-reference-001",
                        OCCURRED_AT);

        assertValidation(
                () ->
                        validator.validateSucceeded(
                                BOOKING_ID.toString(), succeededMessage(), payload),
                BookingErrorCode.TOTAL_AMOUNT_REQUIRED);
    }

    @Test
    void negativeSucceededAmountShouldBeRejected() {

        PaymentSucceededPayload payload =
                new PaymentSucceededPayload(
                        PAYMENT_ID,
                        BOOKING_ID,
                        new BigDecimal("-1.00"),
                        "VND",
                        "MOMO",
                        "momo-reference-001",
                        OCCURRED_AT);

        assertValidation(
                () ->
                        validator.validateSucceeded(
                                BOOKING_ID.toString(), succeededMessage(), payload),
                BookingErrorCode.INVALID_TOTAL_AMOUNT);
    }

    @Test
    void excessiveSucceededAmountScaleShouldBeRejected() {

        PaymentSucceededPayload payload =
                new PaymentSucceededPayload(
                        PAYMENT_ID,
                        BOOKING_ID,
                        new BigDecimal("180000.001"),
                        "VND",
                        "MOMO",
                        "momo-reference-001",
                        OCCURRED_AT);

        assertValidation(
                () ->
                        validator.validateSucceeded(
                                BOOKING_ID.toString(), succeededMessage(), payload),
                BookingErrorCode.INVALID_TOTAL_AMOUNT);
    }

    @Test
    void excessiveSucceededAmountPrecisionShouldBeRejected() {

        PaymentSucceededPayload payload =
                new PaymentSucceededPayload(
                        PAYMENT_ID,
                        BOOKING_ID,
                        new BigDecimal("123456789012345678.00"),
                        "VND",
                        "MOMO",
                        "momo-reference-001",
                        OCCURRED_AT);

        assertValidation(
                () ->
                        validator.validateSucceeded(
                                BOOKING_ID.toString(), succeededMessage(), payload),
                BookingErrorCode.INVALID_TOTAL_AMOUNT);
    }

    @Test
    void missingSucceededCurrencyShouldBeRejected() {

        PaymentSucceededPayload payload =
                new PaymentSucceededPayload(
                        PAYMENT_ID,
                        BOOKING_ID,
                        new BigDecimal("180000.00"),
                        null,
                        "MOMO",
                        "momo-reference-001",
                        OCCURRED_AT);

        assertValidation(
                () ->
                        validator.validateSucceeded(
                                BOOKING_ID.toString(), succeededMessage(), payload),
                BookingErrorCode.CURRENCY_REQUIRED);
    }

    @Test
    void nonCanonicalSucceededCurrencyShouldBeRejected() {

        PaymentSucceededPayload payload =
                new PaymentSucceededPayload(
                        PAYMENT_ID,
                        BOOKING_ID,
                        new BigDecimal("180000.00"),
                        "vnd",
                        "MOMO",
                        "momo-reference-001",
                        OCCURRED_AT);

        assertValidation(
                () ->
                        validator.validateSucceeded(
                                BOOKING_ID.toString(), succeededMessage(), payload),
                BookingErrorCode.INVALID_CURRENCY);
    }

    @Test
    void missingSucceededProviderShouldBeRejected() {

        PaymentSucceededPayload payload =
                new PaymentSucceededPayload(
                        PAYMENT_ID,
                        BOOKING_ID,
                        new BigDecimal("180000.00"),
                        "VND",
                        null,
                        "momo-reference-001",
                        OCCURRED_AT);

        assertInvalidPayload(
                () ->
                        validator.validateSucceeded(
                                BOOKING_ID.toString(), succeededMessage(), payload));
    }

    @Test
    void nonCanonicalSucceededProviderShouldBeRejected() {

        PaymentSucceededPayload payload =
                new PaymentSucceededPayload(
                        PAYMENT_ID,
                        BOOKING_ID,
                        new BigDecimal("180000.00"),
                        "VND",
                        "momo",
                        "momo-reference-001",
                        OCCURRED_AT);

        assertInvalidPayload(
                () ->
                        validator.validateSucceeded(
                                BOOKING_ID.toString(), succeededMessage(), payload));
    }

    @Test
    void missingProviderReferenceShouldBeRejected() {

        PaymentSucceededPayload payload =
                new PaymentSucceededPayload(
                        PAYMENT_ID,
                        BOOKING_ID,
                        new BigDecimal("180000.00"),
                        "VND",
                        "MOMO",
                        null,
                        OCCURRED_AT);

        assertInvalidPayload(
                () ->
                        validator.validateSucceeded(
                                BOOKING_ID.toString(), succeededMessage(), payload));
    }

    @Test
    void succeededTimeMustMatchEnvelopeOccurredAt() {

        PaymentSucceededPayload payload =
                new PaymentSucceededPayload(
                        PAYMENT_ID,
                        BOOKING_ID,
                        new BigDecimal("180000.00"),
                        "VND",
                        "MOMO",
                        "momo-reference-001",
                        OCCURRED_AT.plusSeconds(1));

        assertInvalidPayload(
                () ->
                        validator.validateSucceeded(
                                BOOKING_ID.toString(), succeededMessage(), payload));
    }

    @Test
    void canonicalPaymentFailedPayloadShouldPass() {

        assertThatCode(
                        () ->
                                validator.validateFailed(
                                        BOOKING_ID.toString(),
                                        failedMessage(),
                                        failedPayload("PAYMENT_DECLINED", "Payment was declined")))
                .doesNotThrowAnyException();
    }

    @Test
    void reservationExpiredMayHaveNullMessage() {

        assertThatCode(
                        () ->
                                validator.validateFailed(
                                        BOOKING_ID.toString(),
                                        failedMessage(),
                                        failedPayload("RESERVATION_EXPIRED", null)))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "PAYMENT_DECLINED",
                "PAYMENT_TIMEOUT",
                "PROVIDER_UNAVAILABLE",
                "INVALID_PAYMENT_REQUEST",
                "RESERVATION_EXPIRED",
                "DUPLICATE_PAYMENT"
            })
    void approvedFailureCodeShouldPass(String failureCode) {

        assertThatCode(
                        () ->
                                validator.validateFailed(
                                        BOOKING_ID.toString(),
                                        failedMessage(),
                                        failedPayload(failureCode, null)))
                .doesNotThrowAnyException();
    }

    @Test
    void unsupportedFailureCodeShouldBeRejected() {

        assertInvalidPayload(
                () ->
                        validator.validateFailed(
                                BOOKING_ID.toString(),
                                failedMessage(),
                                failedPayload("UNKNOWN_PROVIDER_ERROR", "Unknown error")));
    }

    @Test
    void retryableFailureShouldBeRejected() {

        PaymentFailedPayload payload =
                new PaymentFailedPayload(
                        PAYMENT_ID,
                        BOOKING_ID,
                        "PAYMENT_TIMEOUT",
                        "Payment timed out",
                        OCCURRED_AT,
                        true);

        assertInvalidPayload(
                () -> validator.validateFailed(BOOKING_ID.toString(), failedMessage(), payload));
    }

    @Test
    void failedPaymentIdMustMatchAggregateId() {

        PaymentFailedPayload payload =
                new PaymentFailedPayload(
                        UuidGenerator.next(),
                        BOOKING_ID,
                        "PAYMENT_DECLINED",
                        "Payment was declined",
                        OCCURRED_AT,
                        false);

        assertInvalidPayload(
                () -> validator.validateFailed(BOOKING_ID.toString(), failedMessage(), payload));
    }

    @Test
    void failedBookingIdMustMatchPartitionKey() {

        assertValidation(
                () ->
                        validator.validateFailed(
                                UuidGenerator.next().toString(),
                                failedMessage(),
                                failedPayload("PAYMENT_DECLINED", "Payment was declined")),
                BookingErrorCode.EVENT_PARTITION_KEY_INVALID);
    }

    @Test
    void blankFailureMessageShouldBeRejected() {

        assertInvalidPayload(
                () ->
                        validator.validateFailed(
                                BOOKING_ID.toString(),
                                failedMessage(),
                                failedPayload("PAYMENT_DECLINED", "   ")));
    }

    @Test
    void failedTimeMustMatchEnvelopeOccurredAt() {

        PaymentFailedPayload payload =
                new PaymentFailedPayload(
                        PAYMENT_ID,
                        BOOKING_ID,
                        "PAYMENT_DECLINED",
                        "Payment was declined",
                        OCCURRED_AT.plusSeconds(1),
                        false);

        assertInvalidPayload(
                () -> validator.validateFailed(BOOKING_ID.toString(), failedMessage(), payload));
    }

    @Test
    void nullMessageOrPayloadShouldBeRejected() {

        assertInvalidPayload(
                () -> validator.validateSucceeded(BOOKING_ID.toString(), null, succeededPayload()));

        assertInvalidPayload(
                () -> validator.validateSucceeded(BOOKING_ID.toString(), succeededMessage(), null));

        assertInvalidPayload(
                () ->
                        validator.validateFailed(
                                BOOKING_ID.toString(),
                                null,
                                failedPayload("PAYMENT_DECLINED", null)));

        assertInvalidPayload(
                () -> validator.validateFailed(BOOKING_ID.toString(), failedMessage(), null));
    }

    private static PaymentSucceededPayload succeededPayload() {

        return new PaymentSucceededPayload(
                PAYMENT_ID,
                BOOKING_ID,
                new BigDecimal("180000.00"),
                "VND",
                "MOMO",
                "momo-reference-001",
                OCCURRED_AT);
    }

    private static PaymentFailedPayload failedPayload(String failureCode, String failureMessage) {

        return new PaymentFailedPayload(
                PAYMENT_ID, BOOKING_ID, failureCode, failureMessage, OCCURRED_AT, false);
    }

    private static OutboxEventMessage succeededMessage() {

        return message(
                BookingEventContract.PAYMENT_SUCCEEDED,
                BookingEventContract.PAYMENT_SUCCEEDED_VERSION);
    }

    private static OutboxEventMessage failedMessage() {

        return message(
                BookingEventContract.PAYMENT_FAILED, BookingEventContract.PAYMENT_FAILED_VERSION);
    }

    private static OutboxEventMessage message(String eventType, String eventVersion) {

        return new OutboxEventMessage(
                UuidGenerator.next(),
                PAYMENT_ID,
                BookingEventContract.PAYMENT_AGGREGATE_TYPE,
                eventType,
                eventVersion,
                OCCURRED_AT,
                BookingEventContract.PAYMENT_PRODUCER,
                UuidGenerator.next(),
                UuidGenerator.next(),
                JsonNodeFactory.instance.objectNode());
    }

    private static void assertInvalidPayload(ThrowingCallable callable) {

        assertValidation(callable, BookingErrorCode.EVENT_PAYLOAD_INVALID);
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
