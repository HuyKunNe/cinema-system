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

    public static final String PAYMENT_REQUESTED = "payment-requested";

    public static final String PAYMENT_REQUESTED_VERSION = "1";

    public static final String BOOKING_CANCELLED = "booking-cancelled";

    public static final String BOOKING_CANCELLED_VERSION = "1";

    public static final String BOOKING_EXPIRED = "booking-expired";

    public static final String BOOKING_EXPIRED_VERSION = "1";

    public static final String PAYMENT_SUCCEEDED = "payment-succeeded";

    public static final String PAYMENT_SUCCEEDED_VERSION = "1";

    public static final String PAYMENT_FAILED = "payment-failed";

    public static final String PAYMENT_FAILED_VERSION = "1";

    public static final String PAYMENT_PRODUCER = "payment-service";

    public static final String PAYMENT_SUCCEEDED_CONSUMER = "booking-payment-succeeded";

    public static final String PAYMENT_FAILED_CONSUMER = "booking-payment-failed";

    private BookingEventContract() {}
}
