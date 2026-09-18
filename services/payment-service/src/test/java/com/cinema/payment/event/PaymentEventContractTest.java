package com.cinema.payment.event;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PaymentEventContractTest {

    @Test
    void paymentRequestedContractShouldRemainCanonical() {

        assertThat(PaymentEventContract.PAYMENT_REQUESTED).isEqualTo("payment-requested");

        assertThat(PaymentEventContract.PAYMENT_REQUESTED_VERSION).isEqualTo("1");

        assertThat(PaymentEventContract.BOOKING_PRODUCER).isEqualTo("booking-service");

        assertThat(PaymentEventContract.BOOKING_AGGREGATE_TYPE).isEqualTo("BOOKING");

        assertThat(PaymentEventContract.PAYMENT_REQUESTED_CONSUMER)
                .isEqualTo("payment-request-processing");
    }

    @Test
    void paymentSucceededContractShouldRemainCanonical() {

        assertThat(PaymentEventContract.PAYMENT_SUCCEEDED).isEqualTo("payment-succeeded");

        assertThat(PaymentEventContract.PAYMENT_SUCCEEDED_VERSION).isEqualTo("1");
    }

    @Test
    void paymentFailedContractShouldRemainCanonical() {

        assertThat(PaymentEventContract.PAYMENT_FAILED).isEqualTo("payment-failed");

        assertThat(PaymentEventContract.PAYMENT_FAILED_VERSION).isEqualTo("1");
    }
}
