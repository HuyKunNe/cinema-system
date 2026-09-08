package com.cinema.payment.provider.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.payment.exception.PaymentErrorCode;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

class ProviderChargeCommandTest {

    private static final OffsetDateTime HOLD_EXPIRES_AT =
            OffsetDateTime.parse("2026-09-08T10:30:00Z");

    @Test
    void validCommandShouldNormalizeCurrency() {

        ProviderChargeCommand command =
                new ProviderChargeCommand(
                        UuidGenerator.next(),
                        UuidGenerator.next(),
                        new BigDecimal("180000.00"),
                        " vnd ",
                        HOLD_EXPIRES_AT);

        assertThat(command.amount()).isEqualByComparingTo("180000.00");

        assertThat(command.currency()).isEqualTo("VND");

        assertThat(command.holdExpiresAt()).isEqualTo(HOLD_EXPIRES_AT);
    }

    @Test
    void missingPaymentIdShouldBeRejected() {

        assertThatThrownBy(
                        () ->
                                new ProviderChargeCommand(
                                        null,
                                        UuidGenerator.next(),
                                        new BigDecimal("180000.00"),
                                        "VND",
                                        HOLD_EXPIRES_AT))
                .isInstanceOf(ValidationException.class)
                .satisfies(
                        throwable ->
                                assertThat(((ValidationException) throwable).getErrorCode())
                                        .isEqualTo(PaymentErrorCode.PAYMENT_ID_REQUIRED));
    }

    @Test
    void negativeAmountShouldBeRejected() {

        assertThatThrownBy(
                        () ->
                                new ProviderChargeCommand(
                                        UuidGenerator.next(),
                                        UuidGenerator.next(),
                                        new BigDecimal("-1.00"),
                                        "VND",
                                        HOLD_EXPIRES_AT))
                .isInstanceOf(ValidationException.class)
                .satisfies(
                        throwable ->
                                assertThat(((ValidationException) throwable).getErrorCode())
                                        .isEqualTo(PaymentErrorCode.AMOUNT_INVALID));
    }

    @Test
    void invalidCurrencyShouldBeRejected() {

        assertThatThrownBy(
                        () ->
                                new ProviderChargeCommand(
                                        UuidGenerator.next(),
                                        UuidGenerator.next(),
                                        new BigDecimal("180000.00"),
                                        "VN",
                                        HOLD_EXPIRES_AT))
                .isInstanceOf(ValidationException.class)
                .satisfies(
                        throwable ->
                                assertThat(((ValidationException) throwable).getErrorCode())
                                        .isEqualTo(PaymentErrorCode.CURRENCY_INVALID));
    }
}
