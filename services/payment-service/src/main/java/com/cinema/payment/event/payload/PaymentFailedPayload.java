package com.cinema.payment.event.payload;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PaymentFailedPayload(
        UUID paymentId,
        UUID bookingId,
        String failureCode,
        String message,
        OffsetDateTime failedAt,
        boolean retryable) {}
