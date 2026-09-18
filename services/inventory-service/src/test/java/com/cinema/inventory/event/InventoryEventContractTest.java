package com.cinema.inventory.event;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class InventoryEventContractTest {

    @Test
    void seatReservationContractShouldRemainCanonical() {

        assertThat(InventoryEventContract.SEAT_RESERVATION_REQUESTED)
                .isEqualTo("seat-reservation-requested");

        assertThat(InventoryEventContract.SEAT_RESERVATION_REQUESTED_VERSION)
                .isEqualTo("1");

        assertThat(InventoryEventContract.SEAT_RESERVED)
                .isEqualTo("seat-reserved");

        assertThat(InventoryEventContract.SEAT_RESERVED_VERSION)
                .isEqualTo("1");

        assertThat(InventoryEventContract.SEAT_RESERVATION_REJECTED)
                .isEqualTo("seat-reservation-rejected");

        assertThat(InventoryEventContract.SEAT_RESERVATION_REJECTED_VERSION)
                .isEqualTo("1");
    }

    @Test
    void bookingConfirmationContractShouldRemainCanonical() {

        assertThat(InventoryEventContract.BOOKING_CONFIRMED)
                .isEqualTo("booking-confirmed");

        assertThat(InventoryEventContract.BOOKING_CONFIRMED_VERSION)
                .isEqualTo("1");

        assertThat(InventoryEventContract.BOOKING_PRODUCER)
                .isEqualTo("booking-service");

        assertThat(InventoryEventContract.BOOKING_AGGREGATE_TYPE)
                .isEqualTo("BOOKING");
    }

    @Test
    void seatReleaseContractShouldRemainCanonical() {

        assertThat(InventoryEventContract.SEAT_RELEASE_REQUESTED)
                .isEqualTo("seat-release-requested");

        assertThat(InventoryEventContract.SEAT_RELEASE_REQUESTED_VERSION)
                .isEqualTo("1");

        assertThat(InventoryEventContract.SEAT_RELEASED)
                .isEqualTo("seat-released");

        assertThat(InventoryEventContract.SEAT_RELEASED_VERSION)
                .isEqualTo("1");

        assertThat(InventoryEventContract.SEAT_RELEASE_REASON_PAYMENT_FAILED)
                .isEqualTo("PAYMENT_FAILED");
    }

    @Test
    void bookingLifecycleContractsShouldRemainCanonical() {

        assertThat(InventoryEventContract.BOOKING_CANCELLED)
                .isEqualTo("booking-cancelled");

        assertThat(InventoryEventContract.BOOKING_CANCELLED_VERSION)
                .isEqualTo("1");

        assertThat(InventoryEventContract.BOOKING_EXPIRED)
                .isEqualTo("booking-expired");

        assertThat(InventoryEventContract.BOOKING_EXPIRED_VERSION)
                .isEqualTo("1");

        assertThat(
                        InventoryEventContract
                                .BOOKING_CANCELLATION_REASON_USER_REQUESTED)
                .isEqualTo("USER_REQUESTED");
    }
}
