package com.cinema.inventory.exception;

import com.cinema.common.exception.code.ErrorCategory;
import com.cinema.common.exception.code.ErrorCode;

public final class InventoryErrorCode implements ErrorCode {

    public static final InventoryErrorCode CINEMA_NOT_FOUND =
            new InventoryErrorCode(
                    ErrorCategory.RESOURCE, "INVENTORY_CINEMA_NOT_FOUND", "Cinema not found");

    public static final InventoryErrorCode ROOM_NOT_FOUND =
            new InventoryErrorCode(
                    ErrorCategory.RESOURCE, "INVENTORY_ROOM_NOT_FOUND", "Room not found");

    public static final InventoryErrorCode SEAT_NOT_FOUND =
            new InventoryErrorCode(
                    ErrorCategory.RESOURCE, "INVENTORY_SEAT_NOT_FOUND", "Seat not found");

    public static final InventoryErrorCode SHOWTIME_NOT_FOUND =
            new InventoryErrorCode(
                    ErrorCategory.RESOURCE, "INVENTORY_SHOWTIME_NOT_FOUND", "Showtime not found");

    public static final InventoryErrorCode SHOWTIME_NOT_PERSISTED =
            new InventoryErrorCode(
                    ErrorCategory.VALIDATION,
                    "INVENTORY_SHOWTIME_NOT_PERSISTED",
                    "Showtime must be persisted before generating show seats");

    public static final InventoryErrorCode SHOWTIME_REQUIRED =
            new InventoryErrorCode(
                    ErrorCategory.VALIDATION,
                    "INVENTORY_SHOWTIME_REQUIRED",
                    "Showtime is required");

    public static final InventoryErrorCode SHOW_SEAT_NOT_FOUND =
            new InventoryErrorCode(
                    ErrorCategory.RESOURCE, "INVENTORY_SHOW_SEAT_NOT_FOUND", "Show seat not found");

    public static final InventoryErrorCode ROOM_NAME_ALREADY_EXISTS =
            new InventoryErrorCode(
                    ErrorCategory.BUSINESS,
                    "INVENTORY_ROOM_NAME_ALREADY_EXISTS",
                    "Room name already exists in this cinema");

    public static final InventoryErrorCode SEAT_NUMBER_ALREADY_EXISTS =
            new InventoryErrorCode(
                    ErrorCategory.BUSINESS,
                    "INVENTORY_SEAT_NUMBER_ALREADY_EXISTS",
                    "Seat number already exists in this room");

    public static final InventoryErrorCode SHOWTIME_OVERLAP =
            new InventoryErrorCode(
                    ErrorCategory.BUSINESS,
                    "INVENTORY_SHOWTIME_OVERLAP",
                    "Showtime overlaps with another showtime in this room");

    public static final InventoryErrorCode SHOW_SEATS_ALREADY_GENERATED =
            new InventoryErrorCode(
                    ErrorCategory.BUSINESS,
                    "INVENTORY_SHOW_SEATS_ALREADY_GENERATED",
                    "Show seats have already been generated");

    public static final InventoryErrorCode CINEMA_INACTIVE =
            new InventoryErrorCode(
                    ErrorCategory.BUSINESS, "INVENTORY_CINEMA_INACTIVE", "Cinema is inactive");

    public static final InventoryErrorCode ROOM_INACTIVE =
            new InventoryErrorCode(
                    ErrorCategory.BUSINESS, "INVENTORY_ROOM_INACTIVE", "Room is inactive");

    public static final InventoryErrorCode SHOWTIME_NOT_EDITABLE =
            new InventoryErrorCode(
                    ErrorCategory.BUSINESS,
                    "INVENTORY_SHOWTIME_NOT_EDITABLE",
                    "Showtime cannot be edited in its current status");

    public static final InventoryErrorCode NO_ACTIVE_SEATS =
            new InventoryErrorCode(
                    ErrorCategory.BUSINESS,
                    "INVENTORY_NO_ACTIVE_SEATS",
                    "Room has no active seats");

    public static final InventoryErrorCode INVALID_SHOWTIME_PERIOD =
            new InventoryErrorCode(
                    ErrorCategory.VALIDATION,
                    "INVENTORY_INVALID_SHOWTIME_PERIOD",
                    "Showtime end time must be after start time");

    public static final InventoryErrorCode INVALID_INVENTORY_STATE =
            new InventoryErrorCode(
                    ErrorCategory.BUSINESS,
                    "INVENTORY_INVALID_STATE",
                    "Inventory resource is in an invalid state");

