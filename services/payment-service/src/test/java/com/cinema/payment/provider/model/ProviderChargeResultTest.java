package com.cinema.payment.provider.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cinema.common.exception.exception.ValidationException;
import com.cinema.payment.exception.PaymentErrorCode;

import org.junit.jupiter.api.Test;

import java.net.URI;

class ProviderChargeResultTest {

    @Test
    void succeededResultShouldRequireProviderReference() {

        ProviderChargeResult result = ProviderChargeResult.succeeded(" momo-transaction-123 ");

        assertThat(result.outcome()).isEqualTo(ProviderOutcome.SUCCEEDED);

        assertThat(result.providerReference()).isEqualTo("momo-transaction-123");

        assertThat(result.redirectUri()).isNull();

        assertThat(result.failureCode()).isNull();
    }

    @Test
    void pendingResultShouldContainAbsoluteRedirectUri() {

        URI redirectUri = URI.create("https://payment.example.test/checkout/123");

        ProviderChargeResult result =
                ProviderChargeResult.pending("provider-order-123", redirectUri);

        assertThat(result.outcome()).isEqualTo(ProviderOutcome.PENDING);

        assertThat(result.providerReference()).isEqualTo("provider-order-123");

        assertThat(result.redirectUri()).isEqualTo(redirectUri);
    }

    @Test
    void terminalFailureShouldContainStableFailureCode() {

        ProviderChargeResult result =
                ProviderChargeResult.failed(null, "PAYMENT_DECLINED", "Payment was declined");

        assertThat(result.outcome()).isEqualTo(ProviderOutcome.FAILED);

        assertThat(result.failureCode()).isEqualTo("PAYMENT_DECLINED");
    }

    @Test
    void unknownOutcomeShouldRemainDifferentFromFailure() {

        ProviderChargeResult result =
                ProviderChargeResult.unknown(
                        "provider-order-123", "PROVIDER_TIMEOUT", "Provider outcome is unknown");

        assertThat(result.outcome()).isEqualTo(ProviderOutcome.UNKNOWN);

        assertThat(result.outcome()).isNotEqualTo(ProviderOutcome.FAILED);
    }

    @Test
    void succeededResultWithoutReferenceShouldBeRejected() {

        assertThatThrownBy(() -> ProviderChargeResult.succeeded(" "))
                .isInstanceOf(ValidationException.class)
                .satisfies(
                        throwable ->
                                assertThat(((ValidationException) throwable).getErrorCode())
                                        .isEqualTo(PaymentErrorCode.PROVIDER_REFERENCE_REQUIRED));
    }

    @Test
    void relativeRedirectUriShouldBeRejected() {

        assertThatThrownBy(
                        () ->
                                ProviderChargeResult.pending(
                                        "provider-order-123", URI.create("/checkout/123")))
                .isInstanceOf(ValidationException.class)
                .satisfies(
                        throwable ->
                                assertThat(((ValidationException) throwable).getErrorCode())
                                        .isEqualTo(PaymentErrorCode.PROVIDER_RESULT_INVALID));
    }

    @Test
    void failedResultWithoutFailureCodeShouldBeRejected() {

        assertThatThrownBy(
                        () ->
                                ProviderChargeResult.failed(
                                        null, " ", "Provider rejected the operation"))
                .isInstanceOf(ValidationException.class)
                .satisfies(
                        throwable ->
                                assertThat(((ValidationException) throwable).getErrorCode())
                                        .isEqualTo(
                                                PaymentErrorCode.PROVIDER_FAILURE_CODE_REQUIRED));
    }
}
