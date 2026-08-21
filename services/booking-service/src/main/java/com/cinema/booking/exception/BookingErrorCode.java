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
