package com.cinema.payment.event;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PaymentEventContractTest {

    @Test
    void shouldExposeCanonicalPaymentRequestedContract() {

        assertThat(PaymentEventContract.PAYMENT_REQUESTED)
                .isEqualTo("payment-requested");

        assertThat(
                        PaymentEventContract
                                .PAYMENT_REQUESTED_VERSION)
                .isEqualTo("1");

        assertThat(PaymentEventContract.BOOKING_PRODUCER)
                .isEqualTo("booking-service");

        assertThat(
                        PaymentEventContract
                                .BOOKING_AGGREGATE_TYPE)
                .isEqualTo("BOOKING");
    }
}
