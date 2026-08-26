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
