package com.cinema.payment.provider.mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.payment.exception.PaymentErrorCode;
import com.cinema.payment.provider.model.ProviderChargeCommand;
import com.cinema.payment.provider.model.ProviderChargeResult;
import com.cinema.payment.provider.model.ProviderOutcome;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

class MockPaymentProviderTest {

    private static final OffsetDateTime HOLD_EXPIRES_AT =
            OffsetDateTime.parse("2026-09-08T12:30:00Z");

    private static final String IDEMPOTENCY_KEY = "charge:01991d2b-bd4a-7000-8000-000000000001";

    private final MockPaymentProvider provider = new MockPaymentProvider();

    @Test
    void providerCodeShouldBeMock() {

        assertThat(provider.providerCode()).isEqualTo(MockPaymentProvider.PROVIDER_CODE);
    }

    @Test
    void sameIdempotencyKeyShouldReturnSameSuccessfulResult() {

        ProviderChargeCommand command = command();

        ProviderChargeResult firstResult = provider.initiateCharge(command, IDEMPOTENCY_KEY);

        ProviderChargeResult secondResult = provider.initiateCharge(command, IDEMPOTENCY_KEY);

        assertThat(firstResult).isEqualTo(secondResult);
        assertThat(firstResult.outcome()).isEqualTo(ProviderOutcome.SUCCEEDED);
        assertThat(firstResult.providerReference()).isEqualTo("mock-" + IDEMPOTENCY_KEY);
        assertThat(firstResult.redirectUri()).isNull();
        assertThat(firstResult.failureCode()).isNull();
        assertThat(firstResult.failureMessage()).isNull();
    }

    @Test
    void differentIdempotencyKeysShouldReturnDifferentReferences() {

        ProviderChargeCommand command = command();

        ProviderChargeResult firstResult = provider.initiateCharge(command, IDEMPOTENCY_KEY);

        ProviderChargeResult secondResult =
                provider.initiateCharge(command, IDEMPOTENCY_KEY + "-retry");

        assertThat(firstResult.providerReference()).isNotEqualTo(secondResult.providerReference());
    }

    @Test
    void nullCommandShouldBeRejected() {

        assertThatThrownBy(() -> provider.initiateCharge(null, IDEMPOTENCY_KEY))
                .isInstanceOf(ValidationException.class)
                .satisfies(
                        throwable ->
                                assertThat(((ValidationException) throwable).getErrorCode())
                                        .isEqualTo(
                                                PaymentErrorCode.PROVIDER_CHARGE_COMMAND_REQUIRED));
    }

    @Test
    void blankIdempotencyKeyShouldBeRejected() {

        assertThatThrownBy(() -> provider.initiateCharge(command(), " "))
                .isInstanceOf(ValidationException.class)
                .satisfies(
                        throwable ->
                                assertThat(((ValidationException) throwable).getErrorCode())
                                        .isEqualTo(PaymentErrorCode.IDEMPOTENCY_KEY_REQUIRED));
    }

    @Test
    void oversizedIdempotencyKeyShouldBeRejected() {

        assertThatThrownBy(() -> provider.initiateCharge(command(), "a".repeat(201)))
                .isInstanceOf(ValidationException.class)
                .satisfies(
                        throwable ->
                                assertThat(((ValidationException) throwable).getErrorCode())
                                        .isEqualTo(PaymentErrorCode.IDEMPOTENCY_KEY_INVALID));
    }

    private static ProviderChargeCommand command() {

        return new ProviderChargeCommand(
                UuidGenerator.next(),
                UuidGenerator.next(),
                new BigDecimal("125000.00"),
                "VND",
                HOLD_EXPIRES_AT);
    }
}
