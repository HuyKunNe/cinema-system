package com.cinema.payment.model;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.cinema.payment.enums.FinancialAuditActorType;

public record ReconciliationRejectRequest(
        UUID reconciliationCaseId,
        FinancialAuditActorType actorType,
        String actorId,
        String reason,
        UUID correlationId,
        OffsetDateTime requestedAt) {}
