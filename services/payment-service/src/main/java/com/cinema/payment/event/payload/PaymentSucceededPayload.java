package com.cinema.payment.event.payload;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PaymentSucceededPayload(
        UUID paymentId,
        UUID bookingId,
        BigDecimal amount,
        String currency,
        String provider,
        String providerReference,
        OffsetDateTime paidAt) {}
