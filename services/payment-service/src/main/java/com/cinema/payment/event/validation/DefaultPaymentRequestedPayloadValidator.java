package com.cinema.payment.event.validation;

import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.cinema.payment.event.payload.PaymentRequestedPayload;
import com.cinema.payment.exception.PaymentErrorCode;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Component
public class DefaultPaymentRequestedPayloadValidator implements PaymentRequestedPayloadValidator {

    private static final int MAX_AMOUNT_PRECISION = 19;

    private static final int MAX_AMOUNT_SCALE = 2;

    @Override
    public void validate(OutboxEventMessage message, PaymentRequestedPayload payload) {

        if (message == null || payload == null) {
            throw invalidPayload();
        }

        validateIdentifiers(message, payload);
        validateAmount(payload.amount());
        validateCurrency(payload.currency());
        validateAttempt(payload.paymentAttempt());
        validateTimeWindow(payload.requestedAt(), payload.holdExpiresAt());
    }

    private static void validateIdentifiers(
            OutboxEventMessage message, PaymentRequestedPayload payload) {

        requireUuidV7(payload.bookingId());
        requireUuidV7(payload.userId());

        if (!payload.bookingId().equals(message.aggregateId())) {

            throw new ValidationException(PaymentErrorCode.EVENT_PAYLOAD_AGGREGATE_MISMATCH);
        }
    }

    private static void validateAmount(BigDecimal amount) {

        if (amount == null) {
            throw new ValidationException(PaymentErrorCode.AMOUNT_REQUIRED);
        }

        if (amount.signum() < 0) {
            throw new ValidationException(PaymentErrorCode.AMOUNT_INVALID);
        }

        BigDecimal normalized = amount.stripTrailingZeros();

        int normalizedScale = Math.max(normalized.scale(), 0);

        int integerDigits = normalized.precision() - normalized.scale();

        if (normalizedScale > MAX_AMOUNT_SCALE
                || integerDigits > MAX_AMOUNT_PRECISION - MAX_AMOUNT_SCALE) {

            throw new ValidationException(PaymentErrorCode.AMOUNT_PRECISION_INVALID);
        }
    }

    private static void validateCurrency(String currency) {

        if (currency == null || currency.isBlank()) {
            throw new ValidationException(PaymentErrorCode.CURRENCY_REQUIRED);
        }

        if (currency.length() != 3
                || !currency.chars().allMatch(character -> character >= 'A' && character <= 'Z')) {

            throw new ValidationException(PaymentErrorCode.CURRENCY_INVALID);
        }
    }

    private static void validateAttempt(int paymentAttempt) {

        if (paymentAttempt <= 0) {
            throw new ValidationException(PaymentErrorCode.PAYMENT_ATTEMPT_INVALID);
        }
    }

    private static void validateTimeWindow(
            OffsetDateTime requestedAt, OffsetDateTime holdExpiresAt) {

        if (requestedAt == null) {
            throw new ValidationException(PaymentErrorCode.REQUESTED_AT_REQUIRED);
        }

        if (holdExpiresAt == null) {
            throw new ValidationException(PaymentErrorCode.HOLD_EXPIRATION_REQUIRED);
        }

        if (!holdExpiresAt.isAfter(requestedAt)) {
            throw new ValidationException(PaymentErrorCode.HOLD_EXPIRATION_INVALID);
        }
    }

    private static void requireUuidV7(UUID value) {

        if (value == null || value.version() != 7) {
            throw invalidPayload();
        }
    }

    private static ValidationException invalidPayload() {

        return new ValidationException(PaymentErrorCode.EVENT_PAYLOAD_INVALID);
    }
}
