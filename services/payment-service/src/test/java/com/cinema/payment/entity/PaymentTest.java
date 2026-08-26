package com.cinema.payment.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.payment.enums.PaymentStatus;
import com.cinema.payment.enums.RefundStatus;
import com.cinema.payment.exception.PaymentErrorCode;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

class PaymentTest {

    private static final UUID BOOKING_ID = UuidGenerator.next();

    private static final UUID USER_ID = UuidGenerator.next();

    private static final UUID SOURCE_EVENT_ID = UuidGenerator.next();

    private static final UUID CORRELATION_ID = UuidGenerator.next();

    private static final OffsetDateTime REQUESTED_AT = OffsetDateTime.parse("2026-08-26T10:00:00Z");

    @Test
    void shouldCreateReceivedPaymentWithNormalizedValues() {

        Payment payment =
                payment(
                        BOOKING_ID,
                        USER_ID,
                        1,
                        new BigDecimal("250000.00"),
                        "vnd",
                        "mock",
                        REQUESTED_AT.plusMinutes(10),
                        REQUESTED_AT,
                        SOURCE_EVENT_ID,
                        CORRELATION_ID);

        assertThat(payment.getId()).isNotNull();
        assertThat(payment.getVersion()).isZero();
        assertThat(payment.getBookingId()).isEqualTo(BOOKING_ID);
        assertThat(payment.getUserId()).isEqualTo(USER_ID);
        assertThat(payment.getPaymentAttempt()).isEqualTo(1);
        assertThat(payment.getAmount()).isEqualByComparingTo("250000.00");
        assertThat(payment.getCurrency()).isEqualTo("VND");
        assertThat(payment.getProvider()).isEqualTo("MOCK");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.RECEIVED);
        assertThat(payment.getRefundStatus()).isEqualTo(RefundStatus.NOT_REQUESTED);
        assertThat(payment.getHoldExpiresAt()).isEqualTo(REQUESTED_AT.plusMinutes(10));
        assertThat(payment.getRequestedAt()).isEqualTo(REQUESTED_AT);
        assertThat(payment.getSourceEventId()).isEqualTo(SOURCE_EVENT_ID);
        assertThat(payment.getCorrelationId()).isEqualTo(CORRELATION_ID);
        assertThat(payment.getProviderReference()).isNull();
        assertThat(payment.getFailureCode()).isNull();
        assertThat(payment.getFailureMessage()).isNull();
        assertThat(payment.getCompletedAt()).isNull();
        assertThat(payment.isReceived()).isTrue();
        assertThat(payment.isTerminal()).isFalse();
    }

    @Test
    void shouldAllowZeroAmountAccordingToCurrentContract() {

        Payment payment =
                payment(
                        BOOKING_ID,
                        USER_ID,
                        1,
                        BigDecimal.ZERO,
                        "VND",
                        "MOCK",
                        REQUESTED_AT.plusMinutes(10),
                        REQUESTED_AT,
                        SOURCE_EVENT_ID,
                        CORRELATION_ID);

        assertThat(payment.getAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void shouldRejectMissingBookingId() {

        assertValidation(
                () ->
                        payment(
                                null,
                                USER_ID,
                                1,
                                new BigDecimal("250000.00"),
                                "VND",
                                "MOCK",
                                REQUESTED_AT.plusMinutes(10),
                                REQUESTED_AT,
                                SOURCE_EVENT_ID,
                                CORRELATION_ID),
                PaymentErrorCode.BOOKING_ID_REQUIRED);
    }

    @Test
    void shouldRejectMissingUserId() {

        assertValidation(
                () ->
                        payment(
                                BOOKING_ID,
                                null,
                                1,
                                new BigDecimal("250000.00"),
                                "VND",
                                "MOCK",
                                REQUESTED_AT.plusMinutes(10),
                                REQUESTED_AT,
                                SOURCE_EVENT_ID,
                                CORRELATION_ID),
                PaymentErrorCode.USER_ID_REQUIRED);
    }

    @Test
    void shouldRejectNonPositivePaymentAttempt() {

        assertValidation(
                () ->
                        payment(
                                BOOKING_ID,
                                USER_ID,
                                0,
                                new BigDecimal("250000.00"),
                                "VND",
                                "MOCK",
                                REQUESTED_AT.plusMinutes(10),
                                REQUESTED_AT,
                                SOURCE_EVENT_ID,
                                CORRELATION_ID),
                PaymentErrorCode.PAYMENT_ATTEMPT_INVALID);
    }

    @Test
    void shouldRejectMissingAmount() {

        assertValidation(
                () ->
                        payment(
                                BOOKING_ID,
                                USER_ID,
                                1,
                                null,
                                "VND",
                                "MOCK",
                                REQUESTED_AT.plusMinutes(10),
                                REQUESTED_AT,
                                SOURCE_EVENT_ID,
                                CORRELATION_ID),
                PaymentErrorCode.AMOUNT_REQUIRED);
    }

    @Test
    void shouldRejectNegativeAmount() {

        assertValidation(
                () ->
                        payment(
                                BOOKING_ID,
                                USER_ID,
                                1,
                                new BigDecimal("-0.01"),
                                "VND",
                                "MOCK",
                                REQUESTED_AT.plusMinutes(10),
                                REQUESTED_AT,
                                SOURCE_EVENT_ID,
                                CORRELATION_ID),
                PaymentErrorCode.AMOUNT_INVALID);
    }

    @Test
    void shouldRejectInvalidCurrency() {

        assertValidation(
                () ->
                        payment(
                                BOOKING_ID,
                                USER_ID,
                                1,
                                new BigDecimal("250000.00"),
                                "VN1",
                                "MOCK",
                                REQUESTED_AT.plusMinutes(10),
                                REQUESTED_AT,
                                SOURCE_EVENT_ID,
                                CORRELATION_ID),
                PaymentErrorCode.CURRENCY_INVALID);
    }

    @Test
    void shouldRejectMissingProvider() {

        assertValidation(
                () ->
                        payment(
                                BOOKING_ID,
                                USER_ID,
                                1,
                                new BigDecimal("250000.00"),
                                "VND",
                                " ",
                                REQUESTED_AT.plusMinutes(10),
                                REQUESTED_AT,
                                SOURCE_EVENT_ID,
                                CORRELATION_ID),
                PaymentErrorCode.PROVIDER_REQUIRED);
    }

    @Test
    void shouldRejectHoldExpirationAtRequestedTime() {

        assertValidation(
                () ->
                        payment(
                                BOOKING_ID,
                                USER_ID,
                                1,
                                new BigDecimal("250000.00"),
                                "VND",
                                "MOCK",
                                REQUESTED_AT,
                                REQUESTED_AT,
                                SOURCE_EVENT_ID,
                                CORRELATION_ID),
                PaymentErrorCode.HOLD_EXPIRATION_INVALID);
    }

    @Test
    void shouldRejectHoldExpirationBeforeRequestedTime() {

        assertValidation(
                () ->
                        payment(
                                BOOKING_ID,
                                USER_ID,
                                1,
                                new BigDecimal("250000.00"),
                                "VND",
                                "MOCK",
                                REQUESTED_AT.minusSeconds(1),
                                REQUESTED_AT,
                                SOURCE_EVENT_ID,
                                CORRELATION_ID),
                PaymentErrorCode.HOLD_EXPIRATION_INVALID);
    }

    @Test
    void shouldRejectMissingSourceEventId() {

        assertValidation(
                () ->
                        payment(
                                BOOKING_ID,
                                USER_ID,
                                1,
                                new BigDecimal("250000.00"),
                                "VND",
                                "MOCK",
                                REQUESTED_AT.plusMinutes(10),
                                REQUESTED_AT,
                                null,
                                CORRELATION_ID),
                PaymentErrorCode.SOURCE_EVENT_ID_REQUIRED);
    }

    @Test
    void shouldRejectMissingCorrelationId() {

        assertValidation(
                () ->
                        payment(
                                BOOKING_ID,
                                USER_ID,
                                1,
                                new BigDecimal("250000.00"),
                                "VND",
                                "MOCK",
                                REQUESTED_AT.plusMinutes(10),
                                REQUESTED_AT,
                                SOURCE_EVENT_ID,
                                null),
                PaymentErrorCode.CORRELATION_ID_REQUIRED);
    }

    private static Payment payment(
            UUID bookingId,
            UUID userId,
            int paymentAttempt,
            BigDecimal amount,
            String currency,
            String provider,
            OffsetDateTime holdExpiresAt,
            OffsetDateTime requestedAt,
            UUID sourceEventId,
            UUID correlationId) {

        return new Payment(
                bookingId,
                userId,
                paymentAttempt,
                amount,
                currency,
                provider,
                holdExpiresAt,
                requestedAt,
                sourceEventId,
                correlationId);
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
