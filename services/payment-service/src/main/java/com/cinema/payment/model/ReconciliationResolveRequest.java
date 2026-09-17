package com.cinema.payment.model;

import com.cinema.payment.enums.FinancialAuditActorType;
import com.cinema.payment.enums.ReconciliationResolution;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ReconciliationResolveRequest(
        UUID reconciliationCaseId,
        ReconciliationResolution resolution,
        FinancialAuditActorType actorType,
        String actorId,
        String reason,
        String providerReference,
        String failureCode,
        String failureMessage,
        UUID correlationId,
        OffsetDateTime requestedAt) {}
