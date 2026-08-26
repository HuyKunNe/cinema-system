package com.cinema.payment.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.payment.enums.PaymentTransactionStatus;
import com.cinema.payment.enums.PaymentTransactionType;
import com.cinema.payment.exception.PaymentErrorCode;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

class PaymentTransactionTest {

    private static final UUID PAYMENT_ID = UuidGenerator.next();

    private static final OffsetDateTime REQUESTED_AT = OffsetDateTime.parse("2026-08-26T10:00:00Z");

    private static final String IDEMPOTENCY_KEY = "charge:booking-1:attempt-1";

    @Test
    void shouldCreateReadyChargeWithStableIdempotencyKey() {

        PaymentTransaction transaction =
                transaction(
                        PAYMENT_ID,
                        "mock",
                        PaymentTransactionType.CHARGE,
                        1,
                        new BigDecimal("250000.00"),
                        "vnd",
                        IDEMPOTENCY_KEY,
                        REQUESTED_AT);

        assertThat(transaction.getId()).isNotNull();
        assertThat(transaction.getPaymentId()).isEqualTo(PAYMENT_ID);
        assertThat(transaction.getProvider()).isEqualTo("MOCK");
        assertThat(transaction.getTransactionType()).isEqualTo(PaymentTransactionType.CHARGE);
        assertThat(transaction.getAttemptNumber()).isEqualTo(1);
        assertThat(transaction.getStatus()).isEqualTo(PaymentTransactionStatus.READY);
        assertThat(transaction.getAmount()).isEqualByComparingTo("250000.00");
        assertThat(transaction.getCurrency()).isEqualTo("VND");
        assertThat(transaction.getIdempotencyKey()).isEqualTo(IDEMPOTENCY_KEY);
        assertThat(transaction.getRequestedAt()).isEqualTo(REQUESTED_AT);
        assertThat(transaction.getProviderReference()).isNull();
        assertThat(transaction.getProviderEventId()).isNull();
        assertThat(transaction.getFailureCode()).isNull();
        assertThat(transaction.getFailureMessage()).isNull();
        assertThat(transaction.getCompletedAt()).isNull();
        assertThat(transaction.getProcessingOwner()).isNull();
        assertThat(transaction.getProcessingExpiresAt()).isNull();
        assertThat(transaction.isReady()).isTrue();
    }

