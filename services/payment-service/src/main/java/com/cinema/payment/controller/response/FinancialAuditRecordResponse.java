package com.cinema.payment.controller.response;

import com.cinema.payment.enums.FinancialAuditAction;
import com.cinema.payment.enums.FinancialAuditActorType;

import java.time.OffsetDateTime;
import java.util.UUID;

public record FinancialAuditRecordResponse(
        UUID id,
        UUID paymentId,
        FinancialAuditAction action,
        FinancialAuditActorType actorType,
        String actorId,
        String reason,
        String metadata,
        UUID correlationId,
        OffsetDateTime occurredAt) {}
