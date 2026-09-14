package com.cinema.booking.event.validation;

import com.cinema.booking.event.payload.PaymentFailedPayload;
import com.cinema.booking.event.payload.PaymentSucceededPayload;
import com.cinema.booking.exception.BookingErrorCode;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.outbox.model.OutboxEventMessage;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Component
public class DefaultPaymentResultPayloadValidator implements PaymentResultPayloadValidator {

    private static final int MAX_AMOUNT_PRECISION = 19;

    private static final int MAX_AMOUNT_SCALE = 2;

    private static final int MAX_PROVIDER_LENGTH = 50;

    private static final int MAX_PROVIDER_REFERENCE_LENGTH = 255;

    private static final int MAX_FAILURE_MESSAGE_LENGTH = 500;

    private static final Set<String> APPROVED_FAILURE_CODES =
            Set.of(
                    "PAYMENT_DECLINED",
                    "PAYMENT_TIMEOUT",
                    "PROVIDER_UNAVAILABLE",
                    "INVALID_PAYMENT_REQUEST",
                    "RESERVATION_EXPIRED",
                    "DUPLICATE_PAYMENT");

    @Override
    public void validateSucceeded(
            String partitionKey, OutboxEventMessage message, PaymentSucceededPayload payload) {

        requireContext(message, payload);

        validateIdentifiers(partitionKey, message, payload.paymentId(), payload.bookingId());

        validateAmount(payload.amount());
        validateCurrency(payload.currency());
        validateProvider(payload.provider());
        validateProviderReference(payload.providerReference());
        validateTerminalTime(message.occurredAt(), payload.paidAt());
    }

    @Override
    public void validateFailed(
            String partitionKey, OutboxEventMessage message, PaymentFailedPayload payload) {

        requireContext(message, payload);

        validateIdentifiers(partitionKey, message, payload.paymentId(), payload.bookingId());

        validateFailureCode(payload.failureCode());
        validateFailureMessage(payload.message());
        validateTerminalTime(message.occurredAt(), payload.failedAt());

        if (payload.retryable()) {
            throw invalidPayload();
        }
    }

    private static void requireContext(OutboxEventMessage message, Object payload) {

        if (message == null || payload == null) {
            throw invalidPayload();
        }
    }

    private static void validateIdentifiers(
            String partitionKey, OutboxEventMessage message, UUID paymentId, UUID bookingId) {

        requireUuidV7(paymentId);
        requireUuidV7(bookingId);

        if (!paymentId.equals(message.aggregateId())) {
            throw invalidPayload();
        }

        if (partitionKey == null || !bookingId.toString().equals(partitionKey)) {

            throw new ValidationException(BookingErrorCode.EVENT_PARTITION_KEY_INVALID);
        }
    }

    private static void validateAmount(BigDecimal amount) {

        if (amount == null) {
            throw new ValidationException(BookingErrorCode.TOTAL_AMOUNT_REQUIRED);
        }

        if (amount.signum() < 0) {
            throw new ValidationException(BookingErrorCode.INVALID_TOTAL_AMOUNT);
        }

        BigDecimal normalized = amount.stripTrailingZeros();

        int normalizedScale = Math.max(normalized.scale(), 0);

        int integerDigits = normalized.precision() - normalized.scale();

        if (normalizedScale > MAX_AMOUNT_SCALE
                || integerDigits > MAX_AMOUNT_PRECISION - MAX_AMOUNT_SCALE) {

            throw new ValidationException(BookingErrorCode.INVALID_TOTAL_AMOUNT);
        }
    }

    private static void validateCurrency(String currency) {

        if (currency == null || currency.isBlank()) {
            throw new ValidationException(BookingErrorCode.CURRENCY_REQUIRED);
        }

        if (currency.length() != 3
                || !currency.chars().allMatch(character -> character >= 'A' && character <= 'Z')) {

            throw new ValidationException(BookingErrorCode.INVALID_CURRENCY);
        }
    }

    private static void validateProvider(String provider) {

        if (provider == null
                || provider.isBlank()
                || provider.length() > MAX_PROVIDER_LENGTH
                || !provider.equals(provider.strip())
                || !provider.equals(provider.toUpperCase(Locale.ROOT))) {

            throw invalidPayload();
        }
    }

    private static void validateProviderReference(String providerReference) {

        if (providerReference == null
                || providerReference.isBlank()
                || providerReference.length() > MAX_PROVIDER_REFERENCE_LENGTH
                || !providerReference.equals(providerReference.strip())) {

            throw invalidPayload();
        }
    }

    private static void validateFailureCode(String failureCode) {

        if (failureCode == null || !APPROVED_FAILURE_CODES.contains(failureCode)) {

            throw invalidPayload();
        }
    }

    private static void validateFailureMessage(String message) {

        /*
         * RESERVATION_EXPIRED is currently emitted with message = null.
         * A non-null message must already be sanitized and normalized.
         */
        if (message == null) {
            return;
        }

        if (message.isBlank()
                || message.length() > MAX_FAILURE_MESSAGE_LENGTH
                || !message.equals(message.strip())) {

            throw invalidPayload();
        }
    }

    private static void validateTerminalTime(
            OffsetDateTime occurredAt, OffsetDateTime terminalTime) {

        if (occurredAt == null || terminalTime == null || !terminalTime.isEqual(occurredAt)) {

            throw invalidPayload();
        }
    }

    private static void requireUuidV7(UUID value) {

        if (value == null || value.version() != 7) {
            throw invalidPayload();
        }
    }

    private static ValidationException invalidPayload() {

        return new ValidationException(BookingErrorCode.EVENT_PAYLOAD_INVALID);
    }
}
