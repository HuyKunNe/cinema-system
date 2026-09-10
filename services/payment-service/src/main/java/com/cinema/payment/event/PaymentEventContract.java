package com.cinema.payment.event;

public final class PaymentEventContract {

    public static final String PAYMENT_REQUESTED = "payment-requested";

    public static final String PAYMENT_REQUESTED_VERSION = "1";

    public static final String PAYMENT_SUCCEEDED = "payment-succeeded";

    public static final String PAYMENT_SUCCEEDED_VERSION = "1";

    public static final String PAYMENT_FAILED = "payment-failed";

    public static final String PAYMENT_FAILED_VERSION = "1";

    public static final String BOOKING_PRODUCER = "booking-service";

    public static final String BOOKING_AGGREGATE_TYPE = "BOOKING";

    public static final String PAYMENT_REQUESTED_CONSUMER = "payment-request-processing";

    private PaymentEventContract() {}
}
