package com.cinema.payment.provider.webhook.model;

import com.cinema.common.exception.exception.ValidationException;
import com.cinema.payment.exception.PaymentErrorCode;
import com.cinema.payment.provider.model.ProviderOutcome;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Locale;

public record VerifiedProviderWebhook(
        String provider,
        String providerEventId,
        String providerReference,
        ProviderOutcome outcome,
        BigDecimal amount,
        String currency,
        OffsetDateTime occurredAt,
        String failureCode,
        String failureMessage) {

    private static final int MAX_PROVIDER_LENGTH = 50;

    private static final int MAX_EVENT_ID_LENGTH = 255;

    private static final int MAX_PROVIDER_REFERENCE_LENGTH = 255;

    private static final int MAX_FAILURE_CODE_LENGTH = 100;

    private static final int MAX_FAILURE_MESSAGE_LENGTH = 500;

    private static final int MAX_AMOUNT_PRECISION = 19;

    private static final int MAX_AMOUNT_SCALE = 2;

    public VerifiedProviderWebhook {

        provider = normalizeProvider(provider);
        providerEventId = normalizeProviderEventId(providerEventId);
        providerReference = normalizeProviderReference(providerReference);

        if (outcome == null) {
            throw new ValidationException(PaymentErrorCode.PROVIDER_OUTCOME_REQUIRED);
        }

        validateAmount(amount);
        currency = normalizeCurrency(currency);

        if (occurredAt == null) {
            throw new ValidationException(PaymentErrorCode.PROVIDER_WEBHOOK_OCCURRED_AT_REQUIRED);
        }

        failureCode = normalizeOptionalText(failureCode, MAX_FAILURE_CODE_LENGTH);

        failureMessage = normalizeOptionalText(failureMessage, MAX_FAILURE_MESSAGE_LENGTH);

        validateOutcomeEvidence(outcome, failureCode, failureMessage);
    }

    private static String normalizeProvider(String provider) {

        if (provider == null || provider.isBlank()) {
            throw new ValidationException(PaymentErrorCode.PROVIDER_REQUIRED);
        }

        String normalizedProvider = provider.strip().toUpperCase(Locale.ROOT);

        if (normalizedProvider.length() > MAX_PROVIDER_LENGTH) {
            throw new ValidationException(PaymentErrorCode.PROVIDER_INVALID);
        }

        return normalizedProvider;
    }

    private static String normalizeProviderEventId(String providerEventId) {

        if (providerEventId == null || providerEventId.isBlank()) {
            throw new ValidationException(PaymentErrorCode.PROVIDER_WEBHOOK_EVENT_ID_REQUIRED);
        }

        String normalizedProviderEventId = providerEventId.strip();

        if (normalizedProviderEventId.length() > MAX_EVENT_ID_LENGTH) {

            throw new ValidationException(PaymentErrorCode.PROVIDER_WEBHOOK_EVENT_ID_INVALID);
        }

        return normalizedProviderEventId;
    }

    private static String normalizeProviderReference(String providerReference) {

        if (providerReference == null || providerReference.isBlank()) {

            throw new ValidationException(PaymentErrorCode.PROVIDER_REFERENCE_REQUIRED);
        }

        String normalizedReference = providerReference.strip();

        if (normalizedReference.length() > MAX_PROVIDER_REFERENCE_LENGTH) {

            throw new ValidationException(PaymentErrorCode.PROVIDER_RESULT_INVALID);
        }

        return normalizedReference;
    }

    private static void validateAmount(BigDecimal amount) {

        if (amount == null) {
            throw new ValidationException(PaymentErrorCode.AMOUNT_REQUIRED);
        }

        if (amount.signum() < 0) {
            throw new ValidationException(PaymentErrorCode.AMOUNT_INVALID);
        }

        if (amount.precision() > MAX_AMOUNT_PRECISION || amount.scale() > MAX_AMOUNT_SCALE) {

            throw new ValidationException(PaymentErrorCode.AMOUNT_PRECISION_INVALID);
        }
    }

    private static String normalizeCurrency(String currency) {

        if (currency == null || currency.isBlank()) {
            throw new ValidationException(PaymentErrorCode.CURRENCY_REQUIRED);
        }

        String normalizedCurrency = currency.strip().toUpperCase(Locale.ROOT);

        if (normalizedCurrency.length() != 3
                || !normalizedCurrency.chars().allMatch(Character::isLetter)) {

            throw new ValidationException(PaymentErrorCode.CURRENCY_INVALID);
        }

        return normalizedCurrency;
    }

    private static String normalizeOptionalText(String value, int maximumLength) {

        if (value == null || value.isBlank()) {
            return null;
        }

        String normalizedValue = value.strip();

        if (normalizedValue.length() > maximumLength) {
            throw new ValidationException(PaymentErrorCode.PROVIDER_WEBHOOK_RESULT_INVALID);
        }

        return normalizedValue;
    }

    private static void validateOutcomeEvidence(
            ProviderOutcome outcome, String failureCode, String failureMessage) {

        boolean failureEvidenceRequired =
                outcome == ProviderOutcome.FAILED || outcome == ProviderOutcome.UNKNOWN;

        if (failureEvidenceRequired && failureCode == null) {
            throw new ValidationException(PaymentErrorCode.PROVIDER_FAILURE_CODE_REQUIRED);
        }

        if (!failureEvidenceRequired && (failureCode != null || failureMessage != null)) {

            throw new ValidationException(PaymentErrorCode.PROVIDER_WEBHOOK_RESULT_INVALID);
        }

        if (failureMessage != null && failureCode == null) {
            throw new ValidationException(PaymentErrorCode.PROVIDER_WEBHOOK_RESULT_INVALID);
        }
    }
}
