package com.cinema.payment.provider.model;

import com.cinema.common.exception.exception.ValidationException;
import com.cinema.payment.exception.PaymentErrorCode;

public record ProviderRefundResult(
        ProviderOutcome outcome,
        String providerReference,
        String failureCode,
        String failureMessage) {

    public ProviderRefundResult {

        if (outcome == null) {
            throw new ValidationException(PaymentErrorCode.PROVIDER_RESULT_INVALID);
        }

        providerReference = normalize(providerReference, 255);
        failureCode = normalize(failureCode, 100);
        failureMessage = normalize(failureMessage, 500);

        switch (outcome) {
            case SUCCEEDED -> {
                if (providerReference == null) {
                    throw new ValidationException(PaymentErrorCode.PROVIDER_RESULT_INVALID);
                }

                failureCode = null;
                failureMessage = null;
            }

            case FAILED -> {
                if (failureCode == null) {
                    throw new ValidationException(PaymentErrorCode.PROVIDER_RESULT_INVALID);
                }
            }

            case PENDING -> {
                failureCode = null;
                failureMessage = null;
            }

            case UNKNOWN -> {
                if (failureCode == null) {
                    throw new ValidationException(PaymentErrorCode.PROVIDER_RESULT_INVALID);
                }
            }
        }
    }

    public static ProviderRefundResult succeeded(String providerReference) {

        return new ProviderRefundResult(ProviderOutcome.SUCCEEDED, providerReference, null, null);
    }

    public static ProviderRefundResult failed(String failureCode, String failureMessage) {

        return new ProviderRefundResult(ProviderOutcome.FAILED, null, failureCode, failureMessage);
    }

    public static ProviderRefundResult pending(String providerReference) {

        return new ProviderRefundResult(ProviderOutcome.PENDING, providerReference, null, null);
    }

    public static ProviderRefundResult unknown(
            String providerReference, String failureCode, String failureMessage) {

        return new ProviderRefundResult(
                ProviderOutcome.UNKNOWN, providerReference, failureCode, failureMessage);
    }

    private static String normalize(String value, int maximumLength) {

        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.strip();

        if (normalized.length() > maximumLength) {
            throw new ValidationException(PaymentErrorCode.PROVIDER_RESULT_INVALID);
        }

        return normalized;
    }
}
