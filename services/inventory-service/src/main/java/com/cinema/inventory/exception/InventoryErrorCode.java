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

    public static final InventoryErrorCode EVENT_MESSAGE_INVALID =
            new InventoryErrorCode(
                    ErrorCategory.VALIDATION,
                    "INVENTORY_EVENT_MESSAGE_INVALID",
                    "Kafka event message is invalid");

    public static final InventoryErrorCode PROCESSED_EVENT_ID_REQUIRED =
            new InventoryErrorCode(
                    ErrorCategory.VALIDATION,
                    "INVENTORY_PROCESSED_EVENT_ID_REQUIRED",
                    "Processed event ID is required");

    public static final InventoryErrorCode CONSUMER_NAME_REQUIRED =
            new InventoryErrorCode(
                    ErrorCategory.VALIDATION,
                    "INVENTORY_CONSUMER_NAME_REQUIRED",
                    "Consumer name is required");

    public static final InventoryErrorCode PROCESSED_EVENT_TYPE_REQUIRED =
            new InventoryErrorCode(
                    ErrorCategory.VALIDATION,
                    "INVENTORY_PROCESSED_EVENT_TYPE_REQUIRED",
                    "Processed event type is required");

    public static final InventoryErrorCode PROCESSED_EVENT_VERSION_REQUIRED =
            new InventoryErrorCode(
                    ErrorCategory.VALIDATION,
                    "INVENTORY_PROCESSED_EVENT_VERSION_REQUIRED",
                    "Processed event version is required");

    public static final InventoryErrorCode PROCESSED_AT_REQUIRED =
            new InventoryErrorCode(
                    ErrorCategory.VALIDATION,
                    "INVENTORY_PROCESSED_AT_REQUIRED",
                    "Processed event timestamp is required");

    public static final InventoryErrorCode EVENT_CAUSATION_ID_INVALID =
            new InventoryErrorCode(
                    ErrorCategory.VALIDATION,
                    "INVENTORY_EVENT_CAUSATION_ID_INVALID",
                    "Event causation ID must be a UUID v7");

    public static final InventoryErrorCode BOOKING_CONFIRMATION_MISMATCH =
            new InventoryErrorCode(
                    ErrorCategory.BUSINESS,
                    "INVENTORY_BOOKING_CONFIRMATION_MISMATCH",
                    "Booking confirmation does not match held inventory");

    public static final InventoryErrorCode SEAT_RELEASE_MISMATCH =
            new InventoryErrorCode(
                    ErrorCategory.BUSINESS,
                    "INVENTORY_SEAT_RELEASE_MISMATCH",
                    "Seat release request does not match held inventory");

    public static final InventoryErrorCode SHOWTIME_QUERY_RANGE_TOO_LARGE =
            new InventoryErrorCode(
                    ErrorCategory.VALIDATION,
                    "INVENTORY_SHOWTIME_QUERY_RANGE_TOO_LARGE",
                    "Showtime query range exceeds the configured maximum");

    public static final InventoryErrorCode SHOWTIME_NOT_BOOKABLE =
            new InventoryErrorCode(
                    ErrorCategory.BUSINESS,
                    "INVENTORY_SHOWTIME_NOT_BOOKABLE",
                    "Showtime must be open for booking, start in the future, "
                            + "and belong to an active room and cinema");

    public static final InventoryErrorCode ROOM_LAYOUT_DATA_INVALID =
            new InventoryErrorCode(
                    ErrorCategory.VALIDATION,
                    "INVENTORY_ROOM_LAYOUT_DATA_INVALID",
                    "Room layout data is invalid");

    public static final InventoryErrorCode ROOM_LAYOUT_GEOMETRY_INVALID =
            new InventoryErrorCode(
                    ErrorCategory.VALIDATION,
                    "INVENTORY_ROOM_LAYOUT_GEOMETRY_INVALID",
                    "Room layout geometry is invalid");

    public static final InventoryErrorCode ROOM_LAYOUT_SEAT_ROOM_MISMATCH =
            new InventoryErrorCode(
                    ErrorCategory.VALIDATION,
                    "INVENTORY_ROOM_LAYOUT_SEAT_ROOM_MISMATCH",
                    "Seat must belong to the room of the layout");

    public static final InventoryErrorCode ROOM_LAYOUT_NOT_DRAFT =
            new InventoryErrorCode(
                    ErrorCategory.BUSINESS,
                    "INVENTORY_ROOM_LAYOUT_NOT_DRAFT",
                    "Only draft room layouts can be modified");

    public static final InventoryErrorCode ROOM_LAYOUT_NOT_FOUND =
            new InventoryErrorCode(
                    ErrorCategory.RESOURCE,
                    "INVENTORY_ROOM_LAYOUT_NOT_FOUND",
                    "Room layout not found");

    public static final InventoryErrorCode ROOM_LAYOUT_VERSION_EXHAUSTED =
            new InventoryErrorCode(
                    ErrorCategory.BUSINESS,
                    "INVENTORY_ROOM_LAYOUT_VERSION_EXHAUSTED",
                    "Room layout version limit has been reached");

    public static final InventoryErrorCode ROOM_LAYOUT_VERSION_CONFLICT =
            new InventoryErrorCode(
                    ErrorCategory.BUSINESS,
                    "INVENTORY_ROOM_LAYOUT_VERSION_CONFLICT",
                    "Room layout has changed; reload before saving");

    public static final InventoryErrorCode ROOM_LAYOUT_DUPLICATE_SEAT =
            new InventoryErrorCode(
                    ErrorCategory.VALIDATION,
                    "INVENTORY_ROOM_LAYOUT_DUPLICATE_SEAT",
                    "A seat can appear only once in a room layout");

    public static final InventoryErrorCode ROOM_LAYOUT_INCOMPLETE =
            new InventoryErrorCode(
                    ErrorCategory.VALIDATION,
                    "INVENTORY_ROOM_LAYOUT_INCOMPLETE",
                    "Publishing requires at least one seat and one screen");

    public static final InventoryErrorCode ROOM_LAYOUT_OUTSIDE_CANVAS =
            new InventoryErrorCode(
                    ErrorCategory.VALIDATION,
                    "INVENTORY_ROOM_LAYOUT_OUTSIDE_CANVAS",
                    "Rotated room layout geometry must stay inside the canvas");

    public static final InventoryErrorCode ROOM_LAYOUT_GEOMETRY_OVERLAP =
            new InventoryErrorCode(
                    ErrorCategory.VALIDATION,
                    "INVENTORY_ROOM_LAYOUT_GEOMETRY_OVERLAP",
                    "Seat geometry overlaps another seat or a layout element");

    public static final InventoryErrorCode SHOWTIME_LAYOUT_ROOM_MISMATCH =
            new InventoryErrorCode(
                    ErrorCategory.VALIDATION,
                    "INVENTORY_SHOWTIME_LAYOUT_ROOM_MISMATCH",
                    "Showtime layout must belong to the selected room");

    public static final InventoryErrorCode ROOM_LAYOUT_NOT_PUBLISHED =
            new InventoryErrorCode(
                    ErrorCategory.BUSINESS,
                    "INVENTORY_ROOM_LAYOUT_NOT_PUBLISHED",
                    "Showtime requires a published room layout");

    public static final InventoryErrorCode ROOM_LAYOUT_SEAT_SNAPSHOT_MISMATCH =
            new InventoryErrorCode(
                    ErrorCategory.BUSINESS,
                    "INVENTORY_ROOM_LAYOUT_SEAT_SNAPSHOT_MISMATCH",
                    "Physical seats no longer match the published layout snapshot");

    public static final InventoryErrorCode SHOWTIME_LAYOUT_SEATS_MISMATCH =
            new InventoryErrorCode(
                    ErrorCategory.BUSINESS,
                    "INVENTORY_SHOWTIME_LAYOUT_SEATS_MISMATCH",
                    "Existing show seats contain seats outside the pinned layout");

    public static final InventoryErrorCode SHOWTIME_LAYOUT_REQUIRED =
            new InventoryErrorCode(
                    ErrorCategory.BUSINESS,
                    "INVENTORY_SHOWTIME_LAYOUT_REQUIRED",
                    "Showtime has no pinned room layout");

    public static final InventoryErrorCode SEAT_MAP_DATA_INCONSISTENT =
            new InventoryErrorCode(
                    ErrorCategory.BUSINESS,
                    "INVENTORY_SEAT_MAP_DATA_INCONSISTENT",
                    "Seat map data is inconsistent with the pinned layout");

    public static final InventoryErrorCode ROOM_LAYOUT_MATRIX_CONVERSION_REQUIRED =
            new InventoryErrorCode(
                    ErrorCategory.BUSINESS,
                    "INVENTORY_ROOM_LAYOUT_MATRIX_CONVERSION_REQUIRED",
                    "Create a new matrix draft instead of replacing a legacy geometry layout");

    public static final InventoryErrorCode ROOM_LAYOUT_MATRIX_PUBLICATION_PENDING =
            new InventoryErrorCode(
                    ErrorCategory.BUSINESS,
                    "INVENTORY_ROOM_LAYOUT_MATRIX_PUBLICATION_PENDING",
                    "Matrix drafts can be saved but publication is not available yet");

    public static final InventoryErrorCode ROOM_LAYOUT_MATRIX_EMPTY =
            new InventoryErrorCode(
                    ErrorCategory.VALIDATION,
                    "INVENTORY_ROOM_LAYOUT_MATRIX_EMPTY",
                    "Add at least one seat before publishing the matrix");

    public static final InventoryErrorCode SEAT_LAYOUT_METADATA_READ_ONLY =
            new InventoryErrorCode(
                    ErrorCategory.BUSINESS,
                    "INVENTORY_SEAT_LAYOUT_METADATA_READ_ONLY",
                    "Edit seat numbers and types in a new layout draft");

    public static final InventoryErrorCode HELD_SHOW_SEAT_CANNOT_BE_CHANGED =
            new InventoryErrorCode(
                    ErrorCategory.BUSINESS,
                    "INVENTORY_HELD_SHOW_SEAT_CANNOT_BE_CHANGED",
                    "Held show seat cannot be changed by availability administration");

    public static final InventoryErrorCode CINEMA_TIME_ZONE_INVALID =
            new InventoryErrorCode(
                    ErrorCategory.VALIDATION,
                    "INVENTORY_CINEMA_TIME_ZONE_INVALID",
                    "Cinema time zone must be a supported named time zone");

    public static final InventoryErrorCode CINEMA_PUBLIC_DETAILS_INVALID =
            new InventoryErrorCode(
                    ErrorCategory.VALIDATION,
                    "INVENTORY_CINEMA_PUBLIC_DETAILS_INVALID",
                    "Cinema public details are invalid");

    public static final InventoryErrorCode ROOM_CONTENT_FORMATS_INVALID =
            new InventoryErrorCode(
                    ErrorCategory.VALIDATION,
                    "INVENTORY_ROOM_CONTENT_FORMATS_INVALID",
                    "Room content formats are invalid");

    public static final InventoryErrorCode ROOM_VERSION_CONFLICT =
            new InventoryErrorCode(
                    ErrorCategory.BUSINESS,
                    "INVENTORY_ROOM_VERSION_CONFLICT",
                    "Room has changed; reload before saving");

    public static final InventoryErrorCode CINEMA_AMENITIES_INVALID =
            new InventoryErrorCode(
                    ErrorCategory.VALIDATION,
                    "INVENTORY_CINEMA_AMENITIES_INVALID",
                    "Cinema amenities are invalid");

    public static final InventoryErrorCode CINEMA_VERSION_CONFLICT =
            new InventoryErrorCode(
                    ErrorCategory.BUSINESS,
                    "INVENTORY_CINEMA_VERSION_CONFLICT",
                    "Cinema has changed; reload before saving");

    public static final InventoryErrorCode SCREENING_VERSION_REQUIRED =
            new InventoryErrorCode(
                    ErrorCategory.VALIDATION,
                    "INVENTORY_SCREENING_VERSION_REQUIRED",
                    "Movie screening version is required");

    public static final InventoryErrorCode SCREENING_VERSION_NOT_FOUND =
            new InventoryErrorCode(
                    ErrorCategory.RESOURCE,
                    "INVENTORY_SCREENING_VERSION_NOT_FOUND",
                    "Screening version was not found for the selected movie");

    public static final InventoryErrorCode SCREENING_VERSION_INACTIVE =
            new InventoryErrorCode(
                    ErrorCategory.BUSINESS,
                    "INVENTORY_SCREENING_VERSION_INACTIVE",
                    "Screening version is inactive");

    public static final InventoryErrorCode SCREENING_VERSION_SNAPSHOT_INVALID =
            new InventoryErrorCode(
                    ErrorCategory.VALIDATION,
                    "INVENTORY_SCREENING_VERSION_SNAPSHOT_INVALID",
                    "Screening version snapshot is invalid");

    public static final InventoryErrorCode SCREENING_VERSION_IMMUTABLE =
            new InventoryErrorCode(
                    ErrorCategory.BUSINESS,
                    "INVENTORY_SCREENING_VERSION_IMMUTABLE",
                    "Movie cannot be changed after a screening version is pinned");

    public static final InventoryErrorCode ROOM_CONTENT_FORMATS_NOT_CONFIGURED =
            new InventoryErrorCode(
                    ErrorCategory.BUSINESS,
                    "INVENTORY_ROOM_CONTENT_FORMATS_NOT_CONFIGURED",
                    "Configure room content formats before creating a showtime");

    public static final InventoryErrorCode SHOWTIME_FORMAT_NOT_SUPPORTED =
            new InventoryErrorCode(
                    ErrorCategory.BUSINESS,
                    "INVENTORY_SHOWTIME_FORMAT_NOT_SUPPORTED",
                    "Room does not support the selected screening version format");

    public static final InventoryErrorCode MOVIE_CATALOG_UNAVAILABLE =
            new InventoryErrorCode(
                    ErrorCategory.SYSTEM,
                    "INVENTORY_MOVIE_CATALOG_UNAVAILABLE",
                    "Movie screening version could not be verified");

    public static final InventoryErrorCode MOVIE_NOT_FOUND =
            new InventoryErrorCode(
                    ErrorCategory.RESOURCE, "INVENTORY_MOVIE_NOT_FOUND", "Movie not found");

    public static final InventoryErrorCode MOVIE_NOT_SCHEDULABLE =
            new InventoryErrorCode(
                    ErrorCategory.BUSINESS,
                    "INVENTORY_MOVIE_NOT_SCHEDULABLE",
                    "Movie is not available for new showtimes");

    public static final InventoryErrorCode INVALID_SHOWTIME_BREAK =
            new InventoryErrorCode(
                    ErrorCategory.VALIDATION,
                    "INVENTORY_INVALID_SHOWTIME_BREAK",
                    "Break minutes must be between 0 and 120");

    public static final InventoryErrorCode SHOWTIME_DURATION_MISMATCH =
            new InventoryErrorCode(
                    ErrorCategory.VALIDATION,
                    "INVENTORY_SHOWTIME_DURATION_MISMATCH",
                    "Showtime duration must match the pinned movie duration");

    public static final InventoryErrorCode SHOWTIME_START_NOT_FUTURE =
            new InventoryErrorCode(
                    ErrorCategory.VALIDATION,
                    "INVENTORY_SHOWTIME_START_NOT_FUTURE",
                    "Showtime start must be in the future");

    public static final InventoryErrorCode SHOWTIME_SCHEDULING_REQUIRED =
            new InventoryErrorCode(
                    ErrorCategory.BUSINESS,
                    "INVENTORY_SHOWTIME_SCHEDULING_REQUIRED",
                    "Showtime has no scheduling snapshot");

    public static final InventoryErrorCode SHOWTIME_VERSION_CONFLICT =
            new InventoryErrorCode(
                    ErrorCategory.BUSINESS,
                    "INVENTORY_SHOWTIME_VERSION_CONFLICT",
                    "Showtime has changed; reload before saving");

    public static final InventoryErrorCode INVALID_SHOWTIME_UPDATE =
            new InventoryErrorCode(
                    ErrorCategory.VALIDATION,
                    "INVENTORY_INVALID_SHOWTIME_UPDATE",
                    "Invalid showtime schedule update");

    public static final InventoryErrorCode INVALID_PRICING_POLICY =
            new InventoryErrorCode(
                    ErrorCategory.VALIDATION,
                    "INVENTORY_INVALID_PRICING_POLICY",
                    "Pricing policy metadata is invalid");

    public static final InventoryErrorCode PRICING_POLICY_NOT_FOUND =
            new InventoryErrorCode(
                    ErrorCategory.RESOURCE,
                    "INVENTORY_PRICING_POLICY_NOT_FOUND",
                    "Pricing policy not found");

    public static final InventoryErrorCode PRICING_POLICY_VERSION_NOT_FOUND =
            new InventoryErrorCode(
                    ErrorCategory.RESOURCE,
                    "INVENTORY_PRICING_POLICY_VERSION_NOT_FOUND",
                    "Pricing policy version not found");

    public static final InventoryErrorCode PRICING_POLICY_NOT_DRAFT =
            new InventoryErrorCode(
                    ErrorCategory.BUSINESS,
                    "INVENTORY_PRICING_POLICY_NOT_DRAFT",
                    "Only draft pricing policy versions can be edited");

    public static final InventoryErrorCode PRICING_POLICY_VERSION_CONFLICT =
            new InventoryErrorCode(
                    ErrorCategory.BUSINESS,
                    "INVENTORY_PRICING_POLICY_VERSION_CONFLICT",
                    "Pricing policy version has changed; reload before saving");

    public static final InventoryErrorCode PRICING_POLICY_CREATE_CONFLICT =
            new InventoryErrorCode(
                    ErrorCategory.BUSINESS,
                    "INVENTORY_PRICING_POLICY_CREATE_CONFLICT",
                    "Client request ID was already used with different metadata");

    public static final InventoryErrorCode PRICING_POLICY_DATA_INCONSISTENT =
            new InventoryErrorCode(
                    ErrorCategory.BUSINESS,
                    "INVENTORY_PRICING_POLICY_DATA_INCONSISTENT",
                    "Pricing policy data is inconsistent");

    public static final InventoryErrorCode INVALID_FARE_MATRIX =
            new InventoryErrorCode(
                    ErrorCategory.VALIDATION,
                    "INVENTORY_INVALID_FARE_MATRIX",
                    "Fare matrix is invalid");

    public static final InventoryErrorCode PREMIUM_CATEGORY_FARE_CONFLICT =
            new InventoryErrorCode(
                    ErrorCategory.VALIDATION,
                    "INVENTORY_PREMIUM_CATEGORY_FARE_CONFLICT",
                    "Premium fares must not differ by ticket category");

    public static final InventoryErrorCode COUPLE_FIXED_FARE_CONFLICT =
            new InventoryErrorCode(
                    ErrorCategory.VALIDATION,
                    "INVENTORY_COUPLE_FIXED_FARE_CONFLICT",
                    "Non-holiday couple fares must remain fixed within each experience and format");

    public static final InventoryErrorCode PRICING_MATRIX_DAY_GROUP_LOCKED =
            new InventoryErrorCode(
                    ErrorCategory.BUSINESS,
                    "INVENTORY_PRICING_MATRIX_DAY_GROUP_LOCKED",
                    "Clear the draft fare matrix before changing its day group");

    public static final InventoryErrorCode PRICING_MATRIX_SCOPE_REQUIRED =
            new InventoryErrorCode(
                    ErrorCategory.VALIDATION,
                    "INVENTORY_PRICING_MATRIX_SCOPE_REQUIRED",
                    "Select at least one pricing experience and content format");

    public static final InventoryErrorCode PRICING_MATRIX_ADULT_REQUIRED =
            new InventoryErrorCode(
                    ErrorCategory.VALIDATION,
                    "INVENTORY_PRICING_MATRIX_ADULT_REQUIRED",
                    "Fare matrix must include ADULT");

    public static final InventoryErrorCode PRICING_MATRIX_SEAT_TYPES_REQUIRED =
            new InventoryErrorCode(
                    ErrorCategory.VALIDATION,
                    "INVENTORY_PRICING_MATRIX_SEAT_TYPES_REQUIRED",
                    "Select at least one seat type");

    public static final InventoryErrorCode PRICING_MATRIX_INCOMPLETE =
            new InventoryErrorCode(
                    ErrorCategory.VALIDATION,
                    "INVENTORY_PRICING_MATRIX_INCOMPLETE",
                    "Fare matrix cell has not been configured");

    public static final InventoryErrorCode PRICING_POLICY_NOT_PUBLISHABLE =
            new InventoryErrorCode(
                    ErrorCategory.BUSINESS,
                    "INVENTORY_PRICING_POLICY_NOT_PUBLISHABLE",
                    "Pricing policy version is not publishable; validate the stored fare matrix");

    public static final InventoryErrorCode ROOM_TYPE_CHANGE_BLOCKED =
            new InventoryErrorCode(
                    ErrorCategory.BUSINESS,
                    "INVENTORY_ROOM_TYPE_CHANGE_BLOCKED",
                    "Room type cannot change while non-cancelled showtimes exist");

    public static final InventoryErrorCode ROOM_FORMAT_REMOVAL_BLOCKED =
            new InventoryErrorCode(
                    ErrorCategory.BUSINESS,
                    "INVENTORY_ROOM_FORMAT_REMOVAL_BLOCKED",
                    "Existing room content formats cannot be removed "
                            + "while non-cancelled showtimes exist");

    public static final InventoryErrorCode ROOM_DEACTIVATION_BLOCKED =
            new InventoryErrorCode(
                    ErrorCategory.BUSINESS,
                    "INVENTORY_ROOM_DEACTIVATION_BLOCKED",
                    "Room cannot be deactivated while open-for-booking "
                            + "showtimes exist today or later in the cinema time zone");

    public static final InventoryErrorCode COUPLE_CATEGORY_FARE_CONFLICT =
            new InventoryErrorCode(
                    ErrorCategory.VALIDATION,
                    "INVENTORY_COUPLE_CATEGORY_FARE_CONFLICT",
                    "Couple fares must not differ by ticket category "
                            + "within the same experience, format and time band");

    public static final InventoryErrorCode CINEMA_DEACTIVATION_BLOCKED =
            new InventoryErrorCode(
                    ErrorCategory.BUSINESS,
                    "INVENTORY_CINEMA_DEACTIVATION_BLOCKED",
                    "Cinema cannot be deactivated while open-for-booking "
                            + "showtimes exist today or later in the cinema time zone");

    public static final InventoryErrorCode SHOWTIME_BEFORE_RELEASE_DATE =
            new InventoryErrorCode(
                    ErrorCategory.VALIDATION,
                    "INVENTORY_SHOWTIME_BEFORE_RELEASE_DATE",
                    "Showtime date must not precede the movie release date "
                            + "in the cinema time zone");

    public static final InventoryErrorCode PRICING_CINEMA_SCOPE_INVALID =
            new InventoryErrorCode(
                    ErrorCategory.VALIDATION,
                    "INVENTORY_PRICING_CINEMA_SCOPE_INVALID",
                    "Select one or more distinct cinemas; "
                            + "the creation context must be included when creating a policy");

    public static final InventoryErrorCode PRICING_FINGERPRINT_UNAVAILABLE =
            new InventoryErrorCode(
                    ErrorCategory.SYSTEM,
                    "INVENTORY_PRICING_FINGERPRINT_UNAVAILABLE",
                    "Pricing request fingerprint could not be calculated");

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