    public static final InventoryErrorCode INV_SEAT_TYPE_REQUIRED =
            new InventoryErrorCode(
                    ErrorCategory.VALIDATION,
                    "INVENTORY_SEAT_TYPE_REQUIRED",
                    "Seat type is required");

    public static final InventoryErrorCode INV_BASE_PRICE_REQUIRED =
            new InventoryErrorCode(
                    ErrorCategory.VALIDATION,
                    "INVENTORY_BASE_PRICE_REQUIRED",
                    "Base price is required");

    public static final InventoryErrorCode INV_INVALID_BASE_PRICE =
            new InventoryErrorCode(
                    ErrorCategory.VALIDATION,
                    "INVENTORY_INVALID_BASE_PRICE",
                    "Base price must be greater than zero");

    public static final InventoryErrorCode SHOWTIME_ROOM_REQUIRED =
            new InventoryErrorCode(
                    ErrorCategory.VALIDATION,
                    "INVENTORY_SHOWTIME_ROOM_REQUIRED",
                    "Showtime room is required");

    public static final InventoryErrorCode SHOW_SEAT_NOT_AVAILABLE =
            new InventoryErrorCode(
                    ErrorCategory.BUSINESS,
                    "INVENTORY_SHOW_SEAT_NOT_AVAILABLE",
                    "Show seat is not available");

    public static final InventoryErrorCode SHOW_SEAT_NOT_HELD =
            new InventoryErrorCode(
                    ErrorCategory.BUSINESS,
                    "INVENTORY_SHOW_SEAT_NOT_HELD",
                    "Show seat is not held");

    public static final InventoryErrorCode SHOW_SEAT_HELD_BY_ANOTHER_BOOKING =
            new InventoryErrorCode(
                    ErrorCategory.BUSINESS,
                    "INVENTORY_SHOW_SEAT_HELD_BY_ANOTHER_BOOKING",
                    "Show seat is held by another booking");

    public static final InventoryErrorCode SHOW_SEAT_HOLD_EXPIRED =
            new InventoryErrorCode(
                    ErrorCategory.BUSINESS,
                    "INVENTORY_SHOW_SEAT_HOLD_EXPIRED",
                    "Show seat hold has expired");

    public static final InventoryErrorCode INVALID_HOLD_EXPIRATION =
            new InventoryErrorCode(
                    ErrorCategory.VALIDATION,
                    "INVENTORY_INVALID_HOLD_EXPIRATION",
                    "Hold expiration must be in the future");

    public static final InventoryErrorCode SHOW_SEAT_ALREADY_UNAVAILABLE =
            new InventoryErrorCode(
                    ErrorCategory.BUSINESS,
                    "INVENTORY_SHOW_SEAT_ALREADY_UNAVAILABLE",
                    "Show seat is already unavailable");

    public static final InventoryErrorCode SHOW_SEAT_CANNOT_BECOME_AVAILABLE =
            new InventoryErrorCode(
                    ErrorCategory.BUSINESS,
                    "INVENTORY_SHOW_SEAT_CANNOT_BECOME_AVAILABLE",
                    "Only an unavailable show seat can become available");

    public static final InventoryErrorCode BOOKED_SHOW_SEAT_CANNOT_BE_CHANGED =
            new InventoryErrorCode(
                    ErrorCategory.BUSINESS,
                    "INVENTORY_BOOKED_SHOW_SEAT_CANNOT_BE_CHANGED",
                    "Booked show seat cannot be changed");

    public static final InventoryErrorCode BOOKING_ID_REQUIRED =
            new InventoryErrorCode(
                    ErrorCategory.VALIDATION,
                    "INVENTORY_BOOKING_ID_REQUIRED",
                    "Booking ID is required");

    public static final InventoryErrorCode HOLD_EXPIRATION_AND_CURRENT_TIME_REQUIRED =
            new InventoryErrorCode(
                    ErrorCategory.VALIDATION,
                    "INVENTORY_HOLD_EXPIRATION_AND_CURRENT_TIME_REQUIRED",
                    "Hold expiration and current time are required");

    public static final InventoryErrorCode ONLY_AVAILABLE_SEATS_CAN_BE_HELD =
            new InventoryErrorCode(
                    ErrorCategory.BUSINESS,
                    "INVENTORY_ONLY_AVAILABLE_SEATS_CAN_BE_HELD",
                    "Only an available seat can be held");

    public static final InventoryErrorCode ONLY_HELD_SEATS_CAN_BE_RELEASED =
            new InventoryErrorCode(
                    ErrorCategory.BUSINESS,
                    "INVENTORY_ONLY_HELD_SEATS_CAN_BE_RELEASED",
                    "Only a held seat can be released");

