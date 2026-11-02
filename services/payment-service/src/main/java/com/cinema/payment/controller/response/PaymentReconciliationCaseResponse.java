package com.cinema.payment.controller.response;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.cinema.payment.enums.FinancialAuditActorType;
import com.cinema.payment.enums.ReconciliationReason;
import com.cinema.payment.enums.ReconciliationResolution;
import com.cinema.payment.enums.ReconciliationStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.ALWAYS)
@Schema(
        requiredProperties = {
            "id",
            "paymentId",
            "paymentTransactionId",
            "status",
            "reason",
            "resolution",
            "provider",
            "providerReference",
            "openedAt",
            "resolvedAt",
            "resolvedByType",
            "resolvedBy",
            "resolutionReason",
            "createdAt",
            "updatedAt"
        })
public record PaymentReconciliationCaseResponse(
        UUID id,
        UUID paymentId,
        UUID paymentTransactionId,
        ReconciliationStatus status,
        ReconciliationReason reason,
        @Schema(nullable = true) ReconciliationResolution resolution,
        String provider,
        @Schema(nullable = true) String providerReference,
        OffsetDateTime openedAt,
        @Schema(nullable = true) OffsetDateTime resolvedAt,
        @Schema(nullable = true) FinancialAuditActorType resolvedByType,
        @Schema(nullable = true) String resolvedBy,
        @Schema(nullable = true) String resolutionReason,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {}
