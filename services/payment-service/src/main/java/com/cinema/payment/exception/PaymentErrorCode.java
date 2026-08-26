package com.cinema.payment.exception;

import com.cinema.common.exception.code.ErrorCategory;
import com.cinema.common.exception.code.ErrorCode;

public final class PaymentErrorCode implements ErrorCode {

    public static final PaymentErrorCode BOOKING_ID_REQUIRED =
            validation("PAYMENT_BOOKING_ID_REQUIRED", "Booking ID is required");

    public static final PaymentErrorCode USER_ID_REQUIRED =
            validation("PAYMENT_USER_ID_REQUIRED", "User ID is required");

    public static final PaymentErrorCode PAYMENT_ATTEMPT_INVALID =
            validation("PAYMENT_ATTEMPT_INVALID", "Payment attempt must be greater than zero");

    public static final PaymentErrorCode AMOUNT_REQUIRED =
            validation("PAYMENT_AMOUNT_REQUIRED", "Payment amount is required");

    public static final PaymentErrorCode AMOUNT_INVALID =
            validation("PAYMENT_AMOUNT_INVALID", "Payment amount must be zero or greater");

    public static final PaymentErrorCode CURRENCY_REQUIRED =
            validation("PAYMENT_CURRENCY_REQUIRED", "Payment currency is required");

    public static final PaymentErrorCode CURRENCY_INVALID =
            validation(
                    "PAYMENT_CURRENCY_INVALID",
                    "Payment currency must contain exactly three letters");

    public static final PaymentErrorCode PROVIDER_REQUIRED =
            validation("PAYMENT_PROVIDER_REQUIRED", "Payment provider is required");

    public static final PaymentErrorCode HOLD_EXPIRATION_REQUIRED =
            validation("PAYMENT_HOLD_EXPIRATION_REQUIRED", "Payment hold expiration is required");

    public static final PaymentErrorCode REQUESTED_AT_REQUIRED =
            validation("PAYMENT_REQUESTED_AT_REQUIRED", "Payment request time is required");

    public static final PaymentErrorCode HOLD_EXPIRATION_INVALID =
            validation(
                    "PAYMENT_HOLD_EXPIRATION_INVALID",
                    "Payment hold expiration must be after the request time");

    public static final PaymentErrorCode SOURCE_EVENT_ID_REQUIRED =
            validation("PAYMENT_SOURCE_EVENT_ID_REQUIRED", "Payment source event ID is required");

    public static final PaymentErrorCode CORRELATION_ID_REQUIRED =
            validation("PAYMENT_CORRELATION_ID_REQUIRED", "Payment correlation ID is required");

    public static final PaymentErrorCode PAYMENT_ID_REQUIRED =
            validation("PAYMENT_ID_REQUIRED", "Payment ID is required");

    public static final PaymentErrorCode TRANSACTION_TYPE_REQUIRED =
            validation("PAYMENT_TRANSACTION_TYPE_REQUIRED", "Payment transaction type is required");

    public static final PaymentErrorCode TRANSACTION_ATTEMPT_INVALID =
            validation(
                    "PAYMENT_TRANSACTION_ATTEMPT_INVALID",
                    "Payment transaction attempt must be greater than zero");

    public static final PaymentErrorCode IDEMPOTENCY_KEY_REQUIRED =
            validation("PAYMENT_IDEMPOTENCY_KEY_REQUIRED", "Provider idempotency key is required");

    public static final PaymentErrorCode IDEMPOTENCY_KEY_INVALID =
            validation(
                    "PAYMENT_IDEMPOTENCY_KEY_INVALID",
                    "Provider idempotency key must not exceed 200 characters");

    public static final PaymentErrorCode PROVIDER_INVALID =
            validation(
                    "PAYMENT_PROVIDER_INVALID", "Payment provider must not exceed 50 characters");

    public static final PaymentErrorCode EVENT_MESSAGE_INVALID =
            validation("PAYMENT_EVENT_MESSAGE_INVALID", "Payment event message is invalid");

    public static final PaymentErrorCode EVENT_ID_REQUIRED =
            validation("PAYMENT_EVENT_ID_REQUIRED", "Payment event ID is required");

    public static final PaymentErrorCode EVENT_ID_INVALID =
            validation("PAYMENT_EVENT_ID_INVALID", "Payment event ID must be a UUID v7");

    public static final PaymentErrorCode EVENT_TYPE_INVALID =
            validation("PAYMENT_EVENT_TYPE_INVALID", "Payment event type is not supported");

    public static final PaymentErrorCode EVENT_VERSION_INVALID =
            validation("PAYMENT_EVENT_VERSION_INVALID", "Payment event version is not supported");

