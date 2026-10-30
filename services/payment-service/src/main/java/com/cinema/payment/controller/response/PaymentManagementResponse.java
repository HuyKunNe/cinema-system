package com.cinema.payment.controller.response;

import com.cinema.payment.enums.PaymentStatus;
import com.cinema.payment.enums.RefundStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.ALWAYS)
@Schema(
        requiredProperties = {
            "id",
            "bookingId",
            "userId",
            "paymentAttempt",
            "amount",
            "currency",
            "provider",
            "status",
            "refundStatus",
            "providerReference",
            "failureCode",
            "holdExpiresAt",
            "requestedAt",
            "completedAt",
            "version",
            "createdAt",
            "updatedAt"
        })
public record PaymentManagementResponse(
        UUID id,
        UUID bookingId,
        UUID userId,
        Integer paymentAttempt,
        @Schema(description = "Stored payment amount received from the booking payment request.")
                BigDecimal amount,
        String currency,
        String provider,
        PaymentStatus status,
        RefundStatus refundStatus,
        @Schema(nullable = true) String providerReference,
        @Schema(nullable = true) String failureCode,
        OffsetDateTime holdExpiresAt,
        OffsetDateTime requestedAt,
        @Schema(nullable = true) OffsetDateTime completedAt,
        Long version,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {}
