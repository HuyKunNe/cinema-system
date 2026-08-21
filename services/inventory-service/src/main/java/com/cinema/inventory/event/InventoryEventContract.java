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

    private InventoryEventContract() {}
}

