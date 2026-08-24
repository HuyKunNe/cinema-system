package com.cinema.booking.exception;

import com.cinema.common.exception.code.ErrorCategory;
import com.cinema.common.exception.code.ErrorCode;

public final class BookingErrorCode implements ErrorCode {

    public static final BookingErrorCode BOOKING_NOT_FOUND =
            new BookingErrorCode(ErrorCategory.RESOURCE, "BOOKING_NOT_FOUND", "Booking not found");

    public static final BookingErrorCode USER_ID_REQUIRED =
            new BookingErrorCode(
                    ErrorCategory.VALIDATION, "BOOKING_USER_ID_REQUIRED", "User ID is required");

    public static final BookingErrorCode SHOWTIME_ID_REQUIRED =
            new BookingErrorCode(
                    ErrorCategory.VALIDATION,
                    "BOOKING_SHOWTIME_ID_REQUIRED",
                    "Showtime ID is required");

    public static final BookingErrorCode CLIENT_REQUEST_ID_REQUIRED =
            new BookingErrorCode(
                    ErrorCategory.VALIDATION,
                    "BOOKING_CLIENT_REQUEST_ID_REQUIRED",
                    "Client request ID is required");

    public static final BookingErrorCode BOOKING_ID_REQUIRED =
            new BookingErrorCode(
                    ErrorCategory.VALIDATION, "BOOKING_ID_REQUIRED", "Booking ID is required");

    public static final BookingErrorCode SEAT_NUMBERS_REQUIRED =
            new BookingErrorCode(
                    ErrorCategory.VALIDATION,
                    "BOOKING_SEAT_NUMBERS_REQUIRED",
                    "At least one seat number is required");

    public static final BookingErrorCode SEAT_NUMBER_REQUIRED =
            new BookingErrorCode(
                    ErrorCategory.VALIDATION,
                    "BOOKING_SEAT_NUMBER_REQUIRED",
                    "Seat number is required");

    public static final BookingErrorCode TOO_MANY_SEATS =
            new BookingErrorCode(
                    ErrorCategory.VALIDATION,
                    "BOOKING_TOO_MANY_SEATS",
                    "Requested seat count exceeds the configured limit");

    public static final BookingErrorCode DUPLICATE_SEAT_NUMBER =
            new BookingErrorCode(
                    ErrorCategory.VALIDATION,
                    "BOOKING_DUPLICATE_SEAT_NUMBER",
                    "Duplicate seat numbers are not allowed");

    public static final BookingErrorCode EXPIRATION_REQUIRED =
            new BookingErrorCode(
                    ErrorCategory.VALIDATION,
                    "BOOKING_EXPIRATION_REQUIRED",
                    "Booking expiration and current time are required");

    public static final BookingErrorCode INVALID_EXPIRATION =
            new BookingErrorCode(
                    ErrorCategory.VALIDATION,
                    "BOOKING_INVALID_EXPIRATION",
                    "Booking expiration must be in the future");

    public static final BookingErrorCode INVENTORY_SEAT_ID_REQUIRED =
            new BookingErrorCode(
                    ErrorCategory.VALIDATION,
                    "BOOKING_INVENTORY_SEAT_ID_REQUIRED",
                    "Inventory seat ID is required");

    public static final BookingErrorCode SEAT_TYPE_REQUIRED =
            new BookingErrorCode(
                    ErrorCategory.VALIDATION,
                    "BOOKING_SEAT_TYPE_REQUIRED",
                    "Seat type is required");

    public static final BookingErrorCode INVALID_SEAT_PRICE =
            new BookingErrorCode(
                    ErrorCategory.VALIDATION,
                    "BOOKING_INVALID_SEAT_PRICE",
                    "Seat price must be zero or greater");

    public static final BookingErrorCode SEAT_SNAPSHOT_ALREADY_COMPLETED =
            new BookingErrorCode(
                    ErrorCategory.BUSINESS,
                    "BOOKING_SEAT_SNAPSHOT_ALREADY_COMPLETED",
                    "Booking seat snapshot has already been completed");

    public static final BookingErrorCode REQUEST_FINGERPRINT_REQUIRED =
            new BookingErrorCode(
                    ErrorCategory.VALIDATION,
                    "BOOKING_REQUEST_FINGERPRINT_REQUIRED",
                    "Booking request fingerprint is required");

    public static final BookingErrorCode CLIENT_REQUEST_ID_PAYLOAD_MISMATCH =
            new BookingErrorCode(
                    ErrorCategory.BUSINESS,
                    "BOOKING_CLIENT_REQUEST_ID_PAYLOAD_MISMATCH",
                    "Client request ID has already been used with a different request");

    public static final BookingErrorCode REQUEST_FINGERPRINT_GENERATION_FAILED =
            new BookingErrorCode(
                    ErrorCategory.SYSTEM,
                    "BOOKING_REQUEST_FINGERPRINT_GENERATION_FAILED",
                    "Booking request fingerprint could not be generated");

    public static final BookingErrorCode OUTBOX_PAYLOAD_SERIALIZATION_FAILED =
            new BookingErrorCode(
                    ErrorCategory.SYSTEM,
                    "BOOKING_OUTBOX_PAYLOAD_SERIALIZATION_FAILED",
                    "Booking event payload could not be serialized");

    public static final BookingErrorCode PROCESSED_EVENT_ID_REQUIRED =
            new BookingErrorCode(
                    ErrorCategory.VALIDATION,
                    "BOOKING_PROCESSED_EVENT_ID_REQUIRED",
                    "Processed event ID is required");

