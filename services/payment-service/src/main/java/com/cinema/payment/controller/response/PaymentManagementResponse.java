package com.cinema.payment.controller.response;

import com.cinema.payment.enums.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record PaymentManagementResponse(
        UUID id,
        UUID bookingId,
        UUID userId,
        Integer paymentAttempt,
                BigDecimal amount,
        String currency,
        String provider,
        PaymentStatus status,
        String providerReference,
        String failureCode,
        OffsetDateTime holdExpiresAt,
        OffsetDateTime requestedAt,
        OffsetDateTime completedAt,
        Long version,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {}
