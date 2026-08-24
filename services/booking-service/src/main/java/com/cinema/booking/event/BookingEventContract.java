package com.cinema.booking.event;

public final class BookingEventContract {

    public static final String SEAT_RESERVATION_REQUESTED = "seat-reservation-requested";

    public static final String SEAT_RESERVATION_REQUESTED_VERSION = "1";

    public static final String SEAT_RESERVED = "seat-reserved";

    public static final String SEAT_RESERVED_VERSION = "1";

    public static final String SEAT_RESERVATION_REJECTED = "seat-reservation-rejected";

    public static final String SEAT_RESERVATION_REJECTED_VERSION = "1";

    public static final String SEAT_RESERVED_CONSUMER = "booking-seat-reserved";

    public static final String SEAT_RESERVATION_REJECTED_CONSUMER = "booking-seat-rejected";

    public static final String INVENTORY_PRODUCER = "inventory-service";

    private BookingEventContract() {}
}