    public static final PaymentErrorCode EVENT_PRODUCER_INVALID =
            validation("PAYMENT_EVENT_PRODUCER_INVALID", "Payment event producer is invalid");

    public static final PaymentErrorCode EVENT_AGGREGATE_INVALID =
            validation("PAYMENT_EVENT_AGGREGATE_INVALID", "Payment event aggregate is invalid");

    public static final PaymentErrorCode EVENT_PARTITION_KEY_INVALID =
            validation(
                    "PAYMENT_EVENT_PARTITION_KEY_INVALID",
                    "Payment event partition key must match the booking ID");

    public static final PaymentErrorCode PAYMENT_ATTEMPT_PAYLOAD_MISMATCH =
            new PaymentErrorCode(
                    ErrorCategory.BUSINESS,
                    "PAYMENT_ATTEMPT_PAYLOAD_MISMATCH",
                    "Payment attempt already exists with different request data");

    public static final PaymentErrorCode PAYMENT_NOT_RECEIVED =
            new PaymentErrorCode(
                    ErrorCategory.BUSINESS,
                    "PAYMENT_NOT_RECEIVED",
                    "Payment is not in received state");

    public static final PaymentErrorCode PAYMENT_NOT_EXPIRED =
            new PaymentErrorCode(
                    ErrorCategory.BUSINESS,
                    "PAYMENT_NOT_EXPIRED",
                    "Payment reservation has not expired");

    public static final PaymentErrorCode CURRENT_TIME_REQUIRED =
            validation("PAYMENT_CURRENT_TIME_REQUIRED", "Current time is required");

    public static final PaymentErrorCode EVENT_CORRELATION_ID_INVALID =
            validation(
                    "PAYMENT_EVENT_CORRELATION_ID_INVALID",
                    "Payment event correlation ID must be a UUID v7");

    public static final PaymentErrorCode EVENT_CAUSATION_ID_REQUIRED =
            validation(
                    "PAYMENT_EVENT_CAUSATION_ID_REQUIRED",
                    "Payment event causation ID is required");

    public static final PaymentErrorCode EVENT_CAUSATION_ID_INVALID =
            validation(
                    "PAYMENT_EVENT_CAUSATION_ID_INVALID",
                    "Payment event causation ID must be a UUID v7");

    public static final PaymentErrorCode EVENT_OCCURRED_AT_REQUIRED =
            validation(
                    "PAYMENT_EVENT_OCCURRED_AT_REQUIRED",
                    "Payment event occurrence time is required");

    public static final PaymentErrorCode EVENT_PAYLOAD_INVALID =
            validation("PAYMENT_EVENT_PAYLOAD_INVALID", "Payment event payload is invalid");

    public static final PaymentErrorCode EVENT_PAYLOAD_AGGREGATE_MISMATCH =
            validation(
                    "PAYMENT_EVENT_PAYLOAD_AGGREGATE_MISMATCH",
                    "Payment payload booking ID must match the event aggregate ID");

    public static final PaymentErrorCode AMOUNT_PRECISION_INVALID =
            validation("PAYMENT_AMOUNT_PRECISION_INVALID", "Payment amount exceeds DECIMAL(19, 2)");

    public static final PaymentErrorCode PROCESSED_EVENT_ID_REQUIRED =
            validation("PAYMENT_PROCESSED_EVENT_ID_REQUIRED", "Processed event ID is required");

    public static final PaymentErrorCode CONSUMER_NAME_REQUIRED =
            validation("PAYMENT_CONSUMER_NAME_REQUIRED", "Consumer name is required");

    public static final PaymentErrorCode EVENT_TYPE_REQUIRED =
            validation("PAYMENT_EVENT_TYPE_REQUIRED", "Event type is required");

    public static final PaymentErrorCode EVENT_VERSION_REQUIRED =
            validation("PAYMENT_EVENT_VERSION_REQUIRED", "Event version is required");

    public static final PaymentErrorCode PROCESSED_AT_REQUIRED =
            validation("PAYMENT_PROCESSED_AT_REQUIRED", "Processed time is required");

    private static PaymentErrorCode validation(String code, String message) {

        return new PaymentErrorCode(ErrorCategory.VALIDATION, code, message);
    }

    private final ErrorCategory category;

    private final String code;

    private final String message;

    private PaymentErrorCode(ErrorCategory category, String code, String message) {

        this.category = category;
        this.code = code;
        this.message = message;
    }

    @Override
    public String code() {

        return code;
    }

    @Override
    public String message() {

        return message;
    }

    @Override
    public ErrorCategory category() {

        return category;
    }

    private PaymentErrorCode() {

        throw new UnsupportedOperationException();
    }
}
