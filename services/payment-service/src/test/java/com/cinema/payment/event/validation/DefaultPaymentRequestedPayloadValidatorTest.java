package com.cinema.payment.event.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.cinema.payment.event.payload.PaymentRequestedPayload;
import com.cinema.payment.exception.PaymentErrorCode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

class DefaultPaymentRequestedPayloadValidatorTest {

    private static final UUID BOOKING_ID = UuidGenerator.next();

    private static final UUID USER_ID = UuidGenerator.next();

    private static final OffsetDateTime REQUESTED_AT = OffsetDateTime.parse("2026-08-26T10:00:00Z");

    private static final OffsetDateTime HOLD_EXPIRES_AT = REQUESTED_AT.plusMinutes(10);

    private final DefaultPaymentRequestedPayloadValidator validator =
            new DefaultPaymentRequestedPayloadValidator();

    @Test
    void canonicalPayloadShouldBeAccepted() {

        assertThatCode(
                        () ->
                                validator.validate(
                                        message(BOOKING_ID),
                                        payload(
                                                BOOKING_ID,
                                                USER_ID,
                                                new BigDecimal("180000.00"),
                                                "VND",
                                                1,
                                                HOLD_EXPIRES_AT,
                                                REQUESTED_AT)))
                .doesNotThrowAnyException();
    }

    @Test
    void nullPayloadShouldBeRejected() {

        assertValidation(
                () -> validator.validate(message(BOOKING_ID), null),
                PaymentErrorCode.EVENT_PAYLOAD_INVALID);
    }

    @Test
    void bookingIdMismatchShouldBeRejected() {

        assertValidation(
                () -> validator.validate(message(BOOKING_ID), validPayload(UuidGenerator.next())),
                PaymentErrorCode.EVENT_PAYLOAD_AGGREGATE_MISMATCH);
    }

    @Test
    void nonUuidV7BookingIdShouldBeRejected() {

        UUID bookingId = UUID.randomUUID();

        assertValidation(
                () -> validator.validate(message(bookingId), validPayload(bookingId)),
                PaymentErrorCode.EVENT_PAYLOAD_INVALID);
    }

    @Test
    void nonUuidV7UserIdShouldBeRejected() {

        assertValidation(
                () ->
                        validator.validate(
                                message(BOOKING_ID),
                                payload(
                                        BOOKING_ID,
                                        UUID.randomUUID(),
                                        new BigDecimal("180000.00"),
                                        "VND",
                                        1,
                                        HOLD_EXPIRES_AT,
                                        REQUESTED_AT)),
                PaymentErrorCode.EVENT_PAYLOAD_INVALID);
    }

    @Test
    void missingAmountShouldBeRejected() {

        assertValidation(
                () ->
                        validator.validate(
                                message(BOOKING_ID),
                                payload(
                                        BOOKING_ID,
                                        USER_ID,
                                        null,
                                        "VND",
                                        1,
                                        HOLD_EXPIRES_AT,
                                        REQUESTED_AT)),
                PaymentErrorCode.AMOUNT_REQUIRED);
    }

    @Test
    void negativeAmountShouldBeRejected() {

        assertValidation(
                () ->
                        validator.validate(
                                message(BOOKING_ID),
                                payload(
                                        BOOKING_ID,
                                        USER_ID,
                                        new BigDecimal("-0.01"),
                                        "VND",
                                        1,
                                        HOLD_EXPIRES_AT,
                                        REQUESTED_AT)),
                PaymentErrorCode.AMOUNT_INVALID);
    }

    @Test
    void amountScaleGreaterThanTwoShouldBeRejected() {

        assertValidation(
                () ->
                        validator.validate(
                                message(BOOKING_ID),
                                payload(
                                        BOOKING_ID,
                                        USER_ID,
                                        new BigDecimal("1.001"),
                                        "VND",
                                        1,
                                        HOLD_EXPIRES_AT,
                                        REQUESTED_AT)),
                PaymentErrorCode.AMOUNT_PRECISION_INVALID);
    }

    @Test
    void amountWithMoreThanSeventeenIntegerDigitsShouldBeRejected() {

        assertValidation(
                () ->
                        validator.validate(
                                message(BOOKING_ID),
                                payload(
                                        BOOKING_ID,
                                        USER_ID,
                                        new BigDecimal("100000000000000000.00"),
                                        "VND",
                                        1,
                                        HOLD_EXPIRES_AT,
                                        REQUESTED_AT)),
                PaymentErrorCode.AMOUNT_PRECISION_INVALID);
    }

    @Test
    void zeroAmountShouldBeAccepted() {

        assertThatCode(
                        () ->
                                validator.validate(
                                        message(BOOKING_ID),
                                        payload(
                                                BOOKING_ID,
                                                USER_ID,
                                                BigDecimal.ZERO,
                                                "VND",
                                                1,
                                                HOLD_EXPIRES_AT,
                                                REQUESTED_AT)))
                .doesNotThrowAnyException();
    }