    @Test
    void shouldAllowZeroAmountAccordingToCurrentContract() {

        PaymentTransaction transaction =
                transaction(
                        PAYMENT_ID,
                        "MOCK",
                        PaymentTransactionType.CHARGE,
                        1,
                        BigDecimal.ZERO,
                        "VND",
                        IDEMPOTENCY_KEY,
                        REQUESTED_AT);

        assertThat(transaction.getAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void shouldTrimIdempotencyKeyWithoutChangingItsValue() {

        PaymentTransaction transaction =
                transaction(
                        PAYMENT_ID,
                        "MOCK",
                        PaymentTransactionType.CHARGE,
                        1,
                        new BigDecimal("250000.00"),
                        "VND",
                        "  " + IDEMPOTENCY_KEY + "  ",
                        REQUESTED_AT);

        assertThat(transaction.getIdempotencyKey()).isEqualTo(IDEMPOTENCY_KEY);
    }

    @Test
    void shouldRejectMissingPaymentId() {

        assertValidation(
                () ->
                        transaction(
                                null,
                                "MOCK",
                                PaymentTransactionType.CHARGE,
                                1,
                                new BigDecimal("250000.00"),
                                "VND",
                                IDEMPOTENCY_KEY,
                                REQUESTED_AT),
                PaymentErrorCode.PAYMENT_ID_REQUIRED);
    }

    @Test
    void shouldRejectMissingProvider() {

        assertValidation(
                () ->
                        transaction(
                                PAYMENT_ID,
                                " ",
                                PaymentTransactionType.CHARGE,
                                1,
                                new BigDecimal("250000.00"),
                                "VND",
                                IDEMPOTENCY_KEY,
                                REQUESTED_AT),
                PaymentErrorCode.PROVIDER_REQUIRED);
    }

    @Test
    void shouldRejectProviderLongerThanSupportedLength() {

        assertValidation(
                () ->
                        transaction(
                                PAYMENT_ID,
                                "P".repeat(51),
                                PaymentTransactionType.CHARGE,
                                1,
                                new BigDecimal("250000.00"),
                                "VND",
                                IDEMPOTENCY_KEY,
                                REQUESTED_AT),
                PaymentErrorCode.PROVIDER_INVALID);
    }

    @Test
    void shouldRejectMissingTransactionType() {

        assertValidation(
                () ->
                        transaction(
                                PAYMENT_ID,
                                "MOCK",
                                null,
                                1,
                                new BigDecimal("250000.00"),
                                "VND",
                                IDEMPOTENCY_KEY,
                                REQUESTED_AT),
                PaymentErrorCode.TRANSACTION_TYPE_REQUIRED);
    }

    @Test
    void shouldRejectNonPositiveAttemptNumber() {

        assertValidation(
                () ->
                        transaction(
                                PAYMENT_ID,
                                "MOCK",
                                PaymentTransactionType.CHARGE,
                                0,
                                new BigDecimal("250000.00"),
                                "VND",
                                IDEMPOTENCY_KEY,
                                REQUESTED_AT),
                PaymentErrorCode.TRANSACTION_ATTEMPT_INVALID);
    }

    @Test
    void shouldRejectMissingAmount() {

        assertValidation(
                () ->
                        transaction(
                                PAYMENT_ID,
                                "MOCK",
                                PaymentTransactionType.CHARGE,
                                1,
                                null,
                                "VND",
                                IDEMPOTENCY_KEY,
                                REQUESTED_AT),
                PaymentErrorCode.AMOUNT_REQUIRED);
    }

    @Test
    void shouldRejectNegativeAmount() {

        assertValidation(
                () ->
                        transaction(
                                PAYMENT_ID,
                                "MOCK",
                                PaymentTransactionType.CHARGE,
                                1,
                                new BigDecimal("-0.01"),
                                "VND",
                                IDEMPOTENCY_KEY,
                                REQUESTED_AT),
                PaymentErrorCode.AMOUNT_INVALID);
    }

    @Test
    void shouldRejectInvalidCurrency() {

        assertValidation(
                () ->
                        transaction(
                                PAYMENT_ID,
                                "MOCK",
                                PaymentTransactionType.CHARGE,
                                1,
                                new BigDecimal("250000.00"),
                                "VN1",
                                IDEMPOTENCY_KEY,
                                REQUESTED_AT),
                PaymentErrorCode.CURRENCY_INVALID);
    }

    @Test
    void shouldRejectMissingIdempotencyKey() {

        assertValidation(
                () ->
                        transaction(
                                PAYMENT_ID,
                                "MOCK",
                                PaymentTransactionType.CHARGE,
                                1,
                                new BigDecimal("250000.00"),
                                "VND",
                                " ",
                                REQUESTED_AT),
                PaymentErrorCode.IDEMPOTENCY_KEY_REQUIRED);
    }

    @Test
    void shouldRejectIdempotencyKeyLongerThanSupportedLength() {

        assertValidation(
                () ->
                        transaction(
                                PAYMENT_ID,
                                "MOCK",
                                PaymentTransactionType.CHARGE,
                                1,
                                new BigDecimal("250000.00"),
                                "VND",
                                "K".repeat(201),
                                REQUESTED_AT),
                PaymentErrorCode.IDEMPOTENCY_KEY_INVALID);
    }

    @Test
    void shouldRejectMissingRequestedTime() {

        assertValidation(
                () ->
                        transaction(
                                PAYMENT_ID,
                                "MOCK",
                                PaymentTransactionType.CHARGE,
                                1,
                                new BigDecimal("250000.00"),
                                "VND",
                                IDEMPOTENCY_KEY,
                                null),
                PaymentErrorCode.REQUESTED_AT_REQUIRED);
    }

    private static PaymentTransaction transaction(
            UUID paymentId,
            String provider,
            PaymentTransactionType transactionType,
            int attemptNumber,
            BigDecimal amount,
            String currency,
            String idempotencyKey,
            OffsetDateTime requestedAt) {

        return new PaymentTransaction(
                paymentId,
                provider,
                transactionType,
                attemptNumber,
                amount,
                currency,
                idempotencyKey,
                requestedAt);
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
