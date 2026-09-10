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

    public static final PaymentErrorCode PROVIDER_OUTCOME_REQUIRED =
            validation("PAYMENT_PROVIDER_OUTCOME_REQUIRED", "Payment provider outcome is required");

    public static final PaymentErrorCode PROVIDER_REFERENCE_REQUIRED =
            validation(
                    "PAYMENT_PROVIDER_REFERENCE_REQUIRED",
                    "Payment provider reference is required");

    public static final PaymentErrorCode PROVIDER_FAILURE_CODE_REQUIRED =
            validation(
                    "PAYMENT_PROVIDER_FAILURE_CODE_REQUIRED",
                    "Payment provider failure code is required");
    public static final PaymentErrorCode PROVIDER_CHARGE_COMMAND_REQUIRED =
            validation(
                    "PAYMENT_PROVIDER_CHARGE_COMMAND_REQUIRED",
                    "Provider charge command is required");

    public static final PaymentErrorCode PROVIDER_NOT_SUPPORTED =
            validation("PAYMENT_PROVIDER_NOT_SUPPORTED", "Payment provider is not supported");

    public static final PaymentErrorCode PROVIDER_CONFIGURATION_INVALID =
            new PaymentErrorCode(
                    ErrorCategory.SYSTEM,
                    "PAYMENT_PROVIDER_CONFIGURATION_INVALID",
                    "Payment provider configuration is invalid");

    public static final PaymentErrorCode PROVIDER_RESULT_INVALID =
            validation("PAYMENT_PROVIDER_RESULT_INVALID", "Payment provider result is invalid");

    public static final PaymentErrorCode PROCESSING_OWNER_REQUIRED =
            validation("PAYMENT_PROCESSING_OWNER_REQUIRED", "Payment processing owner is required");

    public static final PaymentErrorCode PROCESSING_OWNER_INVALID =
            validation(
                    "PAYMENT_PROCESSING_OWNER_INVALID",
                    "Payment processing owner must not exceed 150 characters");

    public static final PaymentErrorCode PROCESSING_EXPIRATION_REQUIRED =
            validation(
                    "PAYMENT_PROCESSING_EXPIRATION_REQUIRED",
                    "Payment processing expiration is required");

    public static final PaymentErrorCode PROCESSING_EXPIRATION_INVALID =
            validation(
                    "PAYMENT_PROCESSING_EXPIRATION_INVALID",
                    "Payment processing expiration must be after the claim time");

    public static final PaymentErrorCode TRANSACTION_NOT_CLAIMABLE =
            new PaymentErrorCode(
                    ErrorCategory.BUSINESS,
                    "PAYMENT_TRANSACTION_NOT_CLAIMABLE",
                    "Payment transaction is not claimable");

    public static final PaymentErrorCode PAYMENT_TRANSACTION_ID_REQUIRED =
            validation("PAYMENT_TRANSACTION_ID_REQUIRED", "Payment transaction ID is required");

    public static final PaymentErrorCode PROVIDER_OPERATION_REQUIRED =
            validation(
                    "PAYMENT_PROVIDER_OPERATION_REQUIRED",
                    "Claimed provider operation is required");

    public static final PaymentErrorCode PAYMENT_NOT_FOUND =
            new PaymentErrorCode(
                    ErrorCategory.RESOURCE, "PAYMENT_NOT_FOUND", "Payment was not found");

    public static final PaymentErrorCode PAYMENT_TRANSACTION_NOT_FOUND =
            new PaymentErrorCode(
                    ErrorCategory.RESOURCE,
                    "PAYMENT_TRANSACTION_NOT_FOUND",
                    "Payment transaction was not found");

    public static final PaymentErrorCode PAYMENT_TRANSACTION_LEASE_NOT_OWNED =
            new PaymentErrorCode(
                    ErrorCategory.BUSINESS,
                    "PAYMENT_TRANSACTION_LEASE_NOT_OWNED",
                    "Payment transaction processing lease is not owned or has expired");

    public static final PaymentErrorCode PAYMENT_TRANSACTION_TYPE_UNSUPPORTED =
            new PaymentErrorCode(
                    ErrorCategory.BUSINESS,
                    "PAYMENT_TRANSACTION_TYPE_UNSUPPORTED",
                    "Payment transaction type is not supported by this operation");

    public static final PaymentErrorCode PAYMENT_NOT_PROCESSABLE =
            new PaymentErrorCode(
                    ErrorCategory.BUSINESS,
                    "PAYMENT_NOT_PROCESSABLE",
                    "Payment cannot enter processing state");

    public static final PaymentErrorCode PAYMENT_HOLD_EXPIRED =
            new PaymentErrorCode(
                    ErrorCategory.BUSINESS,
                    "PAYMENT_HOLD_EXPIRED",
                    "Payment hold expired before provider execution");

    public static final PaymentErrorCode PAYMENT_TRANSACTION_DATA_MISMATCH =
            new PaymentErrorCode(
                    ErrorCategory.SYSTEM,
                    "PAYMENT_TRANSACTION_DATA_MISMATCH",
                    "Payment transaction data does not match its payment");

    public static final PaymentErrorCode PAYMENT_PROVIDER_RESULT_NOT_APPLICABLE =
            new PaymentErrorCode(
                    ErrorCategory.BUSINESS,
                    "PAYMENT_PROVIDER_RESULT_NOT_APPLICABLE",
                    "Provider result cannot be applied to the current payment state");

    public static final PaymentErrorCode PROVIDER_WEBHOOK_HEADERS_REQUIRED =
            validation(
                    "PAYMENT_PROVIDER_WEBHOOK_HEADERS_REQUIRED",
                    "Provider webhook headers are required");

    public static final PaymentErrorCode PROVIDER_WEBHOOK_HEADERS_INVALID =
            validation(
                    "PAYMENT_PROVIDER_WEBHOOK_HEADERS_INVALID",
                    "Provider webhook headers are invalid");

    public static final PaymentErrorCode PROVIDER_WEBHOOK_BODY_REQUIRED =
            validation(
                    "PAYMENT_PROVIDER_WEBHOOK_BODY_REQUIRED",
                    "Provider webhook raw body is required");

    public static final PaymentErrorCode PROVIDER_WEBHOOK_RECEIVED_AT_REQUIRED =
            validation(
                    "PAYMENT_PROVIDER_WEBHOOK_RECEIVED_AT_REQUIRED",
                    "Provider webhook received time is required");

    public static final PaymentErrorCode PROVIDER_WEBHOOK_EVENT_ID_REQUIRED =
            validation(
                    "PAYMENT_PROVIDER_WEBHOOK_EVENT_ID_REQUIRED",
                    "Provider webhook event ID is required");

    public static final PaymentErrorCode PROVIDER_WEBHOOK_EVENT_ID_INVALID =
            validation(
                    "PAYMENT_PROVIDER_WEBHOOK_EVENT_ID_INVALID",
                    "Provider webhook event ID must not exceed 255 characters");

    public static final PaymentErrorCode PROVIDER_WEBHOOK_OCCURRED_AT_REQUIRED =
            validation(
                    "PAYMENT_PROVIDER_WEBHOOK_OCCURRED_AT_REQUIRED",
                    "Provider webhook occurrence time is required");

    public static final PaymentErrorCode PROVIDER_WEBHOOK_RESULT_INVALID =
            validation(
                    "PAYMENT_PROVIDER_WEBHOOK_RESULT_INVALID",
                    "Verified provider webhook result is invalid");

    public static final PaymentErrorCode PROVIDER_WEBHOOK_NOT_SUPPORTED =
            validation(
                    "PAYMENT_PROVIDER_WEBHOOK_NOT_SUPPORTED",
                    "Payment provider webhook is not supported");

    public static final PaymentErrorCode PROVIDER_WEBHOOK_BODY_TOO_LARGE =
            validation(
                    "PAYMENT_PROVIDER_WEBHOOK_BODY_TOO_LARGE",
                    "Payment provider webhook body exceeds the configured limit");

    public static final PaymentErrorCode PROVIDER_WEBHOOK_CONFIGURATION_INVALID =
            system(
                    "PAYMENT_PROVIDER_WEBHOOK_CONFIGURATION_INVALID",
                    "Payment provider webhook configuration is invalid");

    public static final PaymentErrorCode PROVIDER_WEBHOOK_VERIFIER_RESULT_INVALID =
            system(
                    "PAYMENT_PROVIDER_WEBHOOK_VERIFIER_RESULT_INVALID",
                    "Payment provider webhook verifier returned an invalid result");

    public static final PaymentErrorCode PROVIDER_WEBHOOK_REQUIRED =
            validation(
                    "PAYMENT_PROVIDER_WEBHOOK_REQUIRED",
                    "Verified payment provider webhook is required");

    public static final PaymentErrorCode PROVIDER_WEBHOOK_ACKNOWLEDGEMENT_INVALID =
            system(
                    "PAYMENT_PROVIDER_WEBHOOK_ACKNOWLEDGEMENT_INVALID",
                    "Payment provider webhook acknowledgement is invalid");

    public static final PaymentErrorCode PROVIDER_WEBHOOK_AUTHENTICATION_FAILED =
            new PaymentErrorCode(
                    ErrorCategory.SECURITY,
                    "PAYMENT_PROVIDER_WEBHOOK_AUTHENTICATION_FAILED",
                    "Payment provider webhook authentication failed");

    public static final PaymentErrorCode PAYMENT_REQUIRED =
            validation("PAYMENT_REQUIRED", "Payment is required");

    public static final PaymentErrorCode PAYMENT_RESULT_OUTBOX_NOT_APPLICABLE =
            new PaymentErrorCode(
                    ErrorCategory.BUSINESS,
                    "PAYMENT_RESULT_OUTBOX_NOT_APPLICABLE",
                    "Payment result Outbox event is not applicable to the current payment state");

    public static final PaymentErrorCode OUTBOX_PAYLOAD_SERIALIZATION_FAILED =
            system(
                    "PAYMENT_OUTBOX_PAYLOAD_SERIALIZATION_FAILED",
                    "Payment Outbox payload serialization failed");

    private static PaymentErrorCode validation(String code, String message) {

        return new PaymentErrorCode(ErrorCategory.VALIDATION, code, message);
    }

    private static PaymentErrorCode system(String code, String message) {
        return new PaymentErrorCode(ErrorCategory.SYSTEM, code, message);
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
