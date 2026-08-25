package com.cinema.booking.event.payload;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PaymentRequestedPayload(
        UUID bookingId,
        UUID userId,
        BigDecimal amount,
        String currency,
        int paymentAttempt,
        OffsetDateTime holdExpiresAt,
        OffsetDateTime requestedAt) {}
