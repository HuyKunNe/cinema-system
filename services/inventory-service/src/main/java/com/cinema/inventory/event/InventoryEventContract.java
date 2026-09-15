package com.cinema.inventory.event;

public final class InventoryEventContract {

    public static final String SEAT_RESERVATION_REQUESTED = "seat-reservation-requested";

    public static final String SEAT_RESERVATION_REQUESTED_VERSION = "1";

    public static final String SEAT_RESERVED = "seat-reserved";

    public static final String SEAT_RESERVED_VERSION = "1";

    public static final String SEAT_RESERVATION_REJECTED = "seat-reservation-rejected";

    public static final String SEAT_RESERVATION_REJECTED_VERSION = "1";

    public static final String SEAT_RESERVATION_CONSUMER = "inventory-seat-reservation";

    public static final String PRODUCER = "inventory-service";

    public static final String CURRENCY_VND = "VND";

    public static final String BOOKING_CONFIRMED = "booking-confirmed";

    public static final String BOOKING_CONFIRMED_VERSION = "1";

    public static final String BOOKING_CONFIRMED_CONSUMER = "inventory-booking-confirmed";

    public static final String BOOKING_PRODUCER = "booking-service";

    public static final String BOOKING_AGGREGATE_TYPE = "BOOKING";

    public static final String SEAT_RELEASE_REQUESTED = "seat-release-requested";

    public static final String SEAT_RELEASE_REQUESTED_VERSION = "1";

    public static final String SEAT_RELEASE_REQUESTED_CONSUMER = "inventory-seat-release";

    public static final String SEAT_RELEASE_REASON_PAYMENT_FAILED = "PAYMENT_FAILED";

    private InventoryEventContract() {}
}