    @Test
    void lowercaseCurrencyShouldBeRejected() {

        assertValidation(
                () ->
                        validator.validate(
                                message(BOOKING_ID),
                                payload(
                                        BOOKING_ID,
                                        USER_ID,
                                        new BigDecimal("180000.00"),
                                        "vnd",
                                        1,
                                        HOLD_EXPIRES_AT,
                                        REQUESTED_AT)),
                PaymentErrorCode.CURRENCY_INVALID);
    }

    @Test
    void nonLetterCurrencyShouldBeRejected() {

        assertValidation(
                () ->
                        validator.validate(
                                message(BOOKING_ID),
                                payload(
                                        BOOKING_ID,
                                        USER_ID,
                                        new BigDecimal("180000.00"),
                                        "VN1",
                                        1,
                                        HOLD_EXPIRES_AT,
                                        REQUESTED_AT)),
                PaymentErrorCode.CURRENCY_INVALID);
    }

    @Test
    void nonPositivePaymentAttemptShouldBeRejected() {

        assertValidation(
                () ->
                        validator.validate(
                                message(BOOKING_ID),
                                payload(
                                        BOOKING_ID,
                                        USER_ID,
                                        new BigDecimal("180000.00"),
                                        "VND",
                                        0,
                                        HOLD_EXPIRES_AT,
                                        REQUESTED_AT)),
                PaymentErrorCode.PAYMENT_ATTEMPT_INVALID);
    }

    @Test
    void missingRequestedAtShouldBeRejected() {

        assertValidation(
                () ->
                        validator.validate(
                                message(BOOKING_ID),
                                payload(
                                        BOOKING_ID,
                                        USER_ID,
                                        new BigDecimal("180000.00"),
                                        "VND",
                                        1,
                                        HOLD_EXPIRES_AT,
                                        null)),
                PaymentErrorCode.REQUESTED_AT_REQUIRED);
    }

    @Test
    void missingHoldExpirationShouldBeRejected() {

        assertValidation(
                () ->
                        validator.validate(
                                message(BOOKING_ID),
                                payload(
                                        BOOKING_ID,
                                        USER_ID,
                                        new BigDecimal("180000.00"),
                                        "VND",
                                        1,
                                        null,
                                        REQUESTED_AT)),
                PaymentErrorCode.HOLD_EXPIRATION_REQUIRED);
    }

    @Test
    void holdExpirationAtRequestedTimeShouldBeRejected() {

        assertValidation(
                () ->
                        validator.validate(
                                message(BOOKING_ID),
                                payload(
                                        BOOKING_ID,
                                        USER_ID,
                                        new BigDecimal("180000.00"),
                                        "VND",
                                        1,
                                        REQUESTED_AT,
                                        REQUESTED_AT)),
                PaymentErrorCode.HOLD_EXPIRATION_INVALID);
    }

    @Test
    void holdExpirationBeforeRequestedTimeShouldBeRejected() {

        assertValidation(
                () ->
                        validator.validate(
                                message(BOOKING_ID),
                                payload(
                                        BOOKING_ID,
                                        USER_ID,
                                        new BigDecimal("180000.00"),
                                        "VND",
                                        1,
                                        REQUESTED_AT.minusSeconds(1),
                                        REQUESTED_AT)),
                PaymentErrorCode.HOLD_EXPIRATION_INVALID);
    }

    private static PaymentRequestedPayload validPayload(UUID bookingId) {

        return payload(
                bookingId,
                USER_ID,
                new BigDecimal("180000.00"),
                "VND",
                1,
                HOLD_EXPIRES_AT,
                REQUESTED_AT);
    }

    private static PaymentRequestedPayload payload(
            UUID bookingId,
            UUID userId,
            BigDecimal amount,
            String currency,
            int paymentAttempt,
            OffsetDateTime holdExpiresAt,
            OffsetDateTime requestedAt) {

        return new PaymentRequestedPayload(
                bookingId, userId, amount, currency, paymentAttempt, holdExpiresAt, requestedAt);
    }

    private static OutboxEventMessage message(UUID aggregateId) {

        return new OutboxEventMessage(
                UuidGenerator.next(),
                aggregateId,
                "BOOKING",
                "payment-requested",
                "1",
                REQUESTED_AT,
                "booking-service",
                UuidGenerator.next(),
                UuidGenerator.next(),
                JsonNodeFactory.instance.objectNode());
    }

    private static void assertValidation(
            ThrowingCallable callable, PaymentErrorCode expectedErrorCode) {

        assertThatThrownBy(callable)
                .isInstanceOf(ValidationException.class)
                .satisfies(
                        throwable ->
                                assertThat(((ValidationException) throwable).getErrorCode())
                                        .isEqualTo(expectedErrorCode));
    }
}
