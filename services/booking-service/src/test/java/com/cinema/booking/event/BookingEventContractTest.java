package com.cinema.booking.event;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.cinema.booking.enums.BookingCancellationReason;

class BookingEventContractTest {

    @Test
    void bookingCancelledContractShouldRemainCanonical() {

        assertThat(BookingEventContract.BOOKING_CANCELLED).isEqualTo("booking-cancelled");

        assertThat(BookingEventContract.BOOKING_CANCELLED_VERSION).isEqualTo("1");
    }

    @Test
    void bookingExpiredContractShouldRemainCanonical() {

        assertThat(BookingEventContract.BOOKING_EXPIRED).isEqualTo("booking-expired");

        assertThat(BookingEventContract.BOOKING_EXPIRED_VERSION).isEqualTo("1");
    }

    @Test
    void cancellationReasonShouldRemainApproved() {

        assertThat(BookingCancellationReason.values())
                .containsExactly(BookingCancellationReason.USER_REQUESTED);
    }
}