    public static final BookingErrorCode EVENT_ID_REQUIRED =
            new BookingErrorCode(
                    ErrorCategory.VALIDATION, "BOOKING_EVENT_ID_REQUIRED", "Event ID is required");

    public static final BookingErrorCode CONSUMER_NAME_REQUIRED =
            new BookingErrorCode(
                    ErrorCategory.VALIDATION,
                    "BOOKING_CONSUMER_NAME_REQUIRED",
                    "Consumer name is required");

    public static final BookingErrorCode EVENT_TYPE_REQUIRED =
            new BookingErrorCode(
                    ErrorCategory.VALIDATION,
                    "BOOKING_EVENT_TYPE_REQUIRED",
                    "Event type is required");

    public static final BookingErrorCode EVENT_VERSION_REQUIRED =
            new BookingErrorCode(
                    ErrorCategory.VALIDATION,
                    "BOOKING_EVENT_VERSION_REQUIRED",
                    "Event version is required");

    public static final BookingErrorCode PROCESSED_AT_REQUIRED =
            new BookingErrorCode(
                    ErrorCategory.VALIDATION,
                    "BOOKING_PROCESSED_AT_REQUIRED",
                    "Processed time is required");

    public static final BookingErrorCode BOOKING_NOT_PENDING =
            new BookingErrorCode(
                    ErrorCategory.BUSINESS, "BOOKING_NOT_PENDING", "Booking is not pending");

    public static final BookingErrorCode TOTAL_AMOUNT_REQUIRED =
            new BookingErrorCode(
                    ErrorCategory.VALIDATION,
                    "BOOKING_TOTAL_AMOUNT_REQUIRED",
                    "Booking total amount is required");

    public static final BookingErrorCode INVALID_TOTAL_AMOUNT =
            new BookingErrorCode(
                    ErrorCategory.VALIDATION,
                    "BOOKING_INVALID_TOTAL_AMOUNT",
                    "Booking total amount must be zero or greater");

    public static final BookingErrorCode CURRENCY_REQUIRED =
            new BookingErrorCode(
                    ErrorCategory.VALIDATION,
                    "BOOKING_CURRENCY_REQUIRED",
                    "Booking currency is required");

    public static final BookingErrorCode INVALID_CURRENCY =
            new BookingErrorCode(
                    ErrorCategory.VALIDATION,
                    "BOOKING_INVALID_CURRENCY",
                    "Booking currency must contain exactly three letters");

    public static final BookingErrorCode REJECTION_REASON_REQUIRED =
            new BookingErrorCode(
                    ErrorCategory.VALIDATION,
                    "BOOKING_REJECTION_REASON_REQUIRED",
                    "Booking rejection reason is required");

    public static final BookingErrorCode EVENT_MESSAGE_INVALID =
            new BookingErrorCode(
                    ErrorCategory.VALIDATION,
                    "BOOKING_EVENT_MESSAGE_INVALID",
                    "Event message is invalid");

    public static final BookingErrorCode EVENT_ID_INVALID =
            new BookingErrorCode(
                    ErrorCategory.VALIDATION,
                    "BOOKING_EVENT_ID_INVALID",
                    "Event ID must be a UUID v7");

    public static final BookingErrorCode EVENT_TYPE_INVALID =
            new BookingErrorCode(
                    ErrorCategory.VALIDATION,
                    "BOOKING_EVENT_TYPE_INVALID",
                    "Event type is not supported");

    public static final BookingErrorCode EVENT_VERSION_INVALID =
            new BookingErrorCode(
                    ErrorCategory.VALIDATION,
                    "BOOKING_EVENT_VERSION_INVALID",
                    "Event version is not supported");

    public static final BookingErrorCode EVENT_PRODUCER_INVALID =
            new BookingErrorCode(
                    ErrorCategory.VALIDATION,
                    "BOOKING_EVENT_PRODUCER_INVALID",
                    "Event producer is invalid");

    public static final BookingErrorCode EVENT_AGGREGATE_INVALID =
            new BookingErrorCode(
                    ErrorCategory.VALIDATION,
                    "BOOKING_EVENT_AGGREGATE_INVALID",
                    "Event aggregate is invalid");

    public static final BookingErrorCode EVENT_PARTITION_KEY_INVALID =
            new BookingErrorCode(
                    ErrorCategory.VALIDATION,
                    "BOOKING_EVENT_PARTITION_KEY_INVALID",
                    "Event partition key must match the booking ID");

    public static final BookingErrorCode EVENT_CORRELATION_ID_INVALID =
            new BookingErrorCode(
                    ErrorCategory.VALIDATION,
                    "BOOKING_EVENT_CORRELATION_ID_INVALID",
                    "Event correlation ID must be a UUID v7");

    public static final BookingErrorCode EVENT_OCCURRED_AT_REQUIRED =
            new BookingErrorCode(
                    ErrorCategory.VALIDATION,
                    "BOOKING_EVENT_OCCURRED_AT_REQUIRED",
                    "Event occurrence time is required");

    public static final BookingErrorCode EVENT_PAYLOAD_INVALID =
            new BookingErrorCode(
                    ErrorCategory.VALIDATION,
                    "BOOKING_EVENT_PAYLOAD_INVALID",
                    "Event payload is invalid");

    public static final BookingErrorCode RESERVATION_RESULT_MISMATCH =
            new BookingErrorCode(
                    ErrorCategory.BUSINESS,
                    "BOOKING_RESERVATION_RESULT_MISMATCH",
                    "Reservation result does not match the pending booking request");

    private final ErrorCategory category;

    private final String code;

    private final String message;

    private BookingErrorCode(ErrorCategory category, String code, String message) {

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
}
