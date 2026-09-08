package com.cinema.payment.provider.model;

import com.cinema.common.exception.exception.ValidationException;
import com.cinema.payment.exception.PaymentErrorCode;

import java.net.URI;

public record ProviderChargeResult(
        ProviderOutcome outcome,
        String providerReference,
        URI redirectUri,
        String failureCode,
        String failureMessage) {

    private static final int MAX_REFERENCE_LENGTH = 255;

    private static final int MAX_FAILURE_CODE_LENGTH = 100;

    private static final int MAX_FAILURE_MESSAGE_LENGTH = 500;

    public ProviderChargeResult {

        if (outcome == null) {
            throw new ValidationException(PaymentErrorCode.PROVIDER_OUTCOME_REQUIRED);
        }

        providerReference = normalize(providerReference);
        failureCode = normalize(failureCode);
        failureMessage = normalize(failureMessage);

        validateLength(providerReference, MAX_REFERENCE_LENGTH);

        validateLength(failureCode, MAX_FAILURE_CODE_LENGTH);

        validateLength(failureMessage, MAX_FAILURE_MESSAGE_LENGTH);

        if (redirectUri != null && !redirectUri.isAbsolute()) {
            throw invalidResult();
        }

        switch (outcome) {
            case SUCCEEDED ->
                    validateSucceeded(providerReference, redirectUri, failureCode, failureMessage);

            case PENDING -> validatePending(providerReference, failureCode, failureMessage);

            case FAILED -> validateFailed(redirectUri, failureCode);

            case UNKNOWN -> validateUnknown(redirectUri, failureCode);
        }
    }

    public static ProviderChargeResult succeeded(String providerReference) {

        return new ProviderChargeResult(
                ProviderOutcome.SUCCEEDED, providerReference, null, null, null);
    }

    public static ProviderChargeResult pending(String providerReference, URI redirectUri) {

        return new ProviderChargeResult(
                ProviderOutcome.PENDING, providerReference, redirectUri, null, null);
    }

    public static ProviderChargeResult failed(
            String providerReference, String failureCode, String failureMessage) {

        return new ProviderChargeResult(
                ProviderOutcome.FAILED, providerReference, null, failureCode, failureMessage);
    }

    public static ProviderChargeResult unknown(
            String providerReference, String failureCode, String failureMessage) {

        return new ProviderChargeResult(
                ProviderOutcome.UNKNOWN, providerReference, null, failureCode, failureMessage);
    }

    private static void validateSucceeded(
            String providerReference, URI redirectUri, String failureCode, String failureMessage) {

        requireProviderReference(providerReference);

        if (redirectUri != null || failureCode != null || failureMessage != null) {

            throw invalidResult();
        }
    }

    private static void validatePending(
            String providerReference, String failureCode, String failureMessage) {

        requireProviderReference(providerReference);

        if (failureCode != null || failureMessage != null) {
            throw invalidResult();
        }
    }

    private static void validateFailed(URI redirectUri, String failureCode) {

        if (failureCode == null) {
            throw new ValidationException(PaymentErrorCode.PROVIDER_FAILURE_CODE_REQUIRED);
        }

        if (redirectUri != null) {
            throw invalidResult();
        }
    }

    private static void validateUnknown(URI redirectUri, String failureCode) {

        if (failureCode == null) {
            throw new ValidationException(PaymentErrorCode.PROVIDER_FAILURE_CODE_REQUIRED);
        }

        if (redirectUri != null) {
            throw invalidResult();
        }
    }

    private static void requireProviderReference(String providerReference) {

        if (providerReference == null) {
            throw new ValidationException(PaymentErrorCode.PROVIDER_REFERENCE_REQUIRED);
        }
    }

    private static void validateLength(String value, int maximumLength) {

        if (value != null && value.length() > maximumLength) {
            throw invalidResult();
        }
    }

    private static String normalize(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private static ValidationException invalidResult() {

        return new ValidationException(PaymentErrorCode.PROVIDER_RESULT_INVALID);
    }
}
