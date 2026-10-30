package com.cinema.payment.controller.response;

import com.cinema.payment.enums.PaymentTransactionStatus;
import com.cinema.payment.enums.PaymentTransactionType;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.ALWAYS)
@Schema(
        requiredProperties = {
            "id",
            "paymentId",
            "provider",
            "transactionType",
            "attemptNumber",
            "status",
            "amount",
            "currency",
            "providerReference",
            "failureCode",
            "requestedAt",
            "completedAt",
            "createdAt",
            "updatedAt"
        })
public record PaymentTransactionManagementResponse(
        UUID id,
        UUID paymentId,
        String provider,
        PaymentTransactionType transactionType,
        @Schema(
                        description =
                                "Stored operation attempt number. "
                                        + "Distinct from Payment.paymentAttempt.")
                Integer attemptNumber,
        PaymentTransactionStatus status,
        BigDecimal amount,
        String currency,
        @Schema(nullable = true) String providerReference,
        @Schema(nullable = true) String failureCode,
        OffsetDateTime requestedAt,
        @Schema(nullable = true) OffsetDateTime completedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {}
