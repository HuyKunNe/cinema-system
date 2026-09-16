package com.cinema.payment.provider.model;

import com.cinema.common.exception.exception.ValidationException;
import com.cinema.payment.exception.PaymentErrorCode;

import java.util.Locale;
import java.util.UUID;

public record ClaimedProviderRefundOperation(
        UUID transactionId,
        String processingOwner,
        String provider,
        String idempotencyKey,
        ProviderRefundCommand command) {

    private static final int MAX_PROCESSING_OWNER_LENGTH = 150;
    private static final int MAX_PROVIDER_LENGTH = 50;
    private static final int MAX_IDEMPOTENCY_KEY_LENGTH = 200;

    public ClaimedProviderRefundOperation {

        if (transactionId == null) {
            throw new ValidationException(PaymentErrorCode.PAYMENT_TRANSACTION_ID_REQUIRED);
        }

        processingOwner = normalizeProcessingOwner(processingOwner);

        provider = normalizeProvider(provider);

        idempotencyKey = normalizeIdempotencyKey(idempotencyKey);

        if (command == null) {
            throw new ValidationException(PaymentErrorCode.PROVIDER_REFUND_COMMAND_REQUIRED);
        }
    }

    private static String normalizeProcessingOwner(String processingOwner) {

        if (processingOwner == null || processingOwner.isBlank()) {

            throw new ValidationException(PaymentErrorCode.PROCESSING_OWNER_REQUIRED);
        }

        String normalized = processingOwner.strip();

        if (normalized.length() > MAX_PROCESSING_OWNER_LENGTH) {

            throw new ValidationException(PaymentErrorCode.PROCESSING_OWNER_INVALID);
        }

        return normalized;
    }

    private static String normalizeProvider(String provider) {

        if (provider == null || provider.isBlank()) {
            throw new ValidationException(PaymentErrorCode.PROVIDER_REQUIRED);
        }

        String normalized = provider.strip().toUpperCase(Locale.ROOT);

        if (normalized.length() > MAX_PROVIDER_LENGTH) {
            throw new ValidationException(PaymentErrorCode.PROVIDER_INVALID);
        }

        return normalized;
    }

    private static String normalizeIdempotencyKey(String idempotencyKey) {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {

            throw new ValidationException(PaymentErrorCode.IDEMPOTENCY_KEY_REQUIRED);
        }

        String normalized = idempotencyKey.strip();

        if (normalized.length() > MAX_IDEMPOTENCY_KEY_LENGTH) {

            throw new ValidationException(PaymentErrorCode.IDEMPOTENCY_KEY_INVALID);
        }

        return normalized;
    }
}
