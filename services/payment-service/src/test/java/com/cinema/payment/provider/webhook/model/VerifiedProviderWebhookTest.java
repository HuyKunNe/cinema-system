package com.cinema.payment.provider.webhook.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cinema.common.exception.exception.ValidationException;
import com.cinema.payment.exception.PaymentErrorCode;
import com.cinema.payment.provider.model.ProviderOutcome;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

class VerifiedProviderWebhookTest {

    private static final OffsetDateTime OCCURRED_AT = OffsetDateTime.parse("2026-09-08T11:00:00Z");

    @Test
    void shouldCreateNormalizedSuccessfulWebhook() {

        VerifiedProviderWebhook webhook =
                new VerifiedProviderWebhook(
                        " mock ",
                        " provider-event-1 ",
                        " provider-reference-1 ",
                        ProviderOutcome.SUCCEEDED,
                        new BigDecimal("125000.00"),
                        "vnd",
                        OCCURRED_AT,
                        null,
                        null);

        assertThat(webhook.provider()).isEqualTo("MOCK");
        assertThat(webhook.providerEventId()).isEqualTo("provider-event-1");
        assertThat(webhook.providerReference()).isEqualTo("provider-reference-1");
        assertThat(webhook.currency()).isEqualTo("VND");
        assertThat(webhook.outcome()).isEqualTo(ProviderOutcome.SUCCEEDED);
    }

    @Test
    void failedWebhookShouldRequireFailureCode() {

        assertValidation(
                () ->
                        new VerifiedProviderWebhook(
                                "MOCK",
                                "provider-event-1",
                                "provider-reference-1",
                                ProviderOutcome.FAILED,
                                new BigDecimal("125000.00"),
                                "VND",
                                OCCURRED_AT,
                                null,
                                "Payment was declined"),
                PaymentErrorCode.PROVIDER_FAILURE_CODE_REQUIRED);
    }

    @Test
    void unknownWebhookWithSafeReasonShouldBeAccepted() {

        VerifiedProviderWebhook webhook =
                new VerifiedProviderWebhook(
                        "MOCK",
                        "provider-event-unknown",
                        "provider-reference-1",
                        ProviderOutcome.UNKNOWN,
                        new BigDecimal("125000.00"),
                        "VND",
                        OCCURRED_AT,
                        "PROVIDER_OUTCOME_UNKNOWN",
                        "Provider outcome could not be determined");

        assertThat(webhook.outcome()).isEqualTo(ProviderOutcome.UNKNOWN);

        assertThat(webhook.failureCode()).isEqualTo("PROVIDER_OUTCOME_UNKNOWN");
    }

    @Test
    void successfulWebhookShouldRejectFailureEvidence() {

        assertValidation(
                () ->
                        new VerifiedProviderWebhook(
                                "MOCK",
                                "provider-event-1",
                                "provider-reference-1",
                                ProviderOutcome.SUCCEEDED,
                                new BigDecimal("125000.00"),
                                "VND",
                                OCCURRED_AT,
                                "UNEXPECTED_FAILURE",
                                null),
                PaymentErrorCode.PROVIDER_WEBHOOK_RESULT_INVALID);
    }

    @Test
    void oversizedProviderEventIdShouldBeRejected() {

        assertValidation(
                () ->
                        new VerifiedProviderWebhook(
                                "MOCK",
                                "E".repeat(256),
                                "provider-reference-1",
                                ProviderOutcome.SUCCEEDED,
                                new BigDecimal("125000.00"),
                                "VND",
                                OCCURRED_AT,
                                null,
                                null),
                PaymentErrorCode.PROVIDER_WEBHOOK_EVENT_ID_INVALID);
    }

    @Test
    void excessiveAmountPrecisionShouldBeRejected() {

        assertValidation(
                () ->
                        new VerifiedProviderWebhook(
                                "MOCK",
                                "provider-event-1",
                                "provider-reference-1",
                                ProviderOutcome.SUCCEEDED,
                                new BigDecimal("123456789012345678.90"),
                                "VND",
                                OCCURRED_AT,
                                null,
                                null),
                PaymentErrorCode.AMOUNT_PRECISION_INVALID);
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