    public static final InventoryErrorCode CURRENT_TIME_REQUIRED =
            new InventoryErrorCode(
                    ErrorCategory.VALIDATION,
                    "INVENTORY_CURRENT_TIME_REQUIRED",
                    "Current time is required");

    public static final InventoryErrorCode SEAT_HOLD_NOT_EXPIRED =
            new InventoryErrorCode(
                    ErrorCategory.BUSINESS,
                    "INVENTORY_SEAT_HOLD_NOT_EXPIRED",
                    "Seat hold has not expired");

    public static final InventoryErrorCode SEAT_NOT_HELD_BY_BOOKING =
            new InventoryErrorCode(
                    ErrorCategory.BUSINESS,
                    "INVENTORY_SEAT_NOT_HELD_BY_BOOKING",
                    "Seat is not held by booking");

    public static final InventoryErrorCode SHOWTIME_PERIOD_REQUIRED =
            new InventoryErrorCode(
                    ErrorCategory.VALIDATION,
                    "INVENTORY_SHOWTIME_PERIOD_REQUIRED",
                    "Showtime start and end are required");

    public static final InventoryErrorCode SHOW_SEAT_PRICE_INVALID =
            new InventoryErrorCode(
                    ErrorCategory.VALIDATION,
                    "INVENTORY_SHOW_SEAT_PRICE_INVALID",
                    "Show seat price must be zero or greater");

    public static final InventoryErrorCode EVENT_ID_REQUIRED =
            new InventoryErrorCode(
                    ErrorCategory.VALIDATION,
                    "INVENTORY_EVENT_ID_REQUIRED",
                    "Event ID is required");

    public static final InventoryErrorCode EVENT_ID_INVALID =
            new InventoryErrorCode(
                    ErrorCategory.VALIDATION,
                    "INVENTORY_EVENT_ID_INVALID",
                    "Event ID must be a UUID v7");

    public static final InventoryErrorCode EVENT_TYPE_INVALID =
            new InventoryErrorCode(
                    ErrorCategory.VALIDATION,
                    "INVENTORY_EVENT_TYPE_INVALID",
                    "Event type is not supported");

    public static final InventoryErrorCode EVENT_VERSION_INVALID =
            new InventoryErrorCode(
                    ErrorCategory.VALIDATION,
                    "INVENTORY_EVENT_VERSION_INVALID",
                    "Event version is not supported");

    public static final InventoryErrorCode EVENT_PRODUCER_INVALID =
            new InventoryErrorCode(
                    ErrorCategory.VALIDATION,
                    "INVENTORY_EVENT_PRODUCER_INVALID",
                    "Event producer is invalid");

    public static final InventoryErrorCode EVENT_AGGREGATE_INVALID =
            new InventoryErrorCode(
                    ErrorCategory.VALIDATION,
                    "INVENTORY_EVENT_AGGREGATE_INVALID",
                    "Event aggregate is invalid");

    public static final InventoryErrorCode EVENT_PARTITION_KEY_INVALID =
            new InventoryErrorCode(
                    ErrorCategory.VALIDATION,
                    "INVENTORY_EVENT_PARTITION_KEY_INVALID",
                    "Event partition key must match the booking ID");

    public static final InventoryErrorCode EVENT_CORRELATION_ID_INVALID =
            new InventoryErrorCode(
                    ErrorCategory.VALIDATION,
                    "INVENTORY_EVENT_CORRELATION_ID_INVALID",
                    "Event correlation ID must be a UUID v7");

    public static final InventoryErrorCode EVENT_OCCURRED_AT_REQUIRED =
            new InventoryErrorCode(
                    ErrorCategory.VALIDATION,
                    "INVENTORY_EVENT_OCCURRED_AT_REQUIRED",
                    "Event occurrence time is required");

    public static final InventoryErrorCode EVENT_PAYLOAD_INVALID =
            new InventoryErrorCode(
                    ErrorCategory.VALIDATION,
                    "INVENTORY_EVENT_PAYLOAD_INVALID",
                    "Event payload is invalid");

    public static final InventoryErrorCode OUTBOX_PAYLOAD_SERIALIZATION_FAILED =
            new InventoryErrorCode(
                    ErrorCategory.SYSTEM,
                    "INVENTORY_OUTBOX_PAYLOAD_SERIALIZATION_FAILED",
                    "Inventory event payload could not be serialized");

    private final String code;
    private final String message;
    private final ErrorCategory category;

    private InventoryErrorCode(ErrorCategory category, String code, String message) {
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
